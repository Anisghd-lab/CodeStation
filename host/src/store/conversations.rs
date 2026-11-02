//! Conversation and session management per bot.

use super::entries::{ENTRY_COLS, row_entry};
use super::{Conversation, Entry, Store, now_ms};
use crate::LockExt;
use anyhow::Result;
use rusqlite::{OptionalExtension, params};

const CONVERSATION_COLS: &str =
    "id, bot_id, title, cli_id, runner_id, cli_session_id, status, created_at, updated_at";

impl Store {
    pub fn save_conversation(&self, conv: &Conversation) -> Result<()> {
        let c = self.db.locked();
        c.execute(
            "INSERT INTO conversations(id, bot_id, title, cli_id, runner_id, cli_session_id, status, created_at, updated_at)
             VALUES(?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)
             ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                cli_id = excluded.cli_id,
                runner_id = excluded.runner_id,
                cli_session_id = excluded.cli_session_id,
                status = excluded.status,
                updated_at = excluded.updated_at",
            params![
                conv.id,
                conv.bot_id,
                conv.title,
                conv.cli_id,
                conv.runner_id,
                conv.cli_session_id,
                conv.status,
                conv.created_at,
                conv.updated_at
            ],
        )?;
        Ok(())
    }

    pub fn conversations_for_bot(&self, bot_id: &str) -> Result<Vec<Conversation>> {
        let c = self.db.locked();
        let mut st = c.prepare(&format!(
            "SELECT {CONVERSATION_COLS} FROM conversations WHERE bot_id = ?1 ORDER BY updated_at DESC"
        ))?;
        let rows = st
            .query_map(params![bot_id], |r| {
                Ok(Conversation {
                    id: r.get(0)?,
                    bot_id: r.get(1)?,
                    title: r.get(2)?,
                    cli_id: r.get(3)?,
                    runner_id: r.get(4)?,
                    cli_session_id: r.get(5)?,
                    status: r.get(6)?,
                    created_at: r.get(7)?,
                    updated_at: r.get(8)?,
                })
            })?
            .collect::<rusqlite::Result<Vec<_>>>()?;
        Ok(rows)
    }

    pub fn conversation(&self, id: &str) -> Result<Option<Conversation>> {
        let c = self.db.locked();
        let mut st = c.prepare(&format!(
            "SELECT {CONVERSATION_COLS} FROM conversations WHERE id = ?1"
        ))?;
        let row = st
            .query_row(params![id], |r| {
                Ok(Conversation {
                    id: r.get(0)?,
                    bot_id: r.get(1)?,
                    title: r.get(2)?,
                    cli_id: r.get(3)?,
                    runner_id: r.get(4)?,
                    cli_session_id: r.get(5)?,
                    status: r.get(6)?,
                    created_at: r.get(7)?,
                    updated_at: r.get(8)?,
                })
            })
            .optional()?;
        Ok(row)
    }

    pub fn delete_conversation(&self, id: &str) -> Result<()> {
        let c = self.db.locked();
        c.execute("DELETE FROM conversations WHERE id = ?1", params![id])?;
        Ok(())
    }

    pub fn rename_conversation(&self, id: &str, title: &str) -> Result<()> {
        let c = self.db.locked();
        c.execute(
            "UPDATE conversations SET title = ?1, updated_at = ?2 WHERE id = ?3",
            params![title, now_ms(), id],
        )?;
        Ok(())
    }

    pub fn history_conversation(
        &self,
        bot_id: &str,
        conv_id: &str,
        before_seq: i64,
        limit: i64,
    ) -> Result<Vec<Entry>> {
        let c = self.db.locked();
        let mut st = c.prepare(&format!(
            "SELECT {ENTRY_COLS} FROM entries WHERE bot_id = ?1 AND json_extract(data, '$.conversationId') = ?2 AND seq < ?3
             ORDER BY seq DESC LIMIT ?4"
        ))?;
        let mut rows: Vec<Entry> = st
            .query_map(params![bot_id, conv_id, before_seq, limit], row_entry)?
            .collect::<rusqlite::Result<_>>()?;
        rows.reverse();
        Ok(rows)
    }
}
