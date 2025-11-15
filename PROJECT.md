# Blockbase - Project Architecture & Deliverables

## Project Overview

Blockbase is a version control system for Minecraft builds with AI assistance. Think GitHub + Cursor for Minecraft.

## Priority Order

1. **Minecraft Mod** (Primary Focus)
   - Version control (staging, committing, pushing, branches, pull requests)
   - Visual diffing (red/green overlays)
   - AI agent integration
   - Block context tagging (wooden axe selection)

2. **Web Dashboard** (Secondary)
   - Repository browser
   - Pull request interface
   - Build visualization

## Architecture

### System Components

```
┌─────────────────┐
│  Minecraft Mod  │ (Fabric 1.18.2)
│  - Git commands │
│  - Visual diff  │
│  - AI chat      │
└────────┬────────┘
         │ HTTP/WebSocket
         │
┌────────▼────────┐
│  Backend API    │ (Python + FastAPI)
│  - Git logic    │
│  - Block storage│
│  - AI service   │
└────────┬────────┘
         │
┌────────▼────────┐
│   Database      │ (SQLite)
│   - Repos       │
│   - Commits     │
│   - Blocks      │
└─────────────────┘
         │
┌────────▼────────┐
│  Web Dashboard  │ (Next.js)
│  - Repo browser │
│  - PR interface │
└─────────────────┘
```

### Data Flow

#### Version Control Flow
1. Player makes changes in Minecraft
2. Mod tracks block changes (position + block type)
3. Player stages changes via command: `/blockbase stage`
4. Player commits: `/blockbase commit "message"`
5. Mod sends block data to backend API
6. Backend stores commit in database
7. Player pushes: `/blockbase push` → sends to remote

#### Visual Diffing Flow
1. Player runs: `/blockbase diff <commit1> <commit2>`
2. Mod requests diff from backend
3. Backend calculates added/removed blocks
4. Mod receives diff data
5. Mod uses Mixin to render overlays:
   - Green overlay for added blocks
   - Red overlay for removed blocks

#### AI Agent Flow
1. Player selects blocks with wooden axe (right-click)
2. Mod stores selected block positions
3. Player types: `/blockbase ai "explain why this doesn't work"`
4. Mod sends selected blocks + question to backend
5. Backend queries Gemini API with:
   - Block data (positions, types, states)
   - Player question
   - Context about redstone mechanics
6. Backend returns AI response
7. Mod displays response in chat

### Data Structures

#### Block Representation
```typescript
{
  x: number,
  y: number,
  z: number,
  blockType: string,  // e.g., "minecraft:redstone_wire"
  blockState: object  // NBT data, facing direction, etc.
}
```

#### Commit Structure
```typescript
{
  id: string,           // SHA-1 hash
  message: string,
  author: string,
  timestamp: number,
  changes: BlockChange[],
  parentCommitId?: string
}
```

#### Repository Structure
```typescript
{
  id: string,
  name: string,
  owner: string,
  defaultBranch: string,
  branches: Branch[]
}
```

## Deliverables & Implementation Steps

### Phase 1: Core Mod Infrastructure (Foundation)

#### 1.1 Mod Setup
- [ ] Create Fabric mod project structure
- [ ] Configure build.gradle for Minecraft 1.18.2
- [ ] Set up basic mod class and initialization
- [ ] Test mod loads in game

**Deliverable**: Mod that loads without errors

#### 1.2 Block Tracking System
- [ ] Implement block change detection (BlockPlaceEvent, BlockBreakEvent)
- [ ] Create data structure to store block changes
- [ ] Track block positions, types, and states
- [ ] Handle chunk loading/unloading

**Deliverable**: Mod that can detect and store block changes

#### 1.3 Basic Command System
- [ ] Set up Fabric command registration
- [ ] Create `/blockbase` base command
- [ ] Implement command structure: `/blockbase <subcommand>`
- [ ] Add help command: `/blockbase help`

**Deliverable**: Working command system with help menu

---

### Phase 2: Version Control Core (Git-like Features)

#### 2.1 Staging System
- [ ] Implement `/blockbase stage` command
- [ ] Allow staging specific regions or all changes
- [ ] Store staged changes separately from unstaged
- [ ] Show status: `/blockbase status` (staged vs unstaged)

**Deliverable**: Can stage block changes

#### 2.2 Commit System
- [ ] Implement `/blockbase commit <message>` command
- [ ] Create commit data structure (id, message, timestamp, changes)
- [ ] Generate commit hash (SHA-1)
- [ ] Store commits locally (JSON files or database)
- [ ] Link commits in a chain (parent commits)

**Deliverable**: Can create commits with messages

#### 2.3 Repository Initialization
- [ ] Implement `/blockbase init` command
- [ ] Create repository structure in world folder
- [ ] Set up default branch ("main")
- [ ] Create initial commit

**Deliverable**: Can initialize a repository in a world

#### 2.4 Branch System
- [ ] Implement `/blockbase branch <name>` (create branch)
- [ ] Implement `/blockbase checkout <branch>` (switch branch)
- [ ] Store branch pointers to commits
- [ ] Track current branch
- [ ] List branches: `/blockbase branch --list`

**Deliverable**: Can create and switch between branches

---

### Phase 3: Backend API & Communication

#### 3.1 Backend Setup
- [ ] Create FastAPI project structure
- [ ] Set up SQLite database
- [ ] Create database schema (repos, commits, blocks, branches)
- [ ] Set up CORS for mod communication

**Deliverable**: Running backend server with database

#### 3.2 API Endpoints - Repositories
- [ ] `POST /api/repos` - Create repository
- [ ] `GET /api/repos/:id` - Get repository
- [ ] `GET /api/repos` - List repositories

**Deliverable**: Can create and retrieve repositories via API

#### 3.3 API Endpoints - Commits
- [ ] `POST /api/repos/:id/commits` - Create commit
- [ ] `GET /api/repos/:id/commits/:commitId` - Get commit
- [ ] `GET /api/repos/:id/commits` - List commits
- [ ] `POST /api/repos/:id/push` - Push commits to remote

**Deliverable**: Can store and retrieve commits via API

#### 3.4 Mod-Backend Communication
- [ ] Implement HTTP client in mod (Java HttpClient)
- [ ] Create API client class in mod
- [ ] Handle authentication (API keys or tokens)
- [ ] Error handling and retry logic

**Deliverable**: Mod can communicate with backend API

#### 3.5 Push/Pull System
- [ ] Implement `/blockbase push` command
- [ ] Send commits to backend
- [ ] Implement `/blockbase pull` command
- [ ] Fetch and merge remote changes

**Deliverable**: Can push and pull from remote repository

---

### Phase 4: Visual Diffing (High Impact Feature)

#### 4.1 Diff Calculation
- [ ] Implement diff algorithm (compare two commits)
- [ ] Identify added blocks (in new, not in old)
- [ ] Identify removed blocks (in old, not in new)
- [ ] Identify modified blocks (same position, different type)

**Deliverable**: Can calculate differences between commits

#### 4.2 Mixin Setup for Rendering
- [ ] Set up Mixin for block rendering
- [ ] Create mixin class for BlockRenderer
- [ ] Hook into block rendering pipeline
- [ ] Test rendering hook works

**Deliverable**: Can intercept block rendering

#### 4.3 Sky Duplicate Diff Rendering
**Approach**: Create a duplicate build 30 blocks above the original in the sky to visualize changes.

- [ ] Calculate offset position (30 blocks above build)
- [ ] Place duplicate blocks in sky:
  - Added blocks: Place with green color overlay/tint
  - Removed blocks: Place with red color overlay/tint  
  - Modified blocks: Place with yellow color overlay/tint
  - Unchanged blocks: Place normally (or skip for performance)
- [ ] Track which blocks are "diff blocks" (for cleanup)
- [ ] Implement diff command: `/blockbase diff <commit1> <commit2>`
  - Default: Compare latest commit to previous commit
  - Places duplicate build in sky with color overlays
- [ ] Implement clear command: `/blockbase diff clear`
  - Removes all diff blocks from sky
  - Cleans up tracked diff blocks

**Color Overlay Implementation:**
- **Use Mixin to tint block rendering** (chosen approach)
  - Create Mixin that intercepts block rendering
  - Apply color tint based on block's diff status:
    - Green tint for added blocks
    - Red tint for removed blocks
    - Yellow tint for modified blocks
  - Store diff status for each block position
  - Clear tints when exiting diff mode

**Deliverable**: Visual diffing with sky duplicate showing color-coded changes (tinted blocks via Mixin)

---

### Phase 5: AI Agent Integration

#### 5.1 Block Context Selection
- [ ] Implement wooden axe right-click handler
- [ ] Store selected block positions
- [ ] Visual feedback for selected blocks (outline/glow)
- [ ] Clear context command: `/blockbase context clear`
- [ ] Show context status: `/blockbase context status`

**Deliverable**: Can select blocks with wooden axe for AI context

#### 5.2 Gemini API Integration
- [ ] Set up Google AI SDK in backend
- [ ] Create AI service class
- [ ] Format block data for AI (positions, types, states)
- [ ] Create prompt templates for common queries
- [ ] Handle API responses and errors

**Deliverable**: Backend can query Gemini API

#### 5.3 AI Chat Command
- [ ] Implement `/blockbase ai <question>` command
- [ ] Send selected blocks + question to backend
- [ ] Backend queries Gemini with context
- [ ] Display AI response in chat
- [ ] Handle long responses (split into multiple messages)

**Deliverable**: Can ask AI questions about selected blocks

#### 5.4 AI Build Modification (Advanced)
- [ ] AI can suggest block changes
- [ ] Parse AI response for block modifications
- [ ] Apply changes in-game (optional, for demo)
- [ ] Or show suggestions in chat

**Deliverable**: AI can analyze and suggest modifications

---

### Phase 6: Pull Requests (Collaboration)

#### 6.1 Pull Request Data Model
- [ ] Create PR data structure (title, description, from/to branches)
- [ ] Store PRs in database
- [ ] Link PRs to commits

**Deliverable**: PR data model in database

#### 6.2 PR API Endpoints
- [ ] `POST /api/repos/:id/pull-requests` - Create PR
- [ ] `GET /api/repos/:id/pull-requests` - List PRs
- [ ] `GET /api/repos/:id/pull-requests/:prId` - Get PR
- [ ] `POST /api/repos/:id/pull-requests/:prId/merge` - Merge PR

**Deliverable**: Can create and manage PRs via API

#### 6.3 PR Commands in Mod
- [ ] Implement `/blockbase pr create <title>` command
- [ ] Implement `/blockbase pr list` command
- [ ] Implement `/blockbase pr view <id>` command
- [ ] Show PR diff when viewing

**Deliverable**: Can create and view PRs from in-game

---

### Phase 7: Web Dashboard (Lower Priority)

#### 7.1 Web App Setup
- [ ] Create Next.js project
- [ ] Set up Tailwind CSS
- [ ] Create basic layout and routing

**Deliverable**: Running web application

#### 7.2 Repository Browser
- [ ] List repositories
- [ ] View repository details
- [ ] Show commit history
- [ ] Display branches

**Deliverable**: Can browse repositories on web

#### 7.3 Pull Request Interface
- [ ] View pull requests
- [ ] Create pull requests
- [ ] Show PR diff (block changes)
- [ ] Merge pull requests

**Deliverable**: Full PR workflow on web

#### 7.4 Build Visualization (Optional)
- [ ] 3D viewer for builds (Three.js)
- [ ] Or 2D top-down view
- [ ] Highlight changed blocks

**Deliverable**: Visual representation of builds

---

## Technical Implementation Details

### Mod Development (Java/Fabric)

**What is Fabric?**
- Fabric is a mod loader for Minecraft (like Forge, but lighter)
- It provides the framework/APIs we use to interact with Minecraft
- We write Java code that uses Fabric's APIs

**Development Process:**
1. **Setup**: Use Fabric's template generator to create mod project structure
2. **Code**: Write Java classes using Fabric APIs:
   - Event listeners (block placement, commands, etc.)
   - Mixins (to modify Minecraft's rendering/behavior)
   - HTTP clients (to communicate with backend)
3. **Build**: Gradle compiles Java code into a `.jar` file
4. **Test**: Place `.jar` in Minecraft's `mods` folder and launch game
5. **Fabric loads our mod** when Minecraft starts

**Key Technologies:**
- **Language**: Java 17
- **Framework**: Fabric Loader + Fabric API
- **Build Tool**: Gradle
- **Mixin**: Library for modifying Minecraft code at runtime
- **Minecraft Version**: 1.18.2

**What We Actually Write:**
- Java classes extending Fabric's base classes
- Event handlers (e.g., `BlockPlaceCallback`, `ServerCommandSource`)
- Mixin classes to hook into rendering
- HTTP client code (Java's `HttpClient` or library like OkHttp)

**Key Classes:**
- `BlockbaseMod` - Main mod class
- `BlockTracker` - Tracks block changes
- `CommitManager` - Manages commits locally
- `ApiClient` - HTTP client for backend
- `DiffRenderer` - Handles sky duplicate diff rendering (places blocks 30 blocks above)
- `AiContextManager` - Manages block selection for AI
- `BlockbaseCommands` - Command registration

**Key Mixins:**
- `BlockRenderMixin` - Hook into block rendering for overlays

### Backend (Python/FastAPI)

**Key Modules:**
- `main.py` - FastAPI app
- `database.py` - Database models and queries
- `git_logic.py` - Version control algorithms
- `ai_service.py` - Gemini API integration
- `models.py` - Pydantic models for API

**Database Schema:**
```sql
repositories (id, name, owner, default_branch, created_at)
commits (id, repo_id, message, author, timestamp, parent_id, data)
branches (id, repo_id, name, commit_id)
pull_requests (id, repo_id, title, description, from_branch, to_branch, status)
block_changes (id, commit_id, x, y, z, old_block, new_block)
```

### Communication Protocol

**Mod → Backend:**
- REST API (JSON)
- Endpoints: `/api/repos`, `/api/repos/:id/commits`, etc.
- Authentication: API key in mod config

**Backend → AI:**
- Google AI SDK
- Format: Block data + user question → Gemini API

## Demo Flow

1. **Setup** (30s)
   - Show existing redstone build
   - Initialize repository: `/blockbase init`

2. **Version Control** (60s)
   - Make changes to build
   - Stage changes: `/blockbase stage`
   - Commit: `/blockbase commit "Added 4-bit adder"`
   - Show visual diff: `/blockbase diff HEAD~1 HEAD`
   - Push: `/blockbase push`

3. **AI Assistant** (60s)
   - Select blocks with wooden axe
   - Ask AI: `/blockbase ai "explain why this circuit doesn't work"`
   - Show AI response
   - Ask AI to optimize: `/blockbase ai "optimize this circuit"`

4. **Collaboration** (30s)
   - Create branch: `/blockbase branch feature`
   - Make changes
   - Create PR: `/blockbase pr create "New feature"`
   - Show web interface (if time permits)

## Success Criteria

**MVP Complete When:**
- ✅ Can initialize repository
- ✅ Can stage, commit, and push changes
- ✅ Can see visual diffing (red/green overlays)
- ✅ Can select blocks and ask AI questions
- ✅ Can create branches and pull requests
- ✅ Demo-able end-to-end flow

## Future Enhancements (Post-Hackathon)

### Git Pull/Fetch - Live World Updates
**The Vision:** Pull changes from remote repository directly into your Minecraft world without downloading a new world file.

**Why It's Powerful:**
- Complete version control workflow (like real Git)
- Real-time collaboration - teammates' changes appear in your world
- No more manual world downloads
- True distributed version control for Minecraft

**How It Would Work:**
1. Player runs: `/blockbase pull` or `/blockbase fetch`
2. Mod requests latest commits from backend
3. Backend returns block changes (added/removed/modified)
4. Mod applies changes to current world:
   - Place new blocks at specified positions
   - Remove blocks that were deleted
   - Update modified blocks
5. Handle conflicts (if player has local changes)
6. Show summary: "Pulled 150 blocks, removed 23 blocks"

**Technical Challenges:**
- Conflict resolution (local changes vs remote changes)
- Chunk loading (ensure chunks are loaded before placing blocks)
- Performance (applying thousands of block changes)
- Safety checks (don't break player's build accidentally)
- Undo capability (in case pull goes wrong)

**Implementation Approach:**
- Store local changes before pulling
- Apply remote changes in batches
- Show diff preview before applying
- Allow selective pulling (specific commits or branches)
- Create backup before pulling (safety net)

**Demo Impact:** Low (hard to show compellingly in short demo)
**Real-World Impact:** High (makes it a complete solution)

This feature would truly make Blockbase "Git for Minecraft" - not just tracking changes, but actually syncing worlds.

## Beginner-Friendly Mod Development Guide

**Important**: This guide assumes you've never made Minecraft mods before. Every step will be explained in detail.

### Prerequisites Setup

1. **Install Java 17**
   - Download from: https://adoptium.net/ (choose Java 17)
   - Verify: Run `java -version` in terminal (should show version 17)

2. **Install IntelliJ IDEA** (recommended) or VS Code
   - IntelliJ: https://www.jetbrains.com/idea/download/
   - VS Code: https://code.visualstudio.com/

3. **Get Minecraft 1.18.2**
   - You'll need the game installed (for testing)

### Creating the Mod Project (Step-by-Step)

**Step 1: Generate Fabric Template**
- Go to: https://fabricmc.net/develop/template/
- Fill in:
  - Mod Name: `blockbase`
  - Package Name: `com.blockbase`
  - Minecraft Version: `1.18.2`
  - Mod Loader: `Fabric`
- Click "Generate" and download the ZIP

**Step 2: Extract and Open**
- Extract the ZIP to your project folder: `codejam15/mod/`
- Open the `mod/` folder in IntelliJ IDEA
- IntelliJ will ask to "Import Gradle Project" - click Yes

**Step 3: Wait for Gradle Sync**
- IntelliJ will download dependencies (this takes a few minutes)
- Wait until you see "BUILD SUCCESSFUL" in the bottom status bar

**Step 4: Test the Mod**
- In IntelliJ, find `src/main/java` folder
- Look for the main mod class (usually `BlockbaseMod.java`)
- Click the green "Run" button or press Shift+F10
- This will launch Minecraft with your mod loaded
- You should see a message in chat when the mod loads

### Project Structure Explained

```
mod/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/blockbase/
│   │   │       └── BlockbaseMod.java  ← Main mod class
│   │   └── resources/
│   │       └── fabric.mod.json       ← Mod metadata
│   └── test/                          ← Test code (ignore for now)
├── build.gradle                       ← Build configuration
└── settings.gradle                    ← Project settings
```

### Key Files You'll Edit

1. **`fabric.mod.json`** - Mod metadata (name, version, dependencies)
2. **`BlockbaseMod.java`** - Main mod class (where initialization happens)
3. **`build.gradle`** - Dependencies and build settings

### Common Tasks - Step by Step

**Adding a Command:**
1. Create new file: `src/main/java/com/blockbase/BlockbaseCommands.java`
2. Register command in `BlockbaseMod.java` (I'll show you exactly where)
3. Build: Run `./gradlew build` in terminal (or use IntelliJ's Gradle panel)
4. Find the `.jar` file in `build/libs/`
5. Copy `.jar` to `~/.minecraft/mods/` folder
6. Launch Minecraft

**Adding a Mixin:**
1. Create mixin file: `src/main/java/com/blockbase/mixins/BlockRenderMixin.java`
2. Add mixin to `fabric.mod.json` (I'll show you exactly how)
3. Build and test

**Testing Changes:**
- Every time you make changes, you need to:
  1. Build the mod (`./gradlew build`)
  2. Copy the new `.jar` to `mods/` folder
  3. Restart Minecraft

### When I Say "Try This" or "Test It"

I will provide:
- Exact file paths to edit
- Exact code to add
- Exact commands to run
- Exact locations to place files
- Screenshots or detailed descriptions if needed

**You should never have to guess where something goes!**

## Notes

- Focus on mod first, web dashboard is nice-to-have
- Visual diffing is high-impact for demo
- AI integration is differentiating feature
- Keep it simple - hackathon constraints
- SQLite for speed, can migrate to PostgreSQL later
- **User has no modding experience - provide detailed step-by-step instructions**

