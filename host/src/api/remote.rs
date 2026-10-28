//! Devices and pairing for the one local device. CodeStation is single-device
//! local-only: there are no accounts, no push tickets, no cloud claims here.

use super::devices::Caller;
use super::str_arg;
use crate::LockExt;
use crate::hub::Hub;
use anyhow::{Result, anyhow};
use serde_json::{Value, json};
use std::ops::ControlFlow;
use std::sync::Arc;

pub(super) async fn call(hub: &Arc<Hub>, _caller: &Caller, method: &str, b: Value) -> Result<ControlFlow<Value, Value>> {
    Ok(ControlFlow::Break(match method {
        "devices" => {
            let connected = hub.connected.locked().clone();
            let devices: Vec<Value> = hub
                .store
                .devices()?
                .into_iter()
                .map(|d| {
                    let mut v = serde_json::to_value(&d).expect("Device is plain data and always serializes");
                    v["connected"] = connected.contains_key(&d.key).into();
                    v
                })
                .collect();
            json!({"devices": devices})
        }
        "revokeDevice" => {
            let key = str_arg(&b, "key")?;
            hub.store.device(key).ok_or_else(|| anyhow!("unknown device"))?;
            hub.revoke_device(key)?;
            json!({})
        }
        "pairing" => pairing(hub).await?,
        _ => return Ok(ControlFlow::Continue(b)),
    }))
}

/// A new one-time pairing code and the QR that carries it (§4.1), on the loopback only.
async fn pairing(hub: &Arc<Hub>) -> Result<Value> {
    let port = hub.port;
    let urls = tokio::task::spawn_blocking(move || crate::service::addresses(port)).await?;
    if urls.is_empty() {
        anyhow::bail!("No loopback address is available; is the host running?");
    }
    let issued = hub.pairing.locked().issue();
    hub.auth_changed();
    let url = crate::service::pairing_url(&crate::service::PairingQr {
        name: &crate::service::host_name(),
        computer_id: &hub.identity.computer_id(),
        sign_key: &hub.identity.sign_pub_b64(),
        box_key: &hub.identity.box_pub_b64(),
        code: &issued.code,
        urls: &urls,
    });
    Ok(json!({"pairingUrl": url, "urls": urls, "expiresAt": issued.expires_at}))
}
