//! Background services started alongside the host API.

use crate::{agent::registry, hub::Hub, market, usage, voice};
use std::sync::Arc;

pub fn start(hub: &Arc<Hub>) {
    tokio::spawn({
        let hub = hub.clone();
        async move {
            if let Err(error) = crate::chat::lead::ensure(&hub).await {
                tracing::error!(error = format!("{error:#}"), "couldn't make the lead bot");
            }
        }
    });
    tokio::spawn(registry::refresh_loop());
    tokio::spawn(market::refresh_first_page());
    tokio::spawn(market::automatic_loop(hub.clone()));
    tokio::spawn(voice::refresh_loop(hub.clone()));
    tokio::spawn(crate::chat::calls::sweep_loop(hub.clone()));
    tokio::spawn({
        let hub = hub.clone();
        async move { market::agent_sign_in::refresh(&hub).await }
    });
    tokio::spawn(usage::poll(hub.clone()));
}
