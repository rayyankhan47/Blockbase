from typing import List, Optional
from pydantic import BaseModel, Field


class RepoCreate(BaseModel):
    id: str
    name: str
    default_branch: str = "main"


class RepoOut(BaseModel):
    id: str
    name: str
    default_branch: str
    created_at: str


class ChangeIn(BaseModel):
    pos_x: int
    pos_y: int
    pos_z: int
    old_state: Optional[str] = None
    new_state: Optional[str] = None


class CommitCreate(BaseModel):
    id: str
    message: str
    author: str
    timestamp: str  # ISO string; we store as text
    changes: List[ChangeIn] = Field(default_factory=list)


class CommitOut(BaseModel):
    id: str
    repo_id: str
    message: str
    author: str
    timestamp: str


class CommitWithChangesOut(CommitOut):
    changes: List[ChangeIn]


