import os
import sqlite3
from pathlib import Path
from typing import Iterator, Optional

DB_PATH = Path(os.environ.get("BLOCKBASE_DB_PATH", Path(__file__).parent / "blockbase.sqlite")).resolve()

SCHEMA = """
PRAGMA journal_mode=WAL;

CREATE TABLE IF NOT EXISTS repos (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  default_branch TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS commits (
  id TEXT PRIMARY KEY,
  repo_id TEXT NOT NULL,
  message TEXT NOT NULL,
  author TEXT NOT NULL,
  timestamp DATETIME NOT NULL,
  FOREIGN KEY (repo_id) REFERENCES repos(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS changes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  commit_id TEXT NOT NULL,
  pos_x INTEGER NOT NULL,
  pos_y INTEGER NOT NULL,
  pos_z INTEGER NOT NULL,
  old_state TEXT,
  new_state TEXT,
  FOREIGN KEY (commit_id) REFERENCES commits(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_commits_repo_time ON commits (repo_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_changes_commit ON changes (commit_id);
"""


def get_conn() -> sqlite3.Connection:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(DB_PATH))
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    conn = get_conn()
    try:
        conn.executescript(SCHEMA)
        _migrate_add_readme_column(conn)
        _migrate_add_changes_json_column(conn)
        conn.commit()
    finally:
        conn.close()

def _migrate_add_readme_column(conn: sqlite3.Connection) -> None:
    # Add repos.readme TEXT if not present
    cur = conn.execute("PRAGMA table_info(repos)")
    cols = [row["name"] for row in cur.fetchall()]
    if "readme" not in cols:
        conn.execute("ALTER TABLE repos ADD COLUMN readme TEXT")
        conn.commit()

def _migrate_add_changes_json_column(conn: sqlite3.Connection) -> None:
    # Add commits.changes_json TEXT if not present
    cur = conn.execute("PRAGMA table_info(commits)")
    cols = [row["name"] for row in cur.fetchall()]
    if "changes_json" not in cols:
        conn.execute("ALTER TABLE commits ADD COLUMN changes_json TEXT")
        conn.commit()

def with_conn() -> Iterator[sqlite3.Connection]:
    conn = get_conn()
    try:
        yield conn
    finally:
        conn.close()


