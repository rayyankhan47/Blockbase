from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import os

# Support running as package (api.main) or module (main)
try:
	from .db import init_db  # type: ignore
	from .routers import router  # type: ignore
except ImportError:
	from db import init_db  # type: ignore
	from routers import router  # type: ignore

init_db()

app = FastAPI(title="Blockbase API", version="0.1.0")

origins = os.environ.get("BLOCKBASE_CORS", "*")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"] if origins == "*" else [o.strip() for o in origins.split(",")],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)

@app.get("/")
def root():
    return {"ok": True, "service": "blockbase-api"}


