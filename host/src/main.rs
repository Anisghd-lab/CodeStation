//! `codync-host`: runs coding-agent bots over ACP and serves the Codync apps.

mod agent;
mod analytics;
mod api;
mod background;
mod chat;
mod compat;
mod environment;
mod hub;
mod market;
mod mcp;
mod remote;
mod routines;
mod service;
mod shell;
mod store;
mod usage;
mod voice;

use agent::backends;
use remote::identity;

use anyhow::{Context, Result};
use clap::{Parser, Subcommand};
use serde_json::json;
use std::fmt::Write as _;
use std::future::IntoFuture;
use std::io::Read;
use std::sync::{LazyLock, Mutex, MutexGuard, PoisonError};
use std::time::Duration;

/// Poison-tolerant locking. Every critical section in the host is a short,
/// self-contained update, so a panic elsewhere never leaves the data half
/// written — and a daemon must not wedge every later caller over one panic.
pub trait LockExt<T> {
    fn locked(&self) -> MutexGuard<'_, T>;
}

impl<T> LockExt<T> for Mutex<T> {
    fn locked(&self) -> MutexGuard<'_, T> {
        self.lock().unwrap_or_else(PoisonError::into_inner)
    }
}

/// One shared HTTP client (connection pool + TLS config) for the whole process.
pub fn http() -> &'static reqwest::Client {
    static CLIENT: LazyLock<reqwest::Client> = LazyLock::new(|| {
        // Only fails without system CA certificates (bare containers).
        reqwest::Client::builder().build().expect("TLS setup failed: install the ca-certificates package")
    });
    &CLIENT
}

#[derive(Parser)]
#[command(
    name = "codestation",
    version,
    about = "CodeStation: runs your AI coding agents and serves the CodeStation mobile app"
)]
struct Cli {
    #[command(subcommand)]
    cmd: Option<Sub>,
}

#[derive(Subcommand)]
enum Sub {
    /// Start the host daemon in the background.
    Start {
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
        #[arg(long, default_value = "127.0.0.1")]
        bind: String,
    },
    /// Stop any running CodeStation host daemon.
    Stop {
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// Check whether the host daemon is running and healthy.
    Status {
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// Print the local loopback authentication token.
    Token,
    /// Inspect environment, prerequisites, active port, and discovered AI agents.
    Doctor {
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// Run the host in the foreground (what the local start script runs).
    Serve {
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
        /// Address to bind. CodeStation is local-only: 127.0.0.1, on the same phone.
        #[arg(long, default_value = "127.0.0.1")]
        bind: String,
    },
    /// Show a one-time pairing QR code for the CodeStation mobile app (the host must be running).
    Pair {
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
        /// Print machine-readable JSON instead of a QR code.
        #[arg(long)]
        json: bool,
    },
    /// This computer's identity and local API details (for SSH setups).
    Info {
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
        #[arg(long)]
        json: bool,
    },
    /// List or revoke the devices allowed to use this computer.
    Devices {
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
        #[command(subcommand)]
        action: Option<DevicesAction>,
    },
    /// The versions and environment of this host (JSON).
    #[command(hide = true)]
    Compat,
    /// Claude Code statusLine command: forwards usage limits to the host, then prints
    /// the wrapped command's status line (`statusline -- <command>`) or a short default.
    Statusline {
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
        #[arg(last = true)]
        wrapped: Vec<String>,
    },
    /// Rotate the local API token used by this computer's own apps and helpers.
    ResetToken,
    /// npm on this computer's Node, else on Codync's own (downloaded first): what setup
    /// terminals install npm CLIs with.
    #[command(hide = true)]
    Npm {
        #[arg(trailing_var_arg = true, allow_hyphen_values = true)]
        args: Vec<String>,
    },
    /// Add Codync's own Node.js, for agents that run on it (an agent's setup step).
    #[command(hide = true)]
    Node,
    /// Built-in MCP servers that bots are started with (stdio).
    #[command(hide = true)]
    Mcp {
        #[command(subcommand)]
        server: McpServer,
    },
}

#[derive(Subcommand)]
enum DevicesAction {
    /// Show every allowed device (the default).
    List,
    /// Remove a device's access; it disconnects at once.
    Revoke { key: String },
}

#[derive(Subcommand)]
enum McpServer {
    /// Launch a local connector without exposing its credentials in ACP config.
    Local {
        #[arg(long)]
        connector: String,
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// Invite the user to connect services securely.
    Connectors {
        #[arg(long)]
        bot: String,
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// Scheduled and event-triggered bot routines.
    Routines {
        #[arg(long)]
        bot: String,
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// Send the user chat messages.
    Chat {
        #[arg(long)]
        bot: String,
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// Discover and ask the user's other bots for help.
    Team {
        #[arg(long)]
        bot: String,
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// Create bots and change any bot's setup, for a bot that may manage the team.
    Manage {
        #[arg(long)]
        bot: String,
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// Search the bot's own chat history.
    Memory {
        #[arg(long)]
        bot: String,
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// Tools of the apps connected through Composio.
    Composio {
        #[arg(long)]
        bot: String,
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// A remote connector, with its sign-in handled by the host.
    Remote {
        #[arg(long)]
        connector: String,
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
    /// See and operate this computer's desktop.
    Computer {
        #[arg(long)]
        bot: String,
        #[arg(long, default_value_t = service::DEFAULT_PORT)]
        port: u16,
    },
}

/// The database id + local API token live in the database; the token is mirrored to
/// `~/.codync/token` (0600) for local helpers like the statusline command.
fn open_store() -> Result<(store::Store, String, String)> {
    let dir = service::data_dir();
    std::fs::create_dir_all(&dir).with_context(|| format!("creating {}", dir.display()))?;
    let db = dir.join("codync.db");
    let store = store::Store::open(&db).with_context(|| format!("opening {}", db.display()))?;
    let host_id = if let Some(v) = store.kv_get("host_id") {
        v
    } else {
        let v = uuid::Uuid::new_v4().to_string();
        store.kv_set("host_id", &v)?;
        // A new install, whichever command made it: its first start is `host_started.first_run`.
        store.kv_set(analytics::KV_STARTED, "")?;
        v
    };
    let token = match store.kv_get("token") {
        Some(v) => v,
        None => rotate_token(&store)?,
    };
    Ok((store, host_id, token))
}

fn rotate_token(store: &store::Store) -> Result<String> {
    let t = format!("{}{}", uuid::Uuid::new_v4().simple(), uuid::Uuid::new_v4().simple());
    store.kv_set("token", &t)?;
    write_token_file(&t)?;
    Ok(t)
}

fn write_token_file(token: &str) -> Result<()> {
    let path = service::data_dir().join("token");
    std::fs::write(&path, token).with_context(|| format!("writing {}", path.display()))?;
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        std::fs::set_permissions(&path, std::fs::Permissions::from_mode(0o600))
            .with_context(|| format!("restricting {}", path.display()))?;
    }
    Ok(())
}

#[tokio::main]
async fn main() -> Result<()> {
    // Every TLS connection (the relay, HTTPS) uses aws-lc-rs, also in builds that have ring too.
    let _ = rustls::crypto::aws_lc_rs::default_provider().install_default();
    let cli = Cli::parse();
    match cli.cmd.unwrap_or(Sub::Serve { port: service::DEFAULT_PORT, bind: "127.0.0.1".into() }) {
        Sub::Start { port, bind } => start(&bind, port).await,
        Sub::Stop { port } => stop(port).await,
        Sub::Status { port } => status(port).await,
        Sub::Token => print_token(),
        Sub::Doctor { port } => doctor(port).await,
        Sub::Serve { port, bind } => serve(&bind, port).await,
        Sub::Pair { port, json } => pair(port, json).await,
        Sub::Info { port, json } => info(port, json).await,
        Sub::Devices { port, action } => match action.unwrap_or(DevicesAction::List) {
            DevicesAction::List => {
                let v = local_call(port, "devices", json!({})).await?;
                for d in v["devices"].as_array().into_iter().flatten() {
                    println!(
                        "{}  {} ({}, {}){}",
                        d["key"].as_str().unwrap_or_default(),
                        d["name"].as_str().unwrap_or_default(),
                        d["platform"].as_str().unwrap_or_default(),
                        d["source"].as_str().unwrap_or_default(),
                        if d["connected"] == true { "  connected" } else { "" }
                    );
                }
                Ok(())
            }
            DevicesAction::Revoke { key } => {
                local_call(port, "revokeDevice", json!({"key": key})).await?;
                println!("Revoked.");
                Ok(())
            }
        },
        Sub::Compat => {
            println!(
                "{}",
                serde_json::json!({"version": env!("CARGO_PKG_VERSION"), "minApp": compat::MIN_APP, "environment": environment::Environment::current().name()})
            );
            Ok(())
        }
        Sub::Node => agent::node::add().await,
        Sub::Npm { args } => std::process::exit(agent::node::npm(&args).await?),
        Sub::Statusline { port, wrapped } => {
            statusline(port, wrapped.join(" ")).await;
            Ok(())
        }
        Sub::ResetToken => {
            let (store, _, _) = open_store()?;
            rotate_token(&store)?;
            println!("Token rotated. Restart the host so local helpers pick it up.");
            Ok(())
        }
        Sub::Mcp { server: McpServer::Connectors { bot, port } } => {
            mcp::serve(bot, port, mcp::Server::Connectors).await
        }
        Sub::Mcp { server: McpServer::Computer { bot, port } } => mcp::serve(bot, port, mcp::Server::Computer).await,
        Sub::Mcp { server: McpServer::Routines { bot, port } } => mcp::serve(bot, port, mcp::Server::Routines).await,
        Sub::Mcp { server: McpServer::Chat { bot, port } } => mcp::serve(bot, port, mcp::Server::Chat).await,
        Sub::Mcp { server: McpServer::Team { bot, port } } => mcp::serve(bot, port, mcp::Server::Team).await,
        Sub::Mcp { server: McpServer::Manage { bot, port } } => mcp::serve(bot, port, mcp::Server::Manage).await,
        Sub::Mcp { server: McpServer::Memory { bot, port } } => mcp::serve(bot, port, mcp::Server::Memory).await,
        Sub::Mcp { server: McpServer::Composio { bot, port } } => mcp::serve(bot, port, mcp::Server::Composio).await,
        Sub::Mcp { server: McpServer::Local { connector, port } } => mcp::serve_local(connector, port).await,
        Sub::Mcp { server: McpServer::Remote { connector, port } } => mcp::serve_remote(connector, port).await,
    }
}

async fn status(port: u16) -> Result<()> {
    match http()
        .get(format!("http://127.0.0.1:{port}/health"))
        .timeout(Duration::from_secs(3))
        .send()
        .await
    {
        Ok(res) if res.status().is_success() => {
            let body: serde_json::Value = res.json().await.unwrap_or(json!({"ok": true}));
            println!("[CodeStation] Daemon is RUNNING on http://127.0.0.1:{port}");
            println!("{}", serde_json::to_string_pretty(&body).unwrap_or_default());
            Ok(())
        }
        _ => {
            println!("[CodeStation] Daemon is NOT running on http://127.0.0.1:{port}");
            std::process::exit(1);
        }
    }
}

fn print_token() -> Result<()> {
    let token_path = service::data_dir().join("token");
    if let Ok(token) = std::fs::read_to_string(&token_path) {
        println!("{}", token.trim());
        return Ok(());
    }
    let legacy_token = service::home().join(".codync-dev").join("token");
    if let Ok(token) = std::fs::read_to_string(&legacy_token) {
        println!("{}", token.trim());
        return Ok(());
    }
    eprintln!("[CodeStation] No token found in {} or {}", token_path.display(), legacy_token.display());
    std::process::exit(1);
}

async fn stop(port: u16) -> Result<()> {
    println!("[CodeStation] Stopping daemon on port {port}...");
    #[cfg(unix)]
    {
        let _ = std::process::Command::new("fuser")
            .arg("-k")
            .arg("-n")
            .arg("tcp")
            .arg(port.to_string())
            .output();
        let _ = std::process::Command::new("pkill")
            .arg("-f")
            .arg("codestation.*serve")
            .output();
        let _ = std::process::Command::new("pkill")
            .arg("-f")
            .arg("codestation-host.*serve")
            .output();
    }
    println!("[CodeStation] Stopped.");
    Ok(())
}

async fn start(bind: &str, port: u16) -> Result<()> {
    if let Ok(res) = http()
        .get(format!("http://127.0.0.1:{port}/health"))
        .timeout(Duration::from_secs(2))
        .send()
        .await
        && res.status().is_success()
    {
        println!("[CodeStation] Daemon is already running on http://{bind}:{port}");
        return Ok(());
    }

    println!("[CodeStation] Starting daemon on http://{bind}:{port} in background...");
    let exe = std::env::current_exe().context("resolving current executable")?;
    let data_dir = service::data_dir();
    let _ = std::fs::create_dir_all(&data_dir);
    let log_file = std::fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(data_dir.join("host.log"))
        .context("opening host.log")?;

    let mut child = std::process::Command::new(exe);
    child.args(["serve", "--bind", bind, "--port", &port.to_string()])
        .stdout(log_file.try_clone().unwrap())
        .stderr(log_file);

    let child = child.spawn().context("spawning codestation host daemon")?;
    let pid = child.id();
    let _ = std::fs::write(data_dir.join("host.pid"), pid.to_string());
    println!("[CodeStation] Daemon spawned with PID {pid}. Log: {}", data_dir.join("host.log").display());

    for _ in 0..25 {
        tokio::time::sleep(Duration::from_millis(200)).await;
        if let Ok(res) = http()
            .get(format!("http://127.0.0.1:{port}/health"))
            .timeout(Duration::from_millis(500))
            .send()
            .await
            && res.status().is_success()
        {
            println!("[CodeStation] Daemon is RUNNING on http://{bind}:{port}");
            return Ok(());
        }
    }
    println!("[CodeStation] Daemon spawned, still initializing. Check logs with: cat {}", data_dir.join("host.log").display());
    Ok(())
}

async fn doctor(port: u16) -> Result<()> {
    println!("=== CODESTATION DOCTOR DIAGNOSTICS ===");
    let data_dir = service::data_dir();
    println!("Configuration directory: {}", data_dir.display());
    let token_path = data_dir.join("token");
    if token_path.exists() {
        println!("  - Token: PRESENT ({})", token_path.display());
    } else {
        println!("  - Token: MISSING (will be generated on serve)");
    }

    print!("Loopback server status (port {port}): ");
    match http()
        .get(format!("http://127.0.0.1:{port}/health"))
        .timeout(Duration::from_secs(2))
        .send()
        .await
    {
        Ok(res) if res.status().is_success() => {
            println!("RUNNING (HTTP 200)");
        }
        _ => {
            println!("STOPPED / UNREACHABLE");
        }
    }

    println!("\nInstalled AI Agent CLIs:");
    let agents = [
        ("Google Antigravity (agy)", "agy"),
        ("OpenAI Codex (codex)", "codex"),
        ("OpenCode (opencode)", "opencode"),
        ("Google Gemini CLI (gemini)", "gemini"),
        ("Claude Code (claude)", "claude"),
        ("GitHub Copilot (copilot)", "copilot"),
        ("Qwen Code (qwen)", "qwen"),
    ];

    for (name, bin) in agents {
        let found = std::process::Command::new("which")
            .arg(bin)
            .output()
            .ok()
            .filter(|o| o.status.success())
            .and_then(|o| String::from_utf8(o.stdout).ok())
            .map(|s| s.trim().to_string());

        match found {
            Some(path) => println!("  [OK] {:<28} -> {}", name, path),
            None => println!("  [--] {:<28} -> Not installed or not in PATH", name),
        }
    }

    println!("======================================");
    Ok(())
}

/// launchd appends to `host.log` forever. The service already holds it open as
/// stdout (append mode), so truncate in place rather than rotating.
fn cap_log() {
    let log = service::data_dir().join("host.log");
    if std::fs::metadata(&log).is_ok_and(|m| m.len() > 10 * 1024 * 1024)
        && let Ok(f) = std::fs::OpenOptions::new().write(true).open(&log)
    {
        let _ = f.set_len(0);
    }
}

async fn serve(bind: &str, port: u16) -> Result<()> {
    let _instance_lock = tokio::task::spawn_blocking(service::lock_host).await??;
    tokio::task::spawn_blocking(service::capture_binary_identity).await??;
    let listener =
        tokio::net::TcpListener::bind((bind, port)).await.with_context(|| format!("binding {bind}:{port}"))?;
    cap_log();
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env().unwrap_or_else(|_| "codync_host=info".into()),
        )
        // A failed log write would be reported with `eprintln!`, which panics when stderr is gone
        // too (a closed terminal, a full disk): a log line must never take a task down.
        .log_internal_errors(false)
        .init();
    let (store, host_id, token) = open_store()?;
    write_token_file(&token)?;
    let identity = identity::Identity::load_or_create(&service::data_dir())?;
    tokio::task::spawn_blocking(backends::hydrate_path).await?;
    let hub = hub::Hub::new(store, host_id, identity, token, port);
    hub.start()?;
    analytics::host_started(&hub, service::started_by_service());
    background::start(&hub);
    tracing::info!(version = env!("CARGO_PKG_VERSION"), bind, port, "codync-host listening");
    // Peer addresses: some settings may only be changed from this computer.
    let app = api::router(hub.clone()).into_make_service_with_connect_info::<std::net::SocketAddr>();
    // SSE/WebSocket clients can stay connected forever. Stop agents immediately
    // on the signal instead of waiting for those connections to finish.
    let server = axum::serve(listener, app).into_future();
    tokio::pin!(server);
    tokio::select! {
        result = &mut server => result?,
        () = shutdown_signal() => {},
    }
    hub.shutdown().await;
    agent::acp::stopped().await;
    analytics::flush_before_exit(&hub).await;
    tracing::info!("codync-host stopped");
    Ok(())
}

/// Resolves on Ctrl-C or SIGTERM.
async fn shutdown_signal() {
    let ctrl_c = async {
        if let Err(error) = tokio::signal::ctrl_c().await {
            tracing::warn!(%error, "can't listen for Ctrl-C");
            std::future::pending::<()>().await;
        }
    };
    #[cfg(unix)]
    let term = async {
        match tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate()) {
            Ok(mut s) => {
                s.recv().await;
            }
            Err(error) => {
                tracing::warn!(%error, "can't listen for SIGTERM");
                std::future::pending::<()>().await;
            }
        }
    };
    #[cfg(not(unix))]
    let term = std::future::pending::<()>();
    tokio::select! {
        () = ctrl_c => {}
        () = term => {}
    }
}

/// Calls the running host's local API (`~/.codync/token` over loopback).
async fn local_call(port: u16, method: &str, body: serde_json::Value) -> Result<serde_json::Value> {
    let not_running =
        || anyhow::anyhow!("codync-host isn't running here. Start it with `codync-host install` (or `serve`).");
    let token = std::fs::read_to_string(service::data_dir().join("token")).map_err(|_| not_running())?;
    let res = http()
        .post(format!("http://127.0.0.1:{port}/api/{method}"))
        .bearer_auth(token.trim())
        .json(&body)
        .timeout(Duration::from_secs(20))
        .send()
        .await
        .map_err(|_| not_running())?;
    let status = res.status();
    let v: serde_json::Value = res.json().await.context("reading the host's answer")?;
    if !status.is_success() {
        anyhow::bail!("{}", v["error"].as_str().unwrap_or("the host refused"));
    }
    Ok(v)
}

async fn pair(port: u16, as_json: bool) -> Result<()> {
    let hello = local_call(port, "hello", json!({})).await?;
    let p = local_call(port, "pairing", json!({})).await?;
    if as_json {
        println!(
            "{}",
            json!({
                "name": hello["name"], "computerId": hello["computerId"], "signKey": hello["signKey"],
                "boxKey": hello["boxKey"], "urls": p["urls"], "cloud": hello["cloud"],
                "pairingUrl": p["pairingUrl"], "expiresAt": p["expiresAt"],
            })
        );
        return Ok(());
    }
    let url = p["pairingUrl"].as_str().unwrap_or_default();
    let code = qrcode::QrCode::new(url.as_bytes())?;
    println!("{}", code.render::<qrcode::render::unicode::Dense1x2>().quiet_zone(true).build());
    println!("Scan with the Codync iOS app within 10 minutes, or open this link on the phone:\n{url}\n");
    if hello["cloud"].is_null()
        && p["urls"].as_array().is_none_or(|u| {
            !u.iter().any(|u| {
                u.as_str().is_some_and(|u| u.contains("://100.") || u.contains(".ts.net") || u.starts_with("https://"))
            })
        })
    {
        println!(
            "Tip: the phone reaches this computer on the same network only. Turn on the Codync cloud or Tailscale to reach it from anywhere."
        );
    }
    Ok(())
}

/// Reads the identity and token only; never creates them (that's `serve`'s job).
async fn info(port: u16, as_json: bool) -> Result<()> {
    let dir = service::data_dir();
    let (Some(id), Ok(token)) = (identity::Identity::load(&dir)?, std::fs::read_to_string(dir.join("token"))) else {
        eprintln!("start codync-host once first");
        std::process::exit(1);
    };
    let running = http()
        .get(format!("http://127.0.0.1:{port}/health"))
        .timeout(Duration::from_secs(2))
        .send()
        .await
        .is_ok_and(|r| r.status().is_success());
    let v = json!({
        "name": service::host_name(),
        "computerId": id.computer_id(),
        "signKey": id.sign_pub_b64(),
        "boxKey": id.box_pub_b64(),
        "version": env!("CARGO_PKG_VERSION"),
        "port": port,
        "token": token.trim(),
        "running": running,
    });
    if as_json {
        println!("{v}");
    } else {
        println!(
            "name:     {}\ncomputer: {}\nversion:  {}\nport:     {port}\nrunning:  {running}",
            v["name"].as_str().unwrap_or_default(),
            v["computerId"].as_str().unwrap_or_default(),
            env!("CARGO_PKG_VERSION"),
        );
    }
    Ok(())
}

/// Never blocks Claude Code: 1 s budget, all errors ignored.
async fn statusline(port: u16, wrapped: String) {
    let mut input = String::new();
    let _ = std::io::stdin().read_to_string(&mut input);
    let v: serde_json::Value = serde_json::from_str(&input).unwrap_or_default();
    if let Ok(token) = std::fs::read_to_string(service::data_dir().join("token")) {
        let _ = http()
            .post(format!("http://127.0.0.1:{port}/ingest/statusline"))
            .bearer_auth(token.trim())
            .json(&v)
            .timeout(Duration::from_secs(1))
            .send()
            .await;
    }
    if !wrapped.trim().is_empty() {
        // The user's own status line gets the same JSON on stdin.
        use std::io::Write;
        if let Ok(mut child) = shell::line(&wrapped).std().stdin(std::process::Stdio::piped()).spawn() {
            if let Some(mut stdin) = child.stdin.take() {
                let _ = stdin.write_all(input.as_bytes());
            }
            let _ = child.wait();
        }
        return;
    }
    let model = v["model"]["display_name"].as_str().unwrap_or("Claude");
    let mut line = model.to_owned();
    for (k, label) in [("five_hour", "5h"), ("seven_day", "7d")] {
        if let Some(p) = v["rate_limits"][k]["used_percentage"].as_f64() {
            let _ = write!(line, " · {label} {p:.0}%");
        }
    }
    println!("{line}");
}
