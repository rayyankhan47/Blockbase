# Blockbase

**Version Control for Minecraft Builds – Git for Your Worlds**

Blockbase brings the power of modern software development tools to Minecraft. Whether you're building a redstone CPU, a massive castle, an automated farm, or any complex structure, Blockbase helps you track changes, collaborate, and never lose your work.

## What is Blockbase?

Think **Git meets Minecraft**. Blockbase is a complete version control system for all types of Minecraft builds, with Git‑like commands available directly inside the game.

### Use Cases

- ⚡ **Redstone Engineering**: Version control for complex circuits, CPUs, ALUs
- 🏗️ **Large Builds**: Track changes to massive bases, cities, castles
- 🌾 **Farms & Automation**: Manage improvements to automated systems
- 🎨 **Creative Projects**: Collaborate on artistic builds
- 🏛️ **Architecture**: Version control for complex structural projects

## Core Features

### Version Control
- **Staging**: Select which changes to commit
- **Committing**: Save snapshots of your builds
- **Pushing**: Upload builds to remote repositories
- **Resetting**: Rollback to previous commits (undo mistakes!)
- **Visual Diffing**: See exactly what changed with 3D visualization

### Visual Diffing (The Killer Feature)
- Toggle an in‑place diff view inside your current world
- Color-coded changes:
  - 🟢 Green = Added blocks
  - 🔴 Red = Removed blocks
  - 🟡 Yellow = Modified blocks (optional)
- Perfect for understanding what changed in complex builds without leaving Minecraft

## Architecture

- **Minecraft Mod** (Fabric 1.18.2) – In-game tracking, commits, diffing, and Git‑like commands
- **Backend API** (Python + FastAPI + SQLite) – Remote repositories and commit storage
- **Web Dashboard** (Next.js) – GitHub‑style interface for browsing repos, commits, and READMEs

## Getting Started

### Prerequisites
- Java 17
- Minecraft 1.18.2
- Fabric mod loader

### Installation
1. Download the Blockbase mod JAR
2. Place in `~/.minecraft/mods/`
3. Launch Minecraft with Fabric

### Basic Usage

```bash
# Initialize repository for the current world
/bb init

# Stage tracked changes
/bb add .

# Commit changes
/bb commit "Added new wing to castle"

# View commit history
/bb log

# Enter visual diff mode (compare unstaged changes to latest commit)
/bb diff

# Rollback to previous commit (destructive)
/bb reset --hard <commitId>

# Configure remote (from web dashboard)
/bb remote add origin http://localhost:8000/api/repos/<repoId>

# Push commits to remote
/bb push
```

## Demo

Blockbase is perfect for demonstrating version control with complex builds. A redstone CPU/ALU is a great showcase:
- How visual diffing helps understand complex changes
- How version control prevents lost work

But Blockbase works for **any Minecraft build** – from redstone circuits to massive castles!

## Tech Stack

- **Mod**: Fabric (Java)
- **Backend**: Python + FastAPI
- **Database**: SQLite

## License

MIT
