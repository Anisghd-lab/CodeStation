//! Authorized devices and their push and Live Activity tickets.

use super::{Store, logged, now_ms};
use crate::LockExt;
use anyhow::Result;
use rusqlite::{OptionalExtension, params};
use serde::{Deserialize, Serialize};
use serde_json::Value;

/// Where an authorized device came from (wire values `local` / `account`).
#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum DeviceSource {
    /// Paired with the QR code shown on this computer.
    Local,
    /// Approved here for a signed-in account; kept alive by a lease the cloud renews.
    Account,
}

/// What a device may do (wire values `control` / `screen`).
#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum Scope {
    Control,
    Screen,
}

/// One row of the authorized-device table (spec §4.3).
#[derive(Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct Device {
    pub key: String,
    pub name: String,
    pub platform: String,
    pub source: DeviceSource,
    #[serde(skip)]
    pub grant_id: Option<String>,
    pub scopes: Vec<Scope>,
    pub lease_until: Option<i64>,
    pub created_at: i64,
    pub last_seen_at: Option<i64>,
}

const DEVICE_COLS: &str = "key, name, platform, source, grant_id, scopes, lease_until, created_at, last_seen_at";

fn row_device(r: &rusqlite::Row) -> rusqlite::Result<Device> {
    let source: String = r.get(3)?;
    let scopes: String = r.get(5)?;
    Ok(Device {
        key: r.get(0)?,
        name: r.get(1)?,
        platform: r.get(2)?,
        // Unknown values grant nothing: an unreadable source is an account device (leased),
        // unreadable scopes are none.
        source: serde_json::from_value(Value::String(source)).unwrap_or(DeviceSource::Account),
        grant_id: r.get(4)?,
        scopes: serde_json::from_str(&scopes).unwrap_or_default(),
        lease_until: r.get(6)?,
        created_at: r.get(7)?,
        last_seen_at: r.get(8)?,
    })
}

impl Store {
    pub fn device(&self, key: &str) -> Option<Device> {
        let c = self.db.locked();
        let row = c.query_row(&format!("SELECT {DEVICE_COLS} FROM devices WHERE key = ?1"), [key], row_device);
        logged("device", row.optional()).flatten()
    }

    pub fn devices(&self) -> Result<Vec<Device>> {
        let c = self.db.locked();
        let mut st = c.prepare(&format!("SELECT {DEVICE_COLS} FROM devices ORDER BY created_at"))?;
        Ok(st.query_map([], row_device)?.collect::<rusqlite::Result<_>>()?)
    }

    /// Adds (or, for a key paired again, refreshes) an authorized device.
    pub fn put_device(&self, d: &Device) -> Result<()> {
        let c = self.db.locked();
        c.execute(
            "INSERT INTO devices(key, name, platform, source, grant_id, scopes, lease_until, created_at)
             VALUES(?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)
             ON CONFLICT(key) DO UPDATE SET name = ?2, platform = ?3, source = ?4, grant_id = ?5,
               scopes = ?6, lease_until = ?7",
            params![
                d.key,
                d.name,
                d.platform,
                serde_json::to_value(d.source)?.as_str(),
                d.grant_id,
                serde_json::to_string(&d.scopes)?,
                d.lease_until,
                d.created_at
            ],
        )?;
        Ok(())
    }

    /// Removes a device with its push and Live Activity tickets; false when it wasn't there.
    pub fn remove_device(&self, key: &str) -> Result<bool> {
        let mut c = self.db.locked();
        let tx = c.transaction()?;
        let n = tx.execute("DELETE FROM devices WHERE key = ?1", [key])?;
        tx.execute("DELETE FROM push_tickets WHERE device_key = ?1", [key])?;
        tx.execute("DELETE FROM activity_tickets WHERE device_key = ?1", [key])?;
        tx.commit()?;
        Ok(n > 0)
    }

    /// Extends an account device's lease (the cloud still lists its grant).
    pub fn set_lease(&self, key: &str, until: i64) -> Result<()> {
        let c = self.db.locked();
        c.execute("UPDATE devices SET lease_until = ?2 WHERE key = ?1", params![key, until])?;
        Ok(())
    }

    pub fn touch_device(&self, key: &str) -> Result<()> {
        let c = self.db.locked();
        c.execute("UPDATE devices SET last_seen_at = ?2 WHERE key = ?1", params![key, now_ms()])?;
        Ok(())
    }

}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::store::tests::temp_store;

    #[test]
    fn removing_a_device_removes_it() {
        let s = temp_store();
        let d = Device {
            key: "dk1".into(),
            name: "Phone".into(),
            platform: "ios".into(),
            source: DeviceSource::Local,
            grant_id: None,
            scopes: vec![Scope::Control],
            lease_until: None,
            created_at: 1,
            last_seen_at: None,
        };
        s.put_device(&d).unwrap();
        let got = s.device("dk1").unwrap();
        assert_eq!((got.source, got.scopes), (DeviceSource::Local, vec![Scope::Control]));

        assert!(s.remove_device("dk1").unwrap());
        assert!(!s.remove_device("dk1").unwrap());
        assert!(s.device("dk1").is_none());
    }
}
