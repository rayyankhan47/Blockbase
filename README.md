# Blockbase

**Version Control for Minecraft Builds - Like Git/GitHub for Your Creations**

Blockbase brings the power of modern software development tools to Minecraft. Whether you're building a redstone CPU, a massive castle, an automated farm, or any complex structure, Blockbase helps you track changes, collaborate, and never lose your work.

## What is Blockbase?

Think **GitHub + Cursor for Minecraft**. Blockbase is a complete version control system with AI assistance, designed for all types of Minecraft builds.

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

### AI Assistant
- Select blocks with wooden axe for context
- Ask AI questions about your build
- Get suggestions and explanations
- Like Copilot/Cursor, but for Minecraft

### Visual Diffing (The Killer Feature)
- See a duplicate of your build 30 blocks in the sky
- Color-coded changes:
  - 🟢 Green = Added blocks
  - 🔴 Red = Removed blocks
  - 🟡 Yellow = Modified blocks
- Perfect for understanding what changed in complex builds

## Architecture

- **Minecraft Mod** (Fabric 1.18.2) - In-game version control
- **Backend API** (Python + FastAPI) - Version control logic & AI integration
- **Web Dashboard** (Next.js) - Repository browser (future)

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
# Initialize repository
/blockbase init

# Stage changes
/blockbase stage

# Commit changes
/blockbase commit "Added new wing to castle"

# View commit history
/blockbase log

# See what changed (visual diff)
/blockbase diff HEAD~1 HEAD

# Rollback to previous commit
/blockbase reset --hard <commitId>

# Push to remote
/blockbase push

# Ask AI about your build
/blockbase ai "explain why this doesn't work"
```

## Demo

Blockbase is perfect for demonstrating version control with complex builds. Our demo uses a redstone CPU/ALU to show:
- How visual diffing helps understand complex changes
- How AI can assist with debugging
- How version control prevents lost work

But Blockbase works for **any Minecraft build** - from redstone circuits to massive castles!

## Tech Stack

- **Mod**: Fabric (Java)
- **Backend**: Python + FastAPI
- **AI**: Gemini API
- **Database**: SQLite

## License

MIT
