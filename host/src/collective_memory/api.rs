use super::github::GitHubAdapter;
use super::models::{Experience, Knowledge, ValidationStatus};
use super::relations::RelationshipEngine;
use super::search::SearchEngine;
use super::store::MemoryStore;
use anyhow::{Result, anyhow};
use serde_json::{Value, json};
use std::path::PathBuf;
use std::sync::Arc;

pub struct MemoryEngine {
    pub store: Arc<MemoryStore>,
    pub github: GitHubAdapter,
    pub local_repo: PathBuf,
}

impl MemoryEngine {
    pub fn new(db_path: &std::path::Path, local_repo: &std::path::Path) -> Result<Self> {
        let store = Arc::new(MemoryStore::new(db_path, local_repo)?);
        let github = GitHubAdapter::new();
        Ok(Self {
            store,
            github,
            local_repo: local_repo.to_path_buf(),
        })
    }

    pub async fn handle_rpc(&self, method: &str, body: &Value) -> Result<Option<Value>> {
        match method {
            "memoryStatus" => {
                let status = self.store.status()?;
                Ok(Some(json!(status)))
            }
            "memoryManifest" => {
                let manifest = self.store.manifest()?;
                Ok(Some(json!(manifest)))
            }
            "memoryExperiences" => {
                let limit = body["limit"].as_u64().unwrap_or(50) as usize;
                let offset = body["offset"].as_u64().unwrap_or(0) as usize;
                let items = self.store.list_experiences(limit, offset)?;
                Ok(Some(json!({ "experiences": items, "count": items.len() })))
            }
            "memoryExperienceDetail" => {
                let id = body["id"].as_str().ok_or_else(|| anyhow!("id required"))?;
                let exp = self.store.get_experience(id)?;
                Ok(Some(json!({ "experience": exp })))
            }
            "memoryKnowledge" => {
                let limit = body["limit"].as_u64().unwrap_or(50) as usize;
                let offset = body["offset"].as_u64().unwrap_or(0) as usize;
                let items = self.store.list_knowledge(limit, offset)?;
                Ok(Some(json!({ "knowledge": items, "count": items.len() })))
            }
            "memoryKnowledgeDetail" => {
                let id = body["id"].as_str().ok_or_else(|| anyhow!("id required"))?;
                let knw = self.store.get_knowledge(id)?;
                Ok(Some(json!({ "knowledge": knw })))
            }
            "memorySearch" => {
                let query = body["query"].as_str().unwrap_or_default();
                let project = body["project"].as_str();
                let agent = body["agent"].as_str();
                let item_type = body["type"].as_str();
                let status = body["status"].as_str();
                let limit = body["limit"].as_u64().unwrap_or(30) as usize;

                let results = SearchEngine::search(
                    &self.store,
                    query,
                    project,
                    agent,
                    item_type,
                    status,
                    limit,
                )?;
                Ok(Some(json!({ "results": results, "count": results.len() })))
            }
            "memoryRelations" => {
                let entity_id = body["id"].as_str().ok_or_else(|| anyhow!("id required"))?;
                let graph = RelationshipEngine::get_relations_for_entity(&self.store, entity_id)?;
                Ok(Some(json!(graph)))
            }
            "memoryCreateExperience" => {
                let mut exp: Experience = serde_json::from_value(body.clone())?;
                if exp.id.is_empty() {
                    exp.id = format!("exp-{}", uuid::Uuid::new_v4());
                }
                if exp.created_at.is_empty() {
                    exp.created_at = chrono::Utc::now().to_rfc3339();
                }
                self.store.save_experience(&exp, false)?;
                self.store.set_meta("sync_status", "pending_changes")?;
                Ok(Some(json!({ "success": true, "id": exp.id })))
            }
            "memoryCreateKnowledge" => {
                let mut knw: Knowledge = serde_json::from_value(body.clone())?;
                if knw.id.is_empty() {
                    knw.id = format!("knw-{}", uuid::Uuid::new_v4());
                }
                if knw.created_at.is_empty() {
                    knw.created_at = chrono::Utc::now().to_rfc3339();
                }
                self.store.save_knowledge(&knw, false)?;
                self.store.set_meta("sync_status", "pending_changes")?;
                Ok(Some(json!({ "success": true, "id": knw.id })))
            }
            "memoryUpdateKnowledge" => {
                let id = body["id"].as_str().ok_or_else(|| anyhow!("id required"))?;
                let mut existing = self.store.get_knowledge(id)?.ok_or_else(|| anyhow!("not found"))?;

                if let Some(title) = body["title"].as_str() {
                    existing.title = title.to_string();
                }
                if let Some(content) = body["content"].as_str() {
                    existing.content = content.to_string();
                }
                if let Some(status_str) = body["validation_status"].as_str() {
                    if let Ok(st) = serde_json::from_value::<ValidationStatus>(json!(status_str)) {
                        existing.validation_status = st;
                    }
                }
                existing.version += 1;
                existing.updated_at = Some(chrono::Utc::now().to_rfc3339());

                self.store.save_knowledge(&existing, false)?;
                self.store.set_meta("sync_status", "pending_changes")?;
                Ok(Some(json!({ "success": true, "knowledge": existing })))
            }
            "memorySync" => {
                let msg = self.github.sync(&self.store, &self.local_repo).await?;
                let status = self.store.status()?;
                Ok(Some(json!({ "success": true, "message": msg, "status": status })))
            }
            // Phase 8: Agent Collective Memory Interface
            "memoryAgentQuery" => {
                let agent_id = body["agent_id"].as_str().unwrap_or("unknown");
                let _task_context = body["context"].as_str().unwrap_or_default();
                let keywords = body["keywords"].as_str().unwrap_or_default();

                let results = SearchEngine::search(
                    &self.store,
                    keywords,
                    None,
                    None,
                    None,
                    Some("validated_solution"),
                    5,
                )?;

                Ok(Some(json!({
                    "agent": agent_id,
                    "solutions_found": results,
                    "guidance": "Privilégier les solutions au statut 'validated_solution'. Enregistrer toute anomalie via memoryCreateExperience."
                })))
            }
            _ => Ok(None),
        }
    }
}
