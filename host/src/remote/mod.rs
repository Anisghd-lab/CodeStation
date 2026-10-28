//! Reaching this computer from its own device over the local loop: identity keys,
//! wire crypto and the direct E2E channel. CodeStation is single-device local-only:
//! there is no cloud relay, no accounts and no push here.
pub mod channel;
pub mod crypto;
pub mod identity;
