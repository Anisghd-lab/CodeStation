use super::models::Relation;
use super::store::MemoryStore;
use anyhow::Result;
use serde::Serialize;

#[derive(Debug, Clone, Serialize)]
pub struct EntityRelationGraph {
    pub entity_id: String,
    pub outgoing: Vec<Relation>,
    pub incoming: Vec<Relation>,
}

pub struct RelationshipEngine;

impl RelationshipEngine {
    pub fn get_relations_for_entity(store: &MemoryStore, entity_id: &str) -> Result<EntityRelationGraph> {
        let all_relations = store.list_relations(None)?;
        let mut outgoing = Vec::new();
        let mut incoming = Vec::new();

        for rel in all_relations {
            if rel.source_id == entity_id {
                outgoing.push(rel.clone());
            }
            if rel.target_id == entity_id {
                incoming.push(rel);
            }
        }

        Ok(EntityRelationGraph {
            entity_id: entity_id.to_string(),
            outgoing,
            incoming,
        })
    }
}
