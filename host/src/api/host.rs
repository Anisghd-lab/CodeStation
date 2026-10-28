//! Methods about this computer itself: hello/sync, agents and setup
//! terminals, usage, folders and voice.

use super::devices::Caller;
use super::str_arg;
use crate::LockExt;
use crate::agent::backends;
use crate::hub::Hub;
use crate::usage;
use anyhow::{Context, Result, anyhow};
use serde_json::{Value, json};
use std::ops::ControlFlow;
use std::sync::Arc;

pub(super) async fn call(hub: &Arc<Hub>, _caller: &Caller, method: &str, b: Value) -> Result<ControlFlow<Value, Value>> {
    Ok(ControlFlow::Break(match method {
        "voiceStatus" => crate::voice::status(&hub.store)?,
        "setVoiceKey" => {
            let provider = crate::voice::Provider::parse(str_arg(&b, "provider")?)?;
            crate::voice::set_key(&hub.store, provider, b["key"].as_str().unwrap_or_default()).await?
        }
        "voiceSession" => {
            let provider = crate::voice::Provider::parse(str_arg(&b, "provider")?)?;
            crate::voice::session(&hub.store, provider, str_arg(&b, "model")?, str_arg(&b, "voice")?).await?
        }
        "voiceModels" => {
            crate::voice::models(&hub.store, crate::voice::Provider::parse(str_arg(&b, "provider")?)?).await?
        }
        "voiceTranscribe" => {
            let provider = crate::voice::Provider::parse(str_arg(&b, "provider")?)?;
            crate::voice::transcribe(&hub.store, provider, str_arg(&b, "model")?, str_arg(&b, "audio")?).await?
        }
        "voiceSpeak" => {
            let provider = crate::voice::Provider::parse(str_arg(&b, "provider")?)?;
            crate::voice::speak(
                &hub.store,
                provider,
                str_arg(&b, "model")?,
                str_arg(&b, "voice")?,
                str_arg(&b, "text")?,
            )
            .await?
        }
        "hello" => {
            let port = hub.port;
            json!({
                "hostId": hub.host_id,
                "computerId": hub.identity.computer_id(),
                "signKey": hub.identity.sign_pub_b64(),
                "boxKey": hub.identity.box_pub_b64(),
                "name": crate::service::host_name(),
                "version": env!("CARGO_PKG_VERSION"),
                "minApp": crate::compat::MIN_APP,
                "os": std::env::consts::OS,
                "device": tokio::task::spawn_blocking(crate::service::device).await?,
                "home": dirs::home_dir().map(|p| p.to_string_lossy().into_owned()),
                "backends": backends::list(),
                "rev": hub.store.current_rev(),
                "analytics": hub.analytics.state(),
                // Shells out to `tailscale`: keep it off the async workers.
                "urls": tokio::task::spawn_blocking(move || crate::service::addresses(port)).await?,
            })
        }
        "refreshBackends" => {
            tokio::task::spawn_blocking(backends::hydrate_path).await?;
            crate::market::agent_sign_in::refresh(hub).await;
            json!({"backends": backends::list()})
        }
        "usage" => {
            if b["refresh"].as_bool().unwrap_or(false) {
                usage::refresh(hub).await;
            }
            serde_json::to_value(&*hub.usage.locked())?
        }
        "listDirs" => {
            let path = b["path"].as_str().map(std::path::PathBuf::from);
            tokio::task::spawn_blocking(move || list_dirs(path)).await??
        }
        "agentSetup" => {
            let step: crate::agent::term::Step =
                serde_json::from_value(b["step"].clone()).context("`step` is install or login")?;
            let (cols, rows) = term_size(&b);
            // Node.js belongs to the computer, not to one agent.
            let backend = if step == crate::agent::term::Step::Node { "" } else { str_arg(&b, "backend")? };
            json!({"term": hub.terms.start(backend, step, b["method"].as_str(), cols, rows).await?})
        }
        "agentModels" => crate::agent::auth::models(&hub.store, str_arg(&b, "backend")?).await?,
        "agentAuth" | "agentAuthenticate" | "setAgentEnv" => {
            let backend = str_arg(&b, "backend")?;
            let status = match method {
                "agentAuth" => crate::agent::auth::check(&hub.store, backend).await?,
                "agentAuthenticate" => {
                    crate::agent::auth::authenticate(&hub.store, backend, str_arg(&b, "method")?).await?
                }
                _ => {
                    let vars = b["vars"].as_object().ok_or_else(|| anyhow!("`vars` is required"))?;
                    crate::agent::auth::set_env(&hub.store, backend, vars).await?
                }
            };
            // Chats that asked the user to sign in get their messages answered now.
            if status["signedIn"] == true {
                crate::market::agent_sign_in::agent_signed_in(hub, backend)?;
            }
            status
        }
        "termInput" => {
            hub.terms.write(str_arg(&b, "term")?, str_arg(&b, "data")?)?;
            json!({})
        }
        "termResize" => {
            let (cols, rows) = term_size(&b);
            hub.terms.resize(str_arg(&b, "term")?, cols, rows)?;
            json!({})
        }
        "termClose" => {
            hub.terms.close(str_arg(&b, "term")?);
            json!({})
        }
        "setAnalytics" => {
            hub.analytics.set_enabled(&hub.store, b["enabled"].as_bool().unwrap_or(false))?;
            json!({"enabled": hub.analytics.state()})
        }
        "track" => {
            crate::analytics::track(hub, &b)?;
            json!({})
        }
        "reportError" => {
            crate::analytics::errors::report(hub, &b)?;
            json!({})
        }
        _ => return Ok(ControlFlow::Continue(b)),
    }))
}

/// Blocking (`std::fs`): call through `spawn_blocking`.
fn list_dirs(path: Option<std::path::PathBuf>) -> Result<Value> {
    let dir = match path {
        Some(p) => p,
        None => dirs::home_dir().ok_or_else(|| anyhow!("no home directory"))?,
    };
    let mut dirs: Vec<Value> = std::fs::read_dir(&dir)
        .with_context(|| format!("listing {}", dir.display()))?
        .filter_map(Result::ok)
        .filter(|e| e.file_type().is_ok_and(|t| t.is_dir()))
        .filter_map(|e| {
            let name = e.file_name().to_string_lossy().into_owned();
            if name.starts_with('.') {
                return None;
            }
            let p = e.path();
            Some(json!({"name": name, "path": p.to_string_lossy(), "isGit": p.join(".git").exists()}))
        })
        .collect();
    dirs.sort_by_key(|d| d["name"].as_str().unwrap_or_default().to_lowercase());
    Ok(json!({
        "path": dir.to_string_lossy(),
        "parent": dir.parent().map(|p| p.to_string_lossy().into_owned()),
        "isGit": dir.join(".git").exists(),
        "dirs": dirs,
    }))
}

fn term_size(b: &Value) -> (u16, u16) {
    let dim = |k: &str, default: u16| b[k].as_u64().and_then(|v| u16::try_from(v).ok()).unwrap_or(default);
    (dim("cols", 80), dim("rows", 24))
}
