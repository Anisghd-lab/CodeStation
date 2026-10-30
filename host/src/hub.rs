//! Shared host state: store, event fan-out, bot actors, push, usage.

use crate::LockExt;
use crate::agent::bot::{self, BotHandle, Cmd};
use crate::api::devices::Pairing;
use crate::remote::identity::Identity;
use crate::service;
use crate::store::{BotConfig, Entry, EntryKind, Lane, Store};
use crate::usage::Usage;
use anyhow::{Result, anyhow, bail};
use serde::Serialize;
use serde_json::{Value, json};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::sync::{broadcast, watch};

mod order;
mod reactions;
mod sync;
mod validate;
pub(crate) use validate::validate;

/// What a bot is doing right now (wire values: `idle` / `working` / `needsInput` / `error`).
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum BotStatus {
    #[default]
    Idle,
    Working,
    NeedsInput,
    Error,
}

#[derive(Clone, Default, Debug)]
pub struct Runtime {
    pub status: BotStatus,
    pub activity: String,
    pub started_at: Option<i64>,
    /// Where the running turn talks: its own chat or a group.
    pub lane: Option<Lane>,
    /// The user's message that started the running turn, typed in the bot's own chat; `None` for a turn a routine,
    /// a room, another bot or the host started. Memory cites it as where a fact was learned, and only such a turn
    /// may change or take out what memory says.
    pub message: Option<String>,
    /// The ACP session the running turn prompts. A memory write made there is one that session already knows.
    pub session: Option<String>,
    pub fast_mode: crate::agent::config::FastModeSupport,
    /// The fallback the bot is on; `None` on its own agent (see `agent::fallback`).
    pub agent_step: Option<AgentStep>,
}

/// The bot runs on one of its fallbacks: which, since when, and why it left the agent before.
#[derive(Clone, Debug, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentStep {
    pub step: usize,
    pub agent: String,
    pub model: Option<String>,
    pub since: i64,
    pub reason: Option<crate::agent::fallback::Reason>,
}

impl AgentStep {
    pub fn new(cfg: &BotConfig, step: usize, reason: Option<crate::agent::fallback::Reason>) -> Self {
        Self { step, agent: cfg.backend.clone(), model: cfg.model.clone(), since: crate::store::now_ms(), reason }
    }
}

pub struct Hub {
    pub analytics: crate::analytics::Analytics,
    pub routines: crate::routines::Routines,
    pub team: crate::chat::team::Requests,
    pub groups: crate::chat::group::Rooms,
    pub store: Store,
    pub collective_memory: Arc<crate::collective_memory::MemoryEngine>,
    pub host_id: String,
    pub identity: Identity,
    /// Loopback-only bearer token for this computer's own apps and helpers.
    pub token: String,
    pub port: u16,
    pub events: broadcast::Sender<Value>,
    /// Held across "mutate store + broadcast" so events leave in rev order.
    emit_lock: Mutex<()>,
    bots: Mutex<HashMap<String, BotHandle>>,
    runtime: Mutex<HashMap<String, Runtime>>,
    pub usage: Mutex<Usage>,
    keep_awake: Mutex<service::KeepAwake>,
    /// Generation of the authorized-device table and pairing offers: bumped on every
    /// change so open channels re-check their device (and the ACL is republished).
    pub auth: watch::Sender<u64>,
    pub pairing: Mutex<Pairing>,
    /// Device key -> open channels that have exchanged at least one frame.
    pub connected: Mutex<HashMap<String, usize>>,
    /// Setup terminals (installs, sign-ins).
    pub terms: Arc<crate::agent::term::Terms>,
}

impl Hub {
    pub fn busy(&self) -> bool {
        self.runtime.locked().values().any(|r| matches!(r.status, BotStatus::Working | BotStatus::NeedsInput))
            || self.terms.busy()
    }

    pub fn new(store: Store, host_id: String, identity: Identity, token: String, port: u16) -> Arc<Self> {
        let (events, _) = broadcast::channel(1024);
        let mem_db = service::data_dir().join("codync.db");
        let mem_repo = std::path::PathBuf::from("/root/CodeStation-Memory");
        let collective_memory = Arc::new(
            crate::collective_memory::MemoryEngine::new(&mem_db, &mem_repo).unwrap_or_else(|e| {
                tracing::warn!("Failed to initialize collective memory on {}: {e:#}", mem_db.display());
                crate::collective_memory::MemoryEngine::new(std::path::Path::new(":memory:"), &mem_repo)
                    .expect("fallback memory engine")
            })
        );
        Arc::new(Self {
            analytics: crate::analytics::Analytics::load(&store),
            routines: crate::routines::Routines::default(),
            team: crate::chat::team::Requests::default(),
            groups: crate::chat::group::Rooms::default(),
            store,
            collective_memory,
            host_id,
            identity,
            token,
            port,
            events,
            emit_lock: Mutex::new(()),
            bots: Mutex::default(),
            runtime: Mutex::default(),
            usage: Mutex::default(),
            keep_awake: Mutex::default(),
            auth: watch::Sender::new(0),
            pairing: Mutex::default(),
            connected: Mutex::default(),
            terms: Arc::default(),
        })
    }

    pub fn start(self: &Arc<Self>) -> Result<()> {
        // Approval cards and queued sends from a previous run can't be honored anymore.
        self.store.expire_pending()?;
        crate::chat::memory::import_old_memory(self);
        self.routines.restore(&self.store)?;
        crate::market::turn_on_installed_once(&self.store)?;
        let _ = self.ensure_default_bots();
        for row in self.store.bots()? {
            if row.deleted {
                self.routines.disable_bot(&self.store, &row.config.id)?;
            } else if !row.config.is_group() {
                self.spawn_bot(row.config);
            }
        }
        crate::chat::calls::rewatch(self);
        crate::routines::start(self);
        crate::analytics::start(self);
        Ok(())
    }

    fn spawn_bot(self: &Arc<Self>, cfg: BotConfig) {
        let id = cfg.id.clone();
        self.runtime.locked().insert(id.clone(), Runtime::default());
        let handle = bot::spawn(self.clone(), cfg);
        self.bots.locked().insert(id, handle);
    }

    /// Stops every bot actor (and so every agent process), waiting briefly for them to exit.
    pub async fn shutdown(&self) {
        self.routines.stop().await;
        let handles: Vec<BotHandle> = self.bots.locked().drain().map(|(_, h)| h).collect();
        let tasks: Vec<_> = handles
            .into_iter()
            .map(|h| {
                let _ = h.tx.send(Cmd::Shutdown);
                h.task
            })
            .collect();
        if tokio::time::timeout(Duration::from_secs(5), futures::future::join_all(tasks)).await.is_err() {
            tracing::warn!("some bots didn't stop within 5 s");
        }
        self.keep_awake.locked().set(false);
    }

    pub fn send_cmd(&self, bot_id: &str, cmd: Cmd) -> Result<()> {
        let bots = self.bots.locked();
        let target_id = if bots.contains_key(bot_id) {
            bot_id.to_string()
        } else {
            match self.store.resolve_bot(bot_id) {
                Ok(Some(row)) => row.config.id,
                _ => bot_id.to_string(),
            }
        };

        let h = bots.get(&target_id).ok_or_else(|| anyhow!("unknown bot"))?;
        h.tx.send(cmd).map_err(|_| anyhow!("bot stopped"))
    }

    fn ensure_default_bots(&self) -> Result<()> {
        let existing = self.store.bots()?;
        let existing_ids: std::collections::HashSet<_> = existing.iter().filter(|b| !b.deleted).map(|b| b.config.id.as_str()).collect();

        // 1. Google Antigravity
        if !existing_ids.contains("bot-antigravity") {
            let cfg: BotConfig = serde_json::from_value(serde_json::json!({
                "id": "bot-antigravity",
                "kind": "agent",
                "name": "Google Antigravity",
                "description": "Google Antigravity Agent en conteneur PRoot isolé",
                "avatarColor": "#4285F4",
                "avatarShape": "blob",
                "backend": "agy",
                "cwd": "/root/CodeStation",
                "permission": "auto",
                "fastMode": true,
                "pinned": true,
                "position": 4,
            }))?;
            let _ = self.store.save_bot(&cfg);
        }

        // 2. OpenCode GLM
        if !existing_ids.contains("bot-opencode-glm") {
            let cfg: BotConfig = serde_json::from_value(serde_json::json!({
                "id": "bot-opencode-glm",
                "kind": "agent",
                "name": "OpenCode GLM",
                "description": "Agent OpenCode propulsé par GLM-5.3-Flash Free",
                "avatarColor": "#A855F7",
                "avatarShape": "hex",
                "backend": "opencode",
                "cwd": "/root/CodeStation",
                "permission": "auto",
                "model": "opencode/glm-5.3-flash-free",
                "fastMode": false,
                "pinned": true,
                "position": 1,
            }))?;
            let _ = self.store.save_bot(&cfg);
        }

        // 3. CodeBuddy Code
        if !existing_ids.contains("bot-codebuddy") {
            let cfg: BotConfig = serde_json::from_value(serde_json::json!({
                "id": "bot-codebuddy",
                "kind": "agent",
                "name": "CodeBuddy Code",
                "description": "Agent CodeBuddy Code propulsé par la CLI locale (Hy3 v2.178.1)",
                "avatarColor": "#3B82F6",
                "avatarShape": "circle",
                "backend": "codebuddy",
                "cwd": "/root/CodeStation",
                "permission": "auto",
                "model": "hy3",
                "fastMode": true,
                "pinned": true,
                "position": 5,
            }))?;
            let _ = self.store.save_bot(&cfg);
        }

        Ok(())
    }

    // MARK: bots

    pub fn bot_name(&self, id: &str) -> String {
        self.store.bot(id).ok().flatten().map_or_else(|| "A deleted bot".into(), |b| b.config.name)
    }

    /// A group is busy while one of its members works in it: that member's status and activity.
    pub fn group_runtime(&self, group: &BotConfig) -> Runtime {
        let busy = {
            let runtime = self.runtime.locked();
            group.members.iter().find_map(|m| {
                let r = runtime.get(m)?;
                (r.status != BotStatus::Idle && r.lane.as_ref().is_some_and(|l| l.chat == group.id))
                    .then(|| (m.clone(), r.clone()))
            })
        };
        let Some((member, mut r)) = busy else { return Runtime::default() };
        let name = self.bot_name(&member);
        r.activity = if r.activity.is_empty() { name } else { format!("{name}: {}", r.activity) };
        r
    }

    pub fn create_bot(self: &Arc<Self>, mut cfg: BotConfig) -> Result<Value> {
        cfg.id = uuid::Uuid::new_v4().to_string();
        cfg.created_at = crate::store::now_ms();
        cfg.position = self.new_bot_position()?;
        // No name yet (Grok Bot's flow): it gets one after its first few conversations.
        cfg.auto_name = !cfg.is_group() && cfg.name.trim().is_empty();
        if cfg.auto_name {
            crate::chat::naming::PLACEHOLDER.clone_into(&mut cfg.name);
        }
        if cfg.is_group() {
            cfg.members = crate::chat::group::valid_members(&self.store, &cfg.id, &cfg.members)?;
            // The same bots again: open the group they already share (Grok Bot's rule).
            if let Some(existing) = crate::chat::group::with_members(&self.store, &cfg.members)? {
                return Ok(self.bot_value(&existing).unwrap_or(Value::Null));
            }
        }
        crate::agent::fallback::tidy(&mut cfg);
        validate(&cfg)?;
        crate::agent::workspace::prepare(&mut cfg)?;
        {
            let _g = self.emit_lock.locked();
            self.store.save_bot(&cfg)?;
        }
        if !cfg.is_group() {
            self.spawn_bot(cfg.clone());
            crate::market::agent_sign_in::ask_before_first_message(self, &cfg);
        }
        self.emit_bot(&cfg.id);
        Ok(self.bot_value(&cfg.id).unwrap_or(Value::Null))
    }

    pub fn update_bot(&self, patch: &Value) -> Result<Value> {
        let id = patch["id"].as_str().ok_or_else(|| anyhow!("id required"))?.to_owned();
        let row = self.store.bot(&id)?.filter(|b| !b.deleted).ok_or_else(|| anyhow!("unknown bot"))?;
        let mut merged = serde_json::to_value(&row.config)?;
        for (k, v) in patch.as_object().into_iter().flatten() {
            if !matches!(k.as_str(), "id" | "createdAt" | "createdBy" | "lead") {
                merged[k] = v.clone();
            }
        }
        let mut cfg: BotConfig = serde_json::from_value(merged)?;
        if cfg.name.trim().is_empty() {
            cfg.name.clone_from(&row.config.name);
        }
        // A name the user picked ends automatic naming; the namer passes `autoName` itself.
        if cfg.name != row.config.name && patch.get("autoName").is_none() {
            cfg.auto_name = false;
        }
        // Options belong to one model; an app that doesn't know them can't clear them itself.
        if (cfg.model != row.config.model || cfg.backend != row.config.backend) && patch.get("modelOptions").is_none() {
            cfg.model_options.clear();
        }
        if cfg.kind != row.config.kind {
            bail!("a bot can't become a group, or a group a bot");
        }
        if cfg.is_group() {
            cfg.members = crate::chat::group::valid_members(&self.store, &cfg.id, &cfg.members)?;
        }
        crate::agent::fallback::tidy(&mut cfg);
        validate(&cfg)?;
        crate::agent::workspace::prepare(&mut cfg)?;
        crate::agent::own_memory::settle(&mut cfg, &row.config, patch);
        {
            let _g = self.emit_lock.locked();
            self.store.save_bot(&cfg)?;
        }
        if !cfg.is_group() {
            if crate::chat::memory::shared(&cfg) != crate::chat::memory::shared(&row.config) {
                crate::chat::memory::changed(self, Some(&id));
            }
            self.send_cmd(&id, Cmd::Reconfigure(Box::new(cfg)))?;
        }
        self.emit_bot(&id);
        Ok(self.bot_value(&id).unwrap_or(Value::Null))
    }

    pub fn delete_bot(&self, id: &str) -> Result<()> {
        crate::chat::lead::may_delete(&self.store, id)?;
        self.routines.disable_bot(&self.store, id)?;
        self.team.end_turn(id);
        self.groups.stop(self, id);
        let _ = self.send_cmd(id, Cmd::Shutdown);
        self.bots.locked().remove(id);
        self.runtime.locked().remove(id);
        {
            let _g = self.emit_lock.locked();
            let rev = self.store.delete_bot(id)?;
            let _ =
                self.events.send(json!({"type": "bot", "rev": rev, "bot": {"id": id, "deleted": true, "rev": rev}}));
        }
        // Its shared files and artifacts are unreachable once the bot is deleted.
        let folders = [crate::chat::files::root(id), crate::chat::artifacts::root(id)];
        let bot = id.to_owned();
        tokio::task::spawn_blocking(move || {
            crate::chat::memory::files::remove_bot(&bot);
            for folder in folders {
                if let Err(error) = std::fs::remove_dir_all(&folder)
                    && error.kind() != std::io::ErrorKind::NotFound
                {
                    tracing::warn!(error = %error, "couldn't remove a deleted bot's shared files or artifacts");
                }
            }
        });
        // A deleted bot leaves its groups; what it said there stays.
        for row in self.store.bots()? {
            let mut group = row.config;
            if !row.deleted && group.is_group() && group.members.iter().any(|m| m == id) {
                group.members.retain(|m| m != id);
                {
                    let _g = self.emit_lock.locked();
                    self.store.save_bot(&group)?;
                }
                self.emit_bot(&group.id);
            }
        }
        Ok(())
    }

    /// Marks what the user has seen as read. Nothing unread there is a no-op, so clients
    /// may call it whenever a chat is on screen.
    pub fn mark_read(&self, id: &str) -> Result<()> {
        let Some(row) = self.store.bot(id)? else { bail!("unknown bot") };
        if self.store.unread(id, row.read_rev) == 0 {
            return Ok(());
        }
        let _g = self.emit_lock.locked();
        self.store.mark_read(id)?;
        if let Some(v) = self.bot_value(id) {
            let _ = self.events.send(json!({"type": "bot", "rev": v["rev"], "bot": v}));
        }
        Ok(())
    }

    #[cfg(test)]
    pub fn bot_value_for_test(&self, id: &str) -> Value {
        self.bot_value(id).unwrap_or(Value::Null)
    }

    fn bot_value(&self, id: &str) -> Option<Value> {
        self.store.bot(id).ok().flatten().map(|r| self.bot_json(&r))
    }

    /// Saves a change that stamps its own rev and broadcasts the event `save` returns, in rev order.
    pub fn emit_change(&self, save: impl FnOnce() -> Result<Value>) -> Result<()> {
        let _g = self.emit_lock.locked();
        let _ = self.events.send(save()?);
        Ok(())
    }

    /// Bumps the bot's rev and broadcasts its current state.
    pub fn emit_bot(&self, id: &str) {
        let _g = self.emit_lock.locked();
        if self.store.touch_bot(id).is_ok()
            && let Some(v) = self.bot_value(id)
        {
            let _ = self.events.send(json!({"type": "bot", "rev": v["rev"], "bot": v}));
        }
    }

    pub fn set_runtime(&self, id: &str, f: impl FnOnce(&mut Runtime)) {
        let (before, after) = {
            let mut map = self.runtime.locked();
            let rt = map.entry(id.to_owned()).or_default();
            let before = rt.clone();
            f(rt);
            (before, rt.clone())
        };
        if before.status != after.status
            || before.activity != after.activity
            || before.lane != after.lane
            || before.fast_mode != after.fast_mode
            || before.agent_step != after.agent_step
        {
            self.emit_bot(id);
            // A group shows what its busy member is doing.
            for lane in [&before.lane, &after.lane].into_iter().flatten() {
                if lane.chat != id {
                    self.emit_bot(&lane.chat);
                }
            }
            self.update_keep_awake();
        }
    }

    pub fn runtime(&self, id: &str) -> Runtime {
        self.runtime.locked().get(id).cloned().unwrap_or_default()
    }

    pub(crate) fn update_keep_awake(&self) {
        let busy = self.runtime.locked().values().any(|r| r.status == BotStatus::Working);
        self.keep_awake.locked().set(busy || self.routines.keeps_awake());
    }

    // MARK: entries

    /// Persists and broadcasts a new transcript entry. A failed write is logged here
    /// (once) and yields `None`: the conversation goes on without that line.
    pub fn add_entry(&self, lane: &Lane, kind: EntryKind, turn: i64, data: &Value) -> Option<Entry> {
        let _g = self.emit_lock.locked();
        match self.store.insert_entry(lane, kind, turn, data) {
            Ok(e) => {
                let _ = self.events.send(json!({"type": "entry", "rev": e.rev, "entry": e}));
                Some(e)
            }
            Err(error) => {
                tracing::error!(
                    error = format!("{error:#}"),
                    chat = lane.chat,
                    kind = kind.as_str(),
                    "couldn't save transcript entry"
                );
                None
            }
        }
    }

    pub fn add_entry_once(
        &self,
        id: &str,
        lane: &Lane,
        kind: EntryKind,
        turn: i64,
        data: &Value,
    ) -> Result<(Entry, bool)> {
        let _guard = self.emit_lock.locked();
        let (entry, added) = self.store.insert_entry_once(id, lane, kind, turn, data)?;
        if added {
            let _ = self.events.send(json!({"type":"entry","rev":entry.rev,"entry":entry}));
        }
        Ok((entry, added))
    }

    /// Updates and broadcasts an entry; failures are logged here, like [`Self::add_entry`].
    pub fn set_entry(&self, id: &str, data: &Value) -> Option<Entry> {
        let _g = self.emit_lock.locked();
        match self.store.update_entry(id, data) {
            Ok(e) => {
                if let Some(e) = &e {
                    let _ = self.events.send(json!({"type": "entry", "rev": e.rev, "entry": e}));
                }
                e
            }
            Err(error) => {
                tracing::error!(error = format!("{error:#}"), entry = id, "couldn't update transcript entry");
                None
            }
        }
    }

    /// Changes part of an entry and broadcasts it, reading and writing under the emission lock: an edit made
    /// meanwhile elsewhere (a reaction from another device) isn't written over. `edit` returns `false` to leave the
    /// entry as it is.
    pub fn edit_entry(&self, id: &str, edit: impl FnOnce(&mut Entry) -> Result<bool>) -> Result<Entry> {
        let _g = self.emit_lock.locked();
        let mut e = self.store.entry(id).ok_or_else(|| anyhow!("unknown message"))?;
        if !edit(&mut e)? {
            return Ok(e);
        }
        let e = self.store.update_entry(id, &e.data)?.ok_or_else(|| anyhow!("unknown message"))?;
        let _ = self.events.send(json!({"type": "entry", "rev": e.rev, "entry": e}));
        Ok(e)
    }

    pub fn set_usage(&self, usage: Usage) {
        let _ = self.events.send(json!({"type": "usage", "usage": usage}));
        *self.usage.locked() = usage;
    }

    // MARK: devices

    /// Tells open channels (and the ACL publisher) that devices or pairing offers changed.
    pub fn auth_changed(&self) {
        self.auth.send_modify(|g| *g += 1);
    }

    /// Removes an authorized device: its tickets go with it and its open channels close.
    pub fn revoke_device(&self, key: &str) -> Result<bool> {
        let removed = self.store.remove_device(key)?;
        if removed {
            tracing::info!(device = key, "device revoked");
            self.auth_changed();
        }
        Ok(removed)
    }

}
