use super::models::{AgentRecord, Experience, Knowledge, Manifest, ManifestCounts, MemoryStatus, Relation, SyncStatus};
use anyhow::{Result, anyhow};
use rusqlite::{Connection, params};
use std::path::{Path, PathBuf};
use std::sync::Mutex;

pub struct MemoryStore {
    db: Mutex<Connection>,
    local_repo_path: PathBuf,
}

impl MemoryStore {
    pub fn new(db_path: &Path, local_repo_path: &Path) -> Result<Self> {
        let conn = Connection::open(db_path)?;
        conn.execute_batch(
            "CREATE TABLE IF NOT EXISTS collective_experiences (
                id TEXT PRIMARY KEY,
                json TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                synced INTEGER NOT NULL DEFAULT 1
            );
            CREATE TABLE IF NOT EXISTS collective_knowledge (
                id TEXT PRIMARY KEY,
                json TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                synced INTEGER NOT NULL DEFAULT 1
            );
            CREATE TABLE IF NOT EXISTS collective_relations (
                id TEXT PRIMARY KEY,
                json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS collective_agents (
                id TEXT PRIMARY KEY,
                json TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS collective_meta (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            );",
        )?;

        let store = Self {
            db: Mutex::new(conn),
            local_repo_path: local_repo_path.to_path_buf(),
        };

        // Bootstrap from local files if DB is empty
        store.bootstrap_from_local_if_needed()?;

        Ok(store)
    }

    fn bootstrap_from_local_if_needed(&self) -> Result<()> {
        let count = {
            let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
            let mut stmt = conn.prepare("SELECT COUNT(*) FROM collective_knowledge")?;
            stmt.query_row([], |r| r.get::<_, i64>(0))?
        };

        if count == 0 && self.local_repo_path.exists() {
            let _ = self.import_from_directory(&self.local_repo_path);
        }
        Ok(())
    }

    pub fn import_from_directory(&self, dir: &Path) -> Result<()> {
        let exp_dir = dir.join("experiences");
        if exp_dir.is_dir() {
            if let Ok(entries) = std::fs::read_dir(exp_dir) {
                for entry in entries.flatten() {
                    let path = entry.path();
                    if path.extension().and_then(|s| s.to_str()) == Some("json") {
                        if let Ok(content) = std::fs::read_to_string(&path) {
                            if let Ok(exp) = serde_json::from_str::<Experience>(&content) {
                                let _ = self.save_experience(&exp, true);
                            }
                        }
                    }
                }
            }
        }

        let knw_dir = dir.join("knowledge");
        if knw_dir.is_dir() {
            if let Ok(entries) = std::fs::read_dir(knw_dir) {
                for entry in entries.flatten() {
                    let path = entry.path();
                    if path.extension().and_then(|s| s.to_str()) == Some("json") {
                        if let Ok(content) = std::fs::read_to_string(&path) {
                            if let Ok(knw) = serde_json::from_str::<Knowledge>(&content) {
                                let _ = self.save_knowledge(&knw, true);
                            }
                        }
                    }
                }
            }
        }

        let agents_dir = dir.join("agents");
        if agents_dir.is_dir() {
            if let Ok(entries) = std::fs::read_dir(agents_dir) {
                for entry in entries.flatten() {
                    let path = entry.path();
                    if path.extension().and_then(|s| s.to_str()) == Some("json") {
                        if let Ok(content) = std::fs::read_to_string(&path) {
                            if let Ok(ag) = serde_json::from_str::<AgentRecord>(&content) {
                                let _ = self.save_agent(&ag);
                            }
                        }
                    }
                }
            }
        }

        let rel_file = dir.join("relations").join("relations.json");
        if rel_file.is_file() {
            if let Ok(content) = std::fs::read_to_string(&rel_file) {
                if let Ok(rels) = serde_json::from_str::<Vec<Relation>>(&content) {
                    for rel in rels {
                        let _ = self.save_relation(&rel);
                    }
                }
            }
        }

        self.set_meta("last_sync_at", &chrono::Utc::now().to_rfc3339())?;
        self.set_meta("sync_status", "synchronized")?;

        Ok(())
    }

    pub fn save_experience(&self, exp: &Experience, synced: bool) -> Result<()> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let json_str = serde_json::to_string(exp)?;
        let now = chrono::Utc::now().timestamp_millis();
        conn.execute(
            "INSERT INTO collective_experiences(id, json, updated_at, synced)
             VALUES(?1, ?2, ?3, ?4)
             ON CONFLICT(id) DO UPDATE SET json=?2, updated_at=?3, synced=?4",
            params![exp.id, json_str, now, if synced { 1 } else { 0 }],
        )?;
        Ok(())
    }

    pub fn get_experience(&self, id: &str) -> Result<Option<Experience>> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let mut stmt = conn.prepare("SELECT json FROM collective_experiences WHERE id = ?1")?;
        let mut rows = stmt.query(params![id])?;
        if let Some(row) = rows.next()? {
            let json_str: String = row.get(0)?;
            let exp = serde_json::from_str(&json_str)?;
            Ok(Some(exp))
        } else {
            Ok(None)
        }
    }

    pub fn list_experiences(&self, limit: usize, offset: usize) -> Result<Vec<Experience>> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let mut stmt = conn.prepare(
            "SELECT json FROM collective_experiences ORDER BY updated_at DESC LIMIT ?1 OFFSET ?2",
        )?;
        let rows = stmt.query_map(params![limit as i64, offset as i64], |r| r.get::<_, String>(0))?;
        let mut results = Vec::new();
        for r in rows.flatten() {
            if let Ok(exp) = serde_json::from_str(&r) {
                results.push(exp);
            }
        }
        Ok(results)
    }

    pub fn save_knowledge(&self, knw: &Knowledge, synced: bool) -> Result<()> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let json_str = serde_json::to_string(knw)?;
        let now = chrono::Utc::now().timestamp_millis();
        conn.execute(
            "INSERT INTO collective_knowledge(id, json, updated_at, synced)
             VALUES(?1, ?2, ?3, ?4)
             ON CONFLICT(id) DO UPDATE SET json=?2, updated_at=?3, synced=?4",
            params![knw.id, json_str, now, if synced { 1 } else { 0 }],
        )?;
        Ok(())
    }

    pub fn get_knowledge(&self, id: &str) -> Result<Option<Knowledge>> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let mut stmt = conn.prepare("SELECT json FROM collective_knowledge WHERE id = ?1")?;
        let mut rows = stmt.query(params![id])?;
        if let Some(row) = rows.next()? {
            let json_str: String = row.get(0)?;
            let knw = serde_json::from_str(&json_str)?;
            Ok(Some(knw))
        } else {
            Ok(None)
        }
    }

    pub fn list_knowledge(&self, limit: usize, offset: usize) -> Result<Vec<Knowledge>> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let mut stmt = conn.prepare(
            "SELECT json FROM collective_knowledge ORDER BY updated_at DESC LIMIT ?1 OFFSET ?2",
        )?;
        let rows = stmt.query_map(params![limit as i64, offset as i64], |r| r.get::<_, String>(0))?;
        let mut results = Vec::new();
        for r in rows.flatten() {
            if let Ok(knw) = serde_json::from_str(&r) {
                results.push(knw);
            }
        }
        Ok(results)
    }

    pub fn save_relation(&self, rel: &Relation) -> Result<()> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let json_str = serde_json::to_string(rel)?;
        let now = chrono::Utc::now().timestamp_millis();
        conn.execute(
            "INSERT INTO collective_relations(id, json, created_at)
             VALUES(?1, ?2, ?3)
             ON CONFLICT(id) DO UPDATE SET json=?2, created_at=?3",
            params![rel.id, json_str, now],
        )?;
        Ok(())
    }

    pub fn list_relations(&self, entity_id: Option<&str>) -> Result<Vec<Relation>> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let mut stmt = conn.prepare("SELECT json FROM collective_relations ORDER BY created_at ASC")?;
        let rows = stmt.query_map([], |r| r.get::<_, String>(0))?;
        let mut results = Vec::new();
        for r in rows.flatten() {
            if let Ok(rel) = serde_json::from_str::<Relation>(&r) {
                if let Some(target) = entity_id {
                    if rel.source_id == target || rel.target_id == target {
                        results.push(rel);
                    }
                } else {
                    results.push(rel);
                }
            }
        }
        Ok(results)
    }

    pub fn save_agent(&self, ag: &AgentRecord) -> Result<()> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let json_str = serde_json::to_string(ag)?;
        conn.execute(
            "INSERT INTO collective_agents(id, json) VALUES(?1, ?2)
             ON CONFLICT(id) DO UPDATE SET json=?2",
            params![ag.id, json_str],
        )?;
        Ok(())
    }

    pub fn list_agents(&self) -> Result<Vec<AgentRecord>> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let mut stmt = conn.prepare("SELECT json FROM collective_agents ORDER BY id ASC")?;
        let rows = stmt.query_map([], |r| r.get::<_, String>(0))?;
        let mut results = Vec::new();
        for r in rows.flatten() {
            if let Ok(ag) = serde_json::from_str(&r) {
                results.push(ag);
            }
        }
        Ok(results)
    }

    pub fn get_meta(&self, key: &str) -> Result<Option<String>> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let mut stmt = conn.prepare("SELECT value FROM collective_meta WHERE key = ?1")?;
        let mut rows = stmt.query(params![key])?;
        if let Some(r) = rows.next()? {
            Ok(Some(r.get(0)?))
        } else {
            Ok(None)
        }
    }

    pub fn set_meta(&self, key: &str, value: &str) -> Result<()> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        conn.execute(
            "INSERT INTO collective_meta(key, value) VALUES(?1, ?2)
             ON CONFLICT(key) DO UPDATE SET value=?2",
            params![key, value],
        )?;
        Ok(())
    }

    pub fn status(&self) -> Result<MemoryStatus> {
        let conn = self.db.lock().map_err(|_| anyhow!("DB lock poisoned"))?;
        let exp_count: i64 = conn.query_row("SELECT COUNT(*) FROM collective_experiences", [], |r| r.get(0))?;
        let knw_count: i64 = conn.query_row("SELECT COUNT(*) FROM collective_knowledge", [], |r| r.get(0))?;
        let rel_count: i64 = conn.query_row("SELECT COUNT(*) FROM collective_relations", [], |r| r.get(0))?;
        let ag_count: i64 = conn.query_row("SELECT COUNT(*) FROM collective_agents", [], |r| r.get(0))?;

        let pending_exp: i64 = conn.query_row("SELECT COUNT(*) FROM collective_experiences WHERE synced = 0", [], |r| r.get(0)).unwrap_or(0);
        let pending_knw: i64 = conn.query_row("SELECT COUNT(*) FROM collective_knowledge WHERE synced = 0", [], |r| r.get(0)).unwrap_or(0);

        drop(conn);

        let last_sync = self.get_meta("last_sync_at")?;
        let status_str = self.get_meta("sync_status")?.unwrap_or_else(|| "uninitialized".to_string());

        let sync_status = if (pending_exp + pending_knw) > 0 {
            SyncStatus::PendingChanges
        } else {
            match status_str.as_str() {
                "synchronized" => SyncStatus::Synchronized,
                "syncing" => SyncStatus::Syncing,
                "offline" => SyncStatus::Offline,
                "error" => SyncStatus::Error,
                _ => SyncStatus::Uninitialized,
            }
        };

        Ok(MemoryStatus {
            sync_status,
            last_sync_at: last_sync,
            experiences_count: exp_count as usize,
            knowledge_count: knw_count as usize,
            agents_count: ag_count as usize,
            relations_count: rel_count as usize,
            github_repo: "https://github.com/Anisghd-lab/CodeStation-Memory".to_string(),
            local_cache_path: self.local_repo_path.display().to_string(),
            error_message: self.get_meta("last_error")?,
        })
    }

    pub fn manifest(&self) -> Result<Manifest> {
        let status = self.status()?;
        Ok(Manifest {
            manifest_version: "1.0.0".to_string(),
            project: "CodeStation-Memory".to_string(),
            description: "CodeStation Collective Memory & Knowledge Graph".to_string(),
            schema_version: "1.0.0".to_string(),
            updated_at: status.last_sync_at.unwrap_or_else(|| chrono::Utc::now().to_rfc3339()),
            collections: vec![
                "experiences".to_string(),
                "knowledge".to_string(),
                "agents".to_string(),
                "relations".to_string(),
                "projects".to_string(),
            ],
            counts: ManifestCounts {
                experiences: status.experiences_count,
                knowledge: status.knowledge_count,
                agents: status.agents_count,
                relations: status.relations_count,
                projects: 1,
            },
            indexes: serde_json::json!({
                "search": "indexes/search-index.json",
                "tags": "indexes/tag-index.json"
            }),
            sync: serde_json::json!({
                "branch": "main",
                "remote": "origin"
            }),
        })
    }
}
