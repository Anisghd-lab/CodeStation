//! Platform glue: data dir, background service install (launchd / systemd /
//! the Windows Run key, in `unix.rs` and `windows.rs`),
//! keeping the machine awake during turns, pairing addresses, and the Claude
//! Code status line we route through the host.
//!
//! Everything here is blocking (`std::fs`, `std::process`): async callers go
//! through `spawn_blocking`.

use anyhow::{Context, Result, bail};
use base64::{Engine as _, engine::general_purpose::STANDARD as B64};
use serde_json::Value;
use sha2::{Digest, Sha256};
use std::fmt::Write as _;
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::sync::OnceLock;
use std::time::{Duration, Instant};

/// Whether a desktop service manager started this process. CodeStation runs from a
/// terminal script in Termux/PRoot; nothing here is service-managed, so this stays
/// false and the log/first-run behavior matches a foreground start.
pub fn started_by_service() -> bool {
    false
}

static BINARY_IDENTITY: OnceLock<(String, String)> = OnceLock::new();

/// Snapshot before serving: reading the path again after an update would identify
/// the replacement on disk rather than this running process.
pub fn capture_binary_identity() -> Result<()> {
    let exe = current_exe()?;
    let mut hash = String::with_capacity(64);
    for byte in Sha256::digest(std::fs::read(&exe)?) {
        write!(hash, "{byte:02x}")?;
    }
    let _ = BINARY_IDENTITY.set((exe.to_string_lossy().into_owned(), hash));
    Ok(())
}

/// This binary's resolved path, in the form other programs print it: Windows'
/// `canonicalize` adds a `\\?\` prefix that the apps' paths and shells don't have.
pub fn current_exe() -> std::io::Result<PathBuf> {
    let exe = std::env::current_exe()?.canonicalize()?;
    Ok(match exe.to_str().and_then(|p| p.strip_prefix(r"\\?\")) {
        Some(plain) if cfg!(windows) => PathBuf::from(plain),
        _ => exe,
    })
}

pub fn binary_identity() -> Option<&'static (String, String)> {
    BINARY_IDENTITY.get()
}

pub const DEFAULT_PORT: u16 = crate::environment::Environment::current().port();
/// Marker between our command and a wrapped user status line.


/// The user's home. Codync can't do anything useful without one, so its absence is fatal.
pub(crate) fn home() -> PathBuf {
    dirs::home_dir().expect("HOME must be set: Codync keeps its data and agent settings there")
}

pub fn data_dir() -> PathBuf {
    if let Some(dir) = std::env::var_os("CODESTATION_HOME").or_else(|| std::env::var_os("CODYNC_HOME")) {
        return PathBuf::from(dir);
    }
    let default = default_data_dir();
    if default.exists() {
        return default;
    }
    // Backward compatibility: if ~/.codync-dev or ~/.codync exists, use it
    let legacy_dev = home().join(".codync-dev");
    if legacy_dev.exists() {
        return legacy_dev;
    }
    let legacy_main = home().join(".codync");
    if legacy_main.exists() {
        return legacy_main;
    }
    default
}

#[cfg(not(test))]
fn default_data_dir() -> PathBuf {
    home().join(crate::environment::Environment::current().data_folder())
}

/// Unit tests never see a real host's data: tests that start a hub run its start-up clean-up, which deletes files.
#[cfg(test)]
fn default_data_dir() -> PathBuf {
    std::env::temp_dir().join(format!("codync-unit-{}", std::process::id()))
}

fn create_parent(file: &Path) -> Result<()> {
    if let Some(parent) = file.parent() {
        std::fs::create_dir_all(parent).with_context(|| format!("creating {}", parent.display()))?;
    }
    Ok(())
}

/// Held by the daemon for its entire lifetime. OS advisory locks are released
/// after a crash, so a stale lock file never prevents recovery.
pub fn lock_host() -> Result<std::fs::File> {
    let path = data_dir().join("host.lock");
    create_parent(&path)?;
    let file = std::fs::OpenOptions::new().create(true).truncate(false).read(true).write(true).open(path)?;
    fs2::FileExt::try_lock_exclusive(&file)
        .context("another codync-host is already running for this data directory")?;
    Ok(file)
}

/// Holds a sleep inhibitor while any bot is working.
#[derive(Default)]
pub struct KeepAwake(Option<Child>);

impl KeepAwake {
    pub fn set(&mut self, on: bool) {
        match (on, self.0.is_some()) {
            (true, false) => {
                let pid = std::process::id().to_string();
                let child = if cfg!(target_os = "macos") {
                    Command::new("caffeinate").args(["-i", "-w", &pid]).stdout(Stdio::null()).spawn()
                } else if cfg!(windows) {
                    // The request lasts as long as the PowerShell holding it.
                    let script = format!(
                        "$k = Add-Type -Name P -Namespace W -PassThru -MemberDefinition '[DllImport(\"kernel32.dll\")] public static extern uint SetThreadExecutionState(uint f);'; $null = $k::SetThreadExecutionState(0x80000001); Wait-Process -Id {pid}"
                    );
                    let utf16: Vec<u8> = script.encode_utf16().flat_map(u16::to_le_bytes).collect();
                    Command::new("powershell")
                        .args(["-NoProfile", "-NonInteractive", "-EncodedCommand", &B64.encode(utf16)])
                        .stdout(Stdio::null())
                        .stderr(Stdio::null())
                        .spawn()
                } else {
                    Command::new("systemd-inhibit")
                        .args([
                            "--what=sleep:idle",
                            "--who=Codync",
                            "--why=A bot is working or a routine is scheduled",
                            "--mode=block",
                            "sleep",
                            "infinity",
                        ])
                        .stdout(Stdio::null())
                        .stderr(Stdio::null())
                        .spawn()
                };
                self.0 = child.map_err(|error| tracing::warn!(%error, "can't keep the machine awake")).ok();
            }
            (false, true) => {
                if let Some(mut c) = self.0.take() {
                    let _ = c.kill();
                    let _ = c.wait();
                }
            }
            _ => {}
        }
    }
}

/// Where the local device reaches this host: the loopback only. CodeStation is
/// single-device: the app and this host share the phone's network namespace, so
/// `127.0.0.1` in the PRoot environment is the phone's own loopback.
pub fn addresses(port: u16) -> Vec<String> {
    vec![format!("http://127.0.0.1:{port}")]
}

pub fn host_name() -> String {
    let n = gethostname::gethostname().to_string_lossy().into_owned();
    n.trim_end_matches(".local").to_owned()
}

/// What the computer is, so phones can draw the right icon (Linux gets its penguin;
/// Windows counts as a desktop).
#[derive(Clone, Copy, Debug, PartialEq, Eq, serde::Serialize)]
#[serde(rename_all = "lowercase")]
pub enum Device {
    Laptop,
    MacMini,
    MacStudio,
    IMac,
    MacPro,
    Desktop,
    Linux,
}

/// Detected once; on macOS from `system_profiler`'s model name.
pub fn device() -> Device {
    static DEVICE: OnceLock<Device> = OnceLock::new();
    *DEVICE.get_or_init(|| {
        if cfg!(windows) {
            return Device::Desktop;
        }
        if !cfg!(target_os = "macos") {
            return Device::Linux;
        }
        let out = Command::new("system_profiler").args(["SPHardwareDataType", "-json"]).output();
        let json: Value = out.ok().and_then(|o| serde_json::from_slice(&o.stdout).ok()).unwrap_or_default();
        mac_device(json["SPHardwareDataType"][0]["machine_name"].as_str().unwrap_or_default())
    })
}

fn mac_device(machine_name: &str) -> Device {
    match machine_name {
        n if n.starts_with("MacBook") => Device::Laptop,
        "Mac mini" => Device::MacMini,
        "Mac Studio" => Device::MacStudio,
        "iMac" | "iMac Pro" => Device::IMac,
        "Mac Pro" => Device::MacPro,
        _ => Device::Desktop,
    }
}

/// RFC 3986 percent-encoding of everything but unreserved characters.
fn pct(s: &str) -> String {
    use std::fmt::Write as _;
    let mut out = String::with_capacity(s.len());
    for b in s.bytes() {
        if b.is_ascii_alphanumeric() || matches!(b, b'-' | b'.' | b'_' | b'~') {
            out.push(char::from(b));
        } else {
            let _ = write!(out, "%{b:02X}");
        }
    }
    out
}

/// What the pairing QR carries (spec §4.1): public keys and a one-time code, no secrets
/// that outlive the code.
pub struct PairingQr<'a> {
    pub name: &'a str,
    pub computer_id: &'a str,
    pub sign_key: &'a str,
    pub box_key: &'a str,
    pub code: &'a str,
    pub urls: &'a [String],
}

pub fn pairing_url(q: &PairingQr) -> String {
    format!(
        "{}://pair?v=3&name={}&id={}&sk={}&bk={}&code={}&urls={}",
        crate::environment::Environment::current().url_scheme(),
        pct(q.name),
        q.computer_id,
        q.sign_key,
        q.box_key,
        q.code,
        pct(&q.urls.join(","))
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn unit_tests_keep_out_of_the_real_data_folder() {
        assert!(
            std::env::var_os("CODYNC_HOME").is_some() || !data_dir().starts_with(home()),
            "{}",
            data_dir().display()
        );
    }

    #[test]
    fn pairing_url_is_escaped() {
        let urls = ["http://127.0.0.1:19222".to_owned(), "http://a:1".to_owned()];
        let q = PairingQr {
            name: "Kevin's Phone",
            computer_id: "cid",
            sign_key: "sk",
            box_key: "bk",
            code: "c0de",
            urls: &urls,
        };
        assert_eq!(
            pairing_url(&q),
            format!(
                "{}://pair?v=3&name=Kevin%27s%20Phone&id=cid&sk=sk&bk=bk&code=c0de&urls=http%3A%2F%2F127.0.0.1%3A19222%2Chttp%3A%2F%2Fa%3A1",
                crate::environment::Environment::current().url_scheme()
            )
        );
    }
}
