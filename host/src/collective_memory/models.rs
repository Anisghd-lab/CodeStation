use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum ValidationStatus {
    Hypothesis,
    Attempt,
    ObservedResult,
    ValidatedSolution,
    Obsolete,
    Contradicted,
}

impl Default for ValidationStatus {
    fn default() -> Self {
        Self::Hypothesis
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Experience {
    pub id: String,
    pub title: String,
    pub project_id: String,
    pub author_agent: String,
    pub created_at: String,
    #[serde(default)]
    pub updated_at: Option<String>,
    pub problem: String,
    pub context: String,
    #[serde(default)]
    pub attempts: Vec<String>,
    #[serde(default)]
    pub observed_results: Vec<String>,
    #[serde(default)]
    pub errors_encountered: Vec<String>,
    #[serde(default)]
    pub solution_proposed: Option<String>,
    pub validation_status: ValidationStatus,
    #[serde(default)]
    pub tags: Vec<String>,
    #[serde(default)]
    pub relations: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Knowledge {
    pub id: String,
    #[serde(rename = "type")]
    pub kind: String, // solution, architecture, procedure, tool_config, best_practice, known_issue
    pub title: String,
    pub content: String,
    pub project_id: String,
    pub source: String,
    pub author: String,
    pub created_at: String,
    #[serde(default)]
    pub updated_at: Option<String>,
    #[serde(default)]
    pub tags: Vec<String>,
    #[serde(default = "default_confidence")]
    pub confidence: f64,
    pub validation_status: ValidationStatus,
    #[serde(default)]
    pub relations: Vec<String>,
    #[serde(default = "default_version")]
    pub version: u32,
}

fn default_confidence() -> f64 {
    0.8
}

fn default_version() -> u32 {
    1
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AgentRecord {
    pub id: String,
    pub name: String,
    #[serde(rename = "type")]
    pub kind: String,
    pub cli_binary: String,
    pub protocol: String,
    #[serde(default)]
    pub capabilities: Vec<String>,
    #[serde(default)]
    pub contributions_count: u32,
    #[serde(default)]
    pub last_active: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Relation {
    pub id: String,
    pub source_id: String,
    pub target_id: String,
    pub relation_type: String,
    #[serde(default)]
    pub description: Option<String>,
    pub created_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Manifest {
    pub manifest_version: String,
    pub project: String,
    pub description: String,
    pub schema_version: String,
    pub updated_at: String,
    #[serde(default)]
    pub collections: Vec<String>,
    pub counts: ManifestCounts,
    #[serde(default)]
    pub indexes: serde_json::Value,
    #[serde(default)]
    pub sync: serde_json::Value,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct ManifestCounts {
    pub experiences: usize,
    pub knowledge: usize,
    pub agents: usize,
    pub relations: usize,
    #[serde(default)]
    pub projects: usize,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum SyncStatus {
    Synchronized,
    Syncing,
    PendingChanges,
    Offline,
    Error,
    Uninitialized,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MemoryStatus {
    pub sync_status: SyncStatus,
    pub last_sync_at: Option<String>,
    pub experiences_count: usize,
    pub knowledge_count: usize,
    pub agents_count: usize,
    pub relations_count: usize,
    pub github_repo: String,
    pub local_cache_path: String,
    pub error_message: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SearchResult {
    pub id: String,
    pub item_type: String, // "experience" or "knowledge"
    pub title: String,
    pub snippet: String,
    pub validation_status: ValidationStatus,
    pub tags: Vec<String>,
    pub author: String,
    pub score: f32,
}
