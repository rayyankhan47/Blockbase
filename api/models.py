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
    # Shape mirrors BlockChange.toJsonString in the mod
    x: Optional[int] = None
    y: Optional[int] = None
    z: Optional[int] = None
    oldStateId: Optional[str] = None
    newStateId: Optional[str] = None
    type: Optional[str] = None
    # We ignore props for now but keep them loosely typed
    oldProps: Optional[dict] = None
    newProps: Optional[dict] = None


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


