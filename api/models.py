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

class RepoReadmeOut(BaseModel):
    content: str

class RepoReadmeIn(BaseModel):
    content: str


class ChangeIn(BaseModel):
    # Placeholder for future structured changes; for now we accept arbitrary objects
    # coming from the mod and don't persist them in this API.
    pos_x: Optional[int] = None
    pos_y: Optional[int] = None
    pos_z: Optional[int] = None
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


