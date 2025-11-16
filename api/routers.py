from typing import List, Optional
from fastapi import APIRouter, HTTPException
# Support running as package (api.routers) or module (routers)
try:
	from . import db  # type: ignore
	from .models import RepoCreate, RepoOut, CommitCreate, CommitOut, CommitWithChangesOut, ChangeIn, RepoReadmeIn, RepoReadmeOut  # type: ignore
except ImportError:
	import db  # type: ignore
	from models import RepoCreate, RepoOut, CommitCreate, CommitOut, CommitWithChangesOut, ChangeIn, RepoReadmeIn, RepoReadmeOut  # type: ignore

router = APIRouter(prefix="/api")


@router.post("/repos", response_model=RepoOut)
def create_repo(body: RepoCreate):
    with db.get_conn() as conn:
        cur = conn.execute("SELECT id FROM repos WHERE id = ?", (body.id,))
        if cur.fetchone() is not None:
            raise HTTPException(status_code=409, detail="Repository already exists")
        conn.execute(
            "INSERT INTO repos(id, name, default_branch) VALUES (?,?,?)",
            (body.id, body.name, body.default_branch),
        )
        conn.commit()
        row = conn.execute("SELECT id, name, default_branch, created_at FROM repos WHERE id = ?", (body.id,)).fetchone()
        return RepoOut(**dict(row))


@router.get("/repos/{repo_id}", response_model=RepoOut)
def get_repo(repo_id: str):
    with db.get_conn() as conn:
        row = conn.execute(
            "SELECT id, name, default_branch, created_at FROM repos WHERE id = ?",
            (repo_id,),
        ).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Repository not found")
        return RepoOut(**dict(row))


@router.get("/repos", response_model=List[RepoOut])
def list_repos():
    with db.get_conn() as conn:
        rows = conn.execute(
            "SELECT id, name, default_branch, created_at FROM repos ORDER BY created_at DESC"
        ).fetchall()
        return [RepoOut(**dict(r)) for r in rows]

@router.get("/repos/{repo_id}/readme", response_model=RepoReadmeOut)
def get_readme(repo_id: str):
    with db.get_conn() as conn:
        row = conn.execute("SELECT readme FROM repos WHERE id = ?", (repo_id,)).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Repository not found")
        content = row["readme"] or ""
        return RepoReadmeOut(content=content)

@router.put("/repos/{repo_id}/readme", response_model=RepoReadmeOut)
def put_readme(repo_id: str, body: RepoReadmeIn):
    with db.get_conn() as conn:
        repo = conn.execute("SELECT id FROM repos WHERE id = ?", (repo_id,)).fetchone()
        if repo is None:
            raise HTTPException(status_code=404, detail="Repository not found")
        conn.execute("UPDATE repos SET readme = ? WHERE id = ?", (body.content, repo_id))
        conn.commit()
        return RepoReadmeOut(content=body.content)


@router.post("/repos/{repo_id}/commits")
def create_commit(repo_id: str, body: CommitCreate):
    with db.get_conn() as conn:
        repo = conn.execute("SELECT id FROM repos WHERE id = ?", (repo_id,)).fetchone()
        if repo is None:
            raise HTTPException(status_code=404, detail="Repository not found")
        exists = conn.execute("SELECT id FROM commits WHERE id = ?", (body.id,)).fetchone()
        if exists is not None:
            # Idempotent: accept existing
            return {"ok": True, "id": body.id}
        conn.execute(
            "INSERT INTO commits(id, repo_id, message, author, timestamp) VALUES (?,?,?,?,?)",
            (body.id, repo_id, body.message, body.author, body.timestamp),
        )
        if body.changes:
            conn.executemany(
                "INSERT INTO changes(commit_id, pos_x, pos_y, pos_z, old_state, new_state) VALUES (?,?,?,?,?,?)",
                [
                    (body.id, c.pos_x, c.pos_y, c.pos_z, c.old_state, c.new_state)
                    for c in body.changes
                ],
            )
        conn.commit()
        return {"ok": True, "id": body.id}


@router.get("/repos/{repo_id}/commits", response_model=List[CommitOut])
def list_commits(repo_id: str):
    with db.get_conn() as conn:
        repo = conn.execute("SELECT id FROM repos WHERE id = ?", (repo_id,)).fetchone()
        if repo is None:
            raise HTTPException(status_code=404, detail="Repository not found")
        rows = conn.execute(
            "SELECT id, repo_id, message, author, timestamp FROM commits WHERE repo_id = ? ORDER BY timestamp DESC",
            (repo_id,),
        ).fetchall()
        return [CommitOut(**dict(r)) for r in rows]


@router.get("/repos/{repo_id}/commits/{commit_id}", response_model=CommitWithChangesOut)
def get_commit(repo_id: str, commit_id: str):
    with db.get_conn() as conn:
        commit = conn.execute(
            "SELECT id, repo_id, message, author, timestamp FROM commits WHERE id = ? AND repo_id = ?",
            (commit_id, repo_id),
        ).fetchone()
        if commit is None:
            raise HTTPException(status_code=404, detail="Commit not found")
        rows = conn.execute(
            "SELECT pos_x, pos_y, pos_z, old_state, new_state FROM changes WHERE commit_id = ?",
            (commit_id,),
        ).fetchall()
        return CommitWithChangesOut(
            **dict(commit),
            changes=[ChangeIn(**dict(r)) for r in rows],
        )


