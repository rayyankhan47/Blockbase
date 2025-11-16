# Blockbase Mod - Build Plan

## MVP Priority: Core Version Control + Visual Diffing

**Core Features for MVP:**
1. ✅ **Staging** (`/blockbase stage`) - Select changes to commit
2. ✅ **Committing** (`/blockbase commit`) - Save snapshots
3. ✅ **Pushing** (`/blockbase push`) - Upload to remote
4. ✅ **Resetting** (`/blockbase reset`) - Rollback to previous commit
5. ✅ **Visual Diffing** (`/blockbase diff`) - 3D diff visualization (THE KILLER FEATURE)
6. ✅ **AI Agent** (`/blockbase ai`) - AI assistance with block context

**Future Features (Post-MVP):**
- Branching (can pitch as roadmap)
- Pull Requests (can pitch as roadmap)
- Pulling from remote (already decided as future)

**Why This Focus:**
- 3D diffing is the most useful for redstone engineering (see exactly what changed)
- Core git operations prove the concept works
- Reset is essential for fixing mistakes
- AI adds the "wow" factor
- Can demo end-to-end workflow without branching complexity

## Step 1: Mod Setup & Foundation

### 1.1 Generate Fabric Mod Template
- [ ] Go to https://fabricmc.net/develop/template/
- [ ] Fill in form:
  - Mod Name: `blockbase`
  - Package Name: `com.blockbase`
  - Minecraft Version: `1.18.2`
  - Mod Loader: `Fabric`
- [ ] Download the generated ZIP file
- [ ] Extract ZIP contents to `codejam15/mod/` folder
- [ ] Verify structure: `mod/src/main/java/com/blockbase/` exists

### 1.2 Open Project in Cursor IDE
- [ ] Open Cursor IDE
- [ ] Install "Extension Pack for Java" (Extensions panel: Cmd+Shift+X)
- [ ] File → Open Folder → Navigate to `codejam15/mod/`
- [ ] Cursor will detect Java/Gradle project
- [ ] Wait for Java language server to install and index
- [ ] Open integrated terminal: `` Ctrl+` `` (backtick)
- [ ] Run: `./gradlew build` (to test Gradle works)
- [ ] Verify: Build completes successfully

### 1.3 Test Basic Mod Loading
- [ ] Find `BlockbaseMod.java` in `src/main/java/com/blockbase/`
- [ ] Verify it has basic structure (mod initialization)
- [ ] In terminal, run: `./gradlew runClient`
- [ ] Minecraft should launch with mod loaded
- [ ] Check chat for mod initialization message
- [ ] **Success criteria**: Mod loads without errors
- [ ] **Alternative**: Build JAR and copy to `~/.minecraft/mods/` folder

### 1.4 Understand Project Structure
- [ ] Review `fabric.mod.json` (mod metadata)
- [ ] Review `build.gradle` (dependencies)
- [ ] Review `BlockbaseMod.java` (main mod class)
- [ ] Understand: This is where we'll register commands, events, etc.

---

## Step 2: Block Tracking System

### 2.1 Create BlockTracker Class
- [x] Create new file: `src/main/java/com/blockbase/BlockTracker.java`
- [x] Create class to track block changes
- [x] Add data structure to store changes:
  - Block position (x, y, z)
  - Old block state
  - New block state
  - Timestamp
- [x] Add methods:
  - `trackBlockPlace(BlockPos, BlockState)` - when block placed
  - `trackBlockBreak(BlockPos, BlockState)` - when block broken
  - `getChanges()` - return all tracked changes
  - `clearChanges()` - reset tracking

### 2.2 Register Block Event Listeners
- [x] In `BlockbaseMod.java`, register event callbacks:
  - `BlockPlaceCallback.EVENT.register()` - for block placement
  - `BlockBreakCallback.EVENT.register()` - for block breaking
- [x] Connect events to `BlockTracker` methods
- [x] Test: Place a block, verify it's tracked
- [x] Test: Break a block, verify it's tracked

### 2.3 Handle Block State Changes
- [x] Register `BlockStateChangeCallback` for block modifications
- [x] Track when blocks change state (e.g., redstone wire power level)
- [x] Update `BlockTracker` to handle state changes
- [x] Test: Change redstone wire power, verify tracking

### 2.4 Persist Block Changes (Local Storage)
- [x] Create `BlockChange` data class:
  - Position (x, y, z)
  - Old block type/state
  - New block type/state
- [x] Add method to save changes to JSON file
- [x] Add method to load changes from JSON file
- [x] Store in world folder: `.blockbase/changes.json`
- [x] Test: Save and load changes

---

## Step 3: Basic Command System

### 3.1 Set Up Command Registration
- [x] Create new file: `src/main/java/com/blockbase/BlockbaseCommands.java`
- [x] Create command registration method
- [x] In `BlockbaseMod.java`, call command registration on initialization
- [x] Verify: `/blockbase` command exists (should show error about missing subcommand)

### 3.2 Implement Help Command
- [x] Add `/blockbase help` command
- [x] List all available commands
- [x] Show usage examples
- [x] Test: Run `/blockbase help` in-game

### 3.3 Implement Status Command
- [x] Add `/blockbase status` command
- [x] Show:
  - Number of tracked changes
  - Number of staged changes
  - Current branch
  - Repository status
- [x] Test: Run `/blockbase status` after making changes

---

## Step 4: Repository & Commit System

### 4.1 Create Repository Structure
- [x] Create `Repository` class to manage repo data
- [x] Create `.blockbase/` folder in world directory
- [x] Create `repo.json` file with:
  - Repository ID
  - Name
  - Default branch
  - Created timestamp
- [x] Add `/blockbase init` command:
  - Create `.blockbase/` folder
  - Initialize `repo.json`
  - Create initial commit (future enhancement; currently just repo metadata)
- [x] Test: Run `/blockbase init` in a world

### 4.2 Create Commit Data Structure
- [x] Create `Commit` class:
  - Commit ID (SHA-1 hash)
  - Message
  - Author
  - Timestamp
  - List of block changes
  - Parent commit ID
  - Commit ID (SHA-1 hash)
  - Message
  - Author
  - Timestamp
  - List of block changes
  - Parent commit ID
- [x] Create method to generate commit hash
- [x] Create method to serialize commit to JSON

### 4.3 Implement Staging System
- [x] Add `StagingArea` class to track staged changes
- [x] Add `/blockbase add .` command:
  - Stage all current changes
  - Or stage specific region (future enhancement)
- [x] Store staged changes separately from unstaged
- [x] Update `/blockbase status` to show staged vs unstaged
- [x] Test: Make changes, run `/blockbase add .`, check status

### 4.4 Implement Commit Command
- [x] Add `/blockbase commit <message>` command
- [x] Take staged changes
- [x] Create `Commit` object
- [x] Generate commit hash
- [x] Link to previous commit (if exists)
- [x] Save commit to `.blockbase/commits/<commitId>.json`
- [x] Clear staging area after commit (and reset tracked changes)
- [x] Test: Stage changes, commit with message, verify commit file exists

### 4.5 Implement Commit History
- [x] Add `/blockbase log` command
- [x] Read commit files from `.blockbase/commits/`
- [x] Display commit history:
  - Commit ID (short)
  - Author
  - Message
  - Timestamp
- [x] Test: Make multiple commits, run `/blockbase log`

### 4.6 Implement Reset Command (Rollback)
- [x] Add `/blockbase reset --hard <commitId>` command
- [x] Calculate diff: current world state → target commit state (for all tracked positions)
- [x] Apply block changes to match target commit:
  - Remove blocks that don't exist in target
  - Add blocks that exist in target but not current
  - Modify blocks that changed
- [x] Add safety warning if uncommitted changes exist
- [x] Test: Make commits, make mistake, reset to previous commit, verify tracked blocks changed

---

## Step 5: Backend API Communication (MVP: Push Only)

### 5.1 Set Up HTTP Client
- [x] Add HTTP client dependency to `build.gradle` (OkHttp or Java HttpClient)
- [x] Create `ApiClient` class
- [x] Add base URL configuration (config file or command argument)
- [x] Add methods for:
  - `POST /api/repos` - create repository
  - `GET /api/repos/:id` - get repository
  - `POST /api/repos/:id/commits` - create commit
  - `GET /api/repos/:id/commits` - list commits

### 5.2 Implement Repository Creation on Backend
- [x] Add `/blockbase remote add origin <url>` command
- [x] Store remote URL in repo config
- [x] On init, optionally create repo on backend
- [x] Send repository data to backend API
- [x] Test: Create repo locally, push to backend

### 5.3 Implement Push Command
- [x] Add `/blockbase push` command
- [x] Get all local commits not on remote
- [x] Send commits to backend API
- [x] Include all block change data
- [x] Handle errors (network, authentication)
- [x] Test: Make commits, push to backend, verify on backend

---

## Step 7: Visual Diffing - In-Place Toggle (THE KILLER FEATURE)

### 7.1 Implement Diff Calculation
- [x] Create `DiffCalculator` (client or shared)
- [x] Compare Current world vs target commit:
  - Target: latest commit if ≥2; if exactly 1, compare to that; if 0, block diff mode
  - Limit comparison to a player-centered radius (e.g., 96–128 blocks)
  - Added: in Current not in Previous
  - Removed: in Previous not in Current
  - Modified: same pos, different state
- [x] Return `Diff` with categorized positions and previous states
- [x] Test: Compare current vs previous commit in a small area

### 7.2 Client DiffView + Keybinds
- [x] Create `DiffViewManager` (client) to hold:
  - Mode: Diff → Current → Previous
  - Maps: pos → previous state; pos → status (added/removed/modified)
  - Radius and player anchor
- [x] Keybinds:
  - P to cycle modes
  - Shift+P to exit/clear
- [x] Compute diff once on enter; recompute on re-enter or on demand

### 7.3 Rendering Hooks/Mixins (non-destructive)
- [x] Confirm mixin setup (client)
- [x] Previous mode: for positions in diff map, render previous state (no world mutation)
- [x] Current mode: pass-through
- [x] Diff mode: render real world with tinted overlays for changed blocks
- [x] Test: toggle modes; verify visuals without changing blocks

### 7.4 Implement Color Tinting via Mixin
- [x] In `BlockRenderMixin`, check diff status for position in Diff mode
- [x] Apply color tint based on status:
  - Green tint for added blocks
  - Red tint for removed blocks
  - Yellow tint for modified blocks
- [x] Use Minecraft's color overlay/quad tinting
- [x] Test: verify overlays on changed blocks in Diff mode

### 7.5 Diff Commands (enter/exit)
- [x] Add `/blockbase diff` to enter and compute diff (using rules above)
- [x] Add `/blockbase diff clear` to exit/clear diff mode
- [x] Test: Make changes, commit, enter diff, cycle P to view Diff/Current/Previous, exit with Shift+P or clear command

---

## Step 8: AI Agent Integration (Differentiating Feature)

### 8.1 Implement Block Context Selection
- [ ] Create `AiContextManager` class
- [ ] Register wooden axe right-click event
- [ ] When right-clicking block with wooden axe:
  - Add block position to context
  - Store block type/state
  - Visual feedback (particle effect or outline)
- [ ] Add `/blockbase context clear` command
- [ ] Add `/blockbase context status` command (show selected blocks)
- [ ] Test: Right-click blocks with wooden axe, verify tracking

### 8.2 Format Block Data for AI
- [ ] Create method to serialize selected blocks:
  - Block positions
  - Block types
  - Block states (NBT data)
  - Redstone connections (if applicable)
- [ ] Format as JSON or structured text
- [ ] Include context about redstone mechanics

### 8.3 Implement AI Chat Command
- [ ] Add `/blockbase ai <question>` command
- [ ] Get selected blocks from `AiContextManager`
- [ ] Format block data + question
- [ ] Send to backend API endpoint (backend will call Gemini)
- [ ] Display AI response in chat
- [ ] Handle long responses (split into multiple messages)
- [ ] Test: Select blocks, ask question, verify response

### 8.4 Handle AI Errors
- [ ] Handle network errors
- [ ] Handle API errors (rate limits, invalid key)
- [ ] Show user-friendly error messages
- [ ] Test: Disconnect network, verify error handling

### 8.5 AI Build Generation (Simple Builds)
- [ ] Add `/blockbase ai build <description>` command
- [ ] Format prompt to request JSON output from Gemini
- [ ] Request structured format: `{"actions": [{"type": "place", "x": ..., "y": ..., "z": ..., "block": "..."}]}`
- [ ] Parse JSON response in backend (handle markdown code blocks)
- [ ] Validate block placements (coordinates, block names, world bounds)
- [ ] Send validated actions to mod
- [ ] Create `BuildApplier` class in mod to apply block changes
- [ ] Add confirmation step: "AI wants to place X blocks. Type '/blockbase ai build confirm'"
- [ ] Store pending build until confirmed
- [ ] Start with simple blocks only (redstone_wire, stone, basic blocks)
- [ ] Test: "Place a 3x3 square of redstone wire"
- [ ] Add undo capability (store original blocks before applying)

**Implementation Details:**
- See `AI_BUILD_GUIDE.md` for full technical approach
- Use JSON structured output from Gemini
- Parse and validate in backend
- Apply via `BuildApplier` class in mod
- Safety: Confirmation required, bounds checking, undo capability

**Deliverable**: AI can generate and apply simple builds

---

## Step 9: Future Features (Post-MVP - Can Pitch as Roadmap)

### 9.1 Branching System (Future)
- [ ] Create and switch branches
- [ ] Track commits per branch
- [ ] Merge branches

### 9.2 Pull Requests (Future)
- [ ] Pull Requests for collaboration
- [ ] Pulling from remote (sync world with remote)

---

## Step 10: Polish & Testing

### 10.1 Error Handling
- [ ] Add try-catch blocks for all commands
- [ ] Show user-friendly error messages
- [ ] Log errors for debugging
- [ ] Test: Trigger various error conditions

### 10.2 Command Validation
- [ ] Validate command arguments
- [ ] Check prerequisites (e.g., repo must be initialized)
- [ ] Show helpful error messages
- [ ] Test: Run commands with invalid arguments

### 10.3 Performance Optimization
- [ ] Optimize block tracking (don't track every tick)
- [ ] Batch API requests when possible
- [ ] Cache frequently accessed data
- [ ] Test: Large builds, many changes

### 10.4 Documentation
- [ ] Update help command with all commands
- [ ] Add usage examples
- [ ] Document configuration options
- [ ] Test: Run `/blockbase help`, verify completeness

---

## Success Criteria

**MVP Complete When:**
- ✅ Can initialize repository
- ✅ Can track block changes
- ✅ Can stage changes (`/blockbase stage`)
- ✅ Can commit changes (`/blockbase commit "message"`)
- ✅ Can push to remote (`/blockbase push`)
- ✅ Can reset/rollback to previous commit (`/blockbase reset --hard <commit>`)
- ✅ Can see visual diffing (sky duplicate with color tints) - **THE KILLER FEATURE**
- ✅ Can select blocks with wooden axe and ask AI questions (`/blockbase ai`)
- ✅ All commands work without crashes
- ✅ Demo-able end-to-end workflow

**Not Required for MVP:**
- ❌ Branching (can pitch as future roadmap)
- ❌ Pull Requests (can pitch as future roadmap)
- ❌ Pulling from remote (already decided as future)

---

## Notes

- Start with Step 1 and work sequentially
- Test after each major step
- Don't move to next step until current one works
- If stuck, we can debug together
- Focus on getting core features working first, polish later

