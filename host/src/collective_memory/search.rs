use super::models::{Experience, Knowledge, SearchResult};
use super::store::MemoryStore;
use anyhow::Result;

pub struct SearchEngine;

impl SearchEngine {
    pub fn search(
        store: &MemoryStore,
        query: &str,
        filter_project: Option<&str>,
        filter_agent: Option<&str>,
        filter_type: Option<&str>,
        filter_status: Option<&str>,
        limit: usize,
    ) -> Result<Vec<SearchResult>> {
        let query_terms: Vec<String> = query
            .to_lowercase()
            .split_whitespace()
            .map(|s| s.trim_matches(|c: char| !c.is_alphanumeric()).to_string())
            .filter(|s| !s.is_empty())
            .collect();

        let mut results = Vec::new();

        // 1. Search in Knowledge
        let all_knw = store.list_knowledge(500, 0)?;
        for knw in all_knw {
            if let Some(prj) = filter_project {
                if knw.project_id != prj {
                    continue;
                }
            }
            if let Some(ag) = filter_agent {
                if knw.author != ag {
                    continue;
                }
            }
            if let Some(t) = filter_type {
                if t != "knowledge" && knw.kind != t {
                    continue;
                }
            }
            if let Some(st) = filter_status {
                let status_str = serde_json::to_string(&knw.validation_status)?
                    .trim_matches('"')
                    .to_string();
                if status_str != st {
                    continue;
                }
            }

            let score = calculate_knowledge_score(&knw, &query_terms);
            if query_terms.is_empty() || score > 0.0 {
                let snippet = if knw.content.len() > 160 {
                    format!("{}...", &knw.content[..160])
                } else {
                    knw.content.clone()
                };

                results.push(SearchResult {
                    id: knw.id,
                    item_type: "knowledge".to_string(),
                    title: knw.title,
                    snippet,
                    validation_status: knw.validation_status,
                    tags: knw.tags,
                    author: knw.author,
                    score,
                });
            }
        }

        // 2. Search in Experiences
        let all_exp = store.list_experiences(500, 0)?;
        for exp in all_exp {
            if let Some(prj) = filter_project {
                if exp.project_id != prj {
                    continue;
                }
            }
            if let Some(ag) = filter_agent {
                if exp.author_agent != ag {
                    continue;
                }
            }
            if let Some(t) = filter_type {
                if t != "experience" {
                    continue;
                }
            }
            if let Some(st) = filter_status {
                let status_str = serde_json::to_string(&exp.validation_status)?
                    .trim_matches('"')
                    .to_string();
                if status_str != st {
                    continue;
                }
            }

            let score = calculate_experience_score(&exp, &query_terms);
            if query_terms.is_empty() || score > 0.0 {
                let snippet = if exp.problem.len() > 160 {
                    format!("{}...", &exp.problem[..160])
                } else {
                    exp.problem.clone()
                };

                results.push(SearchResult {
                    id: exp.id,
                    item_type: "experience".to_string(),
                    title: exp.title,
                    snippet,
                    validation_status: exp.validation_status,
                    tags: exp.tags,
                    author: exp.author_agent,
                    score,
                });
            }
        }

        // Sort by score descending
        results.sort_by(|a, b| b.score.partial_cmp(&a.score).unwrap_or(std::cmp::Ordering::Equal));
        if results.len() > limit {
            results.truncate(limit);
        }

        Ok(results)
    }
}

fn calculate_knowledge_score(knw: &Knowledge, terms: &[String]) -> f32 {
    if terms.is_empty() {
        return 1.0;
    }
    let title_lower = knw.title.to_lowercase();
    let content_lower = knw.content.to_lowercase();
    let mut score = 0.0f32;

    for term in terms {
        if title_lower.contains(term) {
            score += 10.0;
        }
        if knw.tags.iter().any(|t| t.to_lowercase().contains(term)) {
            score += 5.0;
        }
        if content_lower.contains(term) {
            score += 2.0;
        }
    }
    score
}

fn calculate_experience_score(exp: &Experience, terms: &[String]) -> f32 {
    if terms.is_empty() {
        return 1.0;
    }
    let title_lower = exp.title.to_lowercase();
    let problem_lower = exp.problem.to_lowercase();
    let context_lower = exp.context.to_lowercase();
    let mut score = 0.0f32;

    for term in terms {
        if title_lower.contains(term) {
            score += 10.0;
        }
        if exp.tags.iter().any(|t| t.to_lowercase().contains(term)) {
            score += 5.0;
        }
        if problem_lower.contains(term) {
            score += 3.0;
        }
        if context_lower.contains(term) {
            score += 1.0;
        }
    }
    score
}
