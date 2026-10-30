use super::store::MemoryStore;
use anyhow::{Result, anyhow};
use reqwest::header::{ACCEPT, AUTHORIZATION, USER_AGENT};
use std::path::Path;

const REPO_OWNER: &str = "Anisghd-lab";
const REPO_NAME: &str = "CodeStation-Memory";

pub struct GitHubAdapter {
    token: Option<String>,
}

impl GitHubAdapter {
    pub fn new() -> Self {
        let token = Self::find_github_token();
        Self { token }
    }

    fn find_github_token() -> Option<String> {
        if let Ok(t) = std::env::var("GITHUB_TOKEN") {
            if !t.trim().is_empty() {
                return Some(t.trim().to_string());
            }
        }
        if let Ok(t) = std::env::var("GH_TOKEN") {
            if !t.trim().is_empty() {
                return Some(t.trim().to_string());
            }
        }
        // Check ~/.config/gh/hosts.yml
        let hosts_path = dirs::home_dir()?.join(".config/gh/hosts.yml");
        if let Ok(content) = std::fs::read_to_string(hosts_path) {
            for line in content.lines() {
                let trimmed = line.trim();
                if let Some(token) = trimmed.strip_prefix("oauth_token: ") {
                    return Some(token.trim().to_string());
                }
            }
        }
        None
    }

    pub async fn test_connection(&self) -> Result<bool> {
        let client = reqwest::Client::builder()
            .timeout(std::time::Duration::from_secs(10))
            .build()?;

        let url = format!("https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}");
        let mut req = client.get(&url).header(USER_AGENT, "CodeStation-Host/2.28");
        if let Some(token) = &self.token {
            req = req.header(AUTHORIZATION, format!("token {token}"));
        }

        let resp = req.send().await?;
        Ok(resp.status().is_success())
    }

    pub async fn sync(&self, store: &MemoryStore, local_repo: &Path) -> Result<String> {
        store.set_meta("sync_status", "syncing")?;

        // 1. Check remote repository availability
        let client = reqwest::Client::builder()
            .timeout(std::time::Duration::from_secs(15))
            .build()?;

        let repo_url = format!("https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/commits/main");
        let mut req = client
            .get(&repo_url)
            .header(USER_AGENT, "CodeStation-Host/2.28")
            .header(ACCEPT, "application/vnd.github.v3+json");

        if let Some(token) = &self.token {
            req = req.header(AUTHORIZATION, format!("token {token}"));
        }

        match req.send().await {
            Ok(resp) if resp.status().is_success() => {
                let body: serde_json::Value = resp.json().await?;
                let sha = body["sha"].as_str().unwrap_or("unknown");
                store.set_meta("remote_commit_sha", sha)?;

                // Sync local directory into SQLite cache if local repo exists
                if local_repo.exists() {
                    let _ = store.import_from_directory(local_repo);
                }

                let now_iso = chrono::Utc::now().to_rfc3339();
                store.set_meta("last_sync_at", &now_iso)?;
                store.set_meta("sync_status", "synchronized")?;
                let _ = store.set_meta("last_error", "");

                Ok(format!("Synchronisation réussie avec GitHub (commit {})", &sha[..7.min(sha.len())]))
            }
            Ok(resp) => {
                let status_code = resp.status();
                let err_msg = format!("GitHub API a répondu avec le statut HTTP {status_code}");
                store.set_meta("sync_status", "error")?;
                store.set_meta("last_error", &err_msg)?;
                Err(anyhow!(err_msg))
            }
            Err(e) => {
                let err_msg = format!("Échec de connexion réseau GitHub: {e:#}");
                store.set_meta("sync_status", "offline")?;
                store.set_meta("last_error", &err_msg)?;
                Err(anyhow!(err_msg))
            }
        }
    }
}
