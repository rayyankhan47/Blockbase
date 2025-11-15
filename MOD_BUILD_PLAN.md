# Blockbase Mod - Build Plan

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

### 1.2 Open Project in IDE
- [ ] Open IntelliJ IDEA (or VS Code)
- [ ] Open the `mod/` folder as a project
- [ ] If IntelliJ asks to "Import Gradle Project", click Yes
- [ ] Wait for Gradle sync to complete (downloads dependencies)
- [ ] Verify: Bottom status bar shows "BUILD SUCCESSFUL"

### 1.3 Test Basic Mod Loading
- [ ] Find `BlockbaseMod.java` in `src/main/java/com/blockbase/`
- [ ] Verify it has basic structure (mod initialization)
- [ ] Click Run button (green play icon) or press Shift+F10
- [ ] Minecraft should launch with mod loaded
- [ ] Check chat for mod initialization message
- [ ] **Success criteria**: Mod loads without errors

### 1.4 Understand Project Structure
- [ ] Review `fabric.mod.json` (mod metadata)
- [ ] Review `build.gradle` (dependencies)
- [ ] Review `BlockbaseMod.java` (main mod class)
- [ ] Understand: This is where we'll register commands, events, etc.

---

## Step 2: Block Tracking System

### 2.1 Create BlockTracker Class
- [ ] Create new file: `src/main/java/com/blockbase/BlockTracker.java`
- [ ] Create class to track block changes
- [ ] Add data structure to store changes:
  - Block position (x, y, z)
  - Old block state
  - New block state
  - Timestamp
- [ ] Add methods:
  - `trackBlockPlace(BlockPos, BlockState)` - when block placed
  - `trackBlockBreak(BlockPos, BlockState)` - when block broken
  - `getChanges()` - return all tracked changes
  - `clearChanges()` - reset tracking

### 2.2 Register Block Event Listeners
- [ ] In `BlockbaseMod.java`, register event callbacks:
  - `BlockPlaceCallback.EVENT.register()` - for block placement
  - `BlockBreakCallback.EVENT.register()` - for block breaking
- [ ] Connect events to `BlockTracker` methods
- [ ] Test: Place a block, verify it's tracked
- [ ] Test: Break a block, verify it's tracked

### 2.3 Handle Block State Changes
- [ ] Register `BlockStateChangeCallback` for block modifications
- [ ] Track when blocks change state (e.g., redstone wire power level)
- [ ] Update `BlockTracker` to handle state changes
- [ ] Test: Change redstone wire power, verify tracking

### 2.4 Persist Block Changes (Local Storage)
- [ ] Create `BlockChange` data class:
  - Position (x, y, z)
  - Old block type/state
  - New block type/state
- [ ] Add method to save changes to JSON file
- [ ] Add method to load changes from JSON file
- [ ] Store in world folder: `.blockbase/changes.json`
- [ ] Test: Save and load changes

---

## Step 3: Basic Command System

### 3.1 Set Up Command Registration
- [ ] Create new file: `src/main/java/com/blockbase/BlockbaseCommands.java`
- [ ] Create command registration method
- [ ] In `BlockbaseMod.java`, call command registration on initialization
- [ ] Verify: `/blockbase` command exists (should show error about missing subcommand)

### 3.2 Implement Help Command
- [ ] Add `/blockbase help` command
- [ ] List all available commands
- [ ] Show usage examples
- [ ] Test: Run `/blockbase help` in-game

### 3.3 Implement Status Command
- [ ] Add `/blockbase status` command
- [ ] Show:
  - Number of tracked changes
  - Number of staged changes
  - Current branch
  - Repository status
- [ ] Test: Run `/blockbase status` after making changes

---

## Step 4: Repository & Commit System

### 4.1 Create Repository Structure
- [ ] Create `Repository` class to manage repo data
- [ ] Create `.blockbase/` folder in world directory
- [ ] Create `repo.json` file with:
  - Repository ID
  - Name
  - Default branch
  - Created timestamp
- [ ] Add `/blockbase init` command:
  - Create `.blockbase/` folder
  - Initialize `repo.json`
  - Create initial commit
- [ ] Test: Run `/blockbase init` in a world

### 4.2 Create Commit Data Structure
- [ ] Create `Commit` class:
  - Commit ID (SHA-1 hash)
  - Message
  - Author
  - Timestamp
  - List of block changes
  - Parent commit ID
- [ ] Create method to generate commit hash
- [ ] Create method to serialize commit to JSON

### 4.3 Implement Staging System
- [ ] Add `StagingArea` class to track staged changes
- [ ] Add `/blockbase stage` command:
  - Stage all current changes
  - Or stage specific region (future enhancement)
- [ ] Store staged changes separately from unstaged
- [ ] Update `/blockbase status` to show staged vs unstaged
- [ ] Test: Make changes, run `/blockbase stage`, check status

### 4.4 Implement Commit Command
- [ ] Add `/blockbase commit <message>` command
- [ ] Take staged changes
- [ ] Create `Commit` object
- [ ] Generate commit hash
- [ ] Link to previous commit (if exists)
- [ ] Save commit to `.blockbase/commits/<commitId>.json`
- [ ] Clear staging area after commit
- [ ] Test: Stage changes, commit with message, verify commit file exists

### 4.5 Implement Commit History
- [ ] Add `/blockbase log` command
- [ ] Read all commit files from `.blockbase/commits/`
- [ ] Display commit history:
  - Commit ID (short)
  - Author
  - Message
  - Timestamp
- [ ] Test: Make multiple commits, run `/blockbase log`

---

## Step 5: Branch System

### 5.1 Create Branch Data Structure
- [ ] Create `Branch` class:
  - Branch name
  - Commit ID it points to
- [ ] Store branches in `.blockbase/branches.json`
- [ ] Track current branch in repository

### 5.2 Implement Branch Commands
- [ ] Add `/blockbase branch <name>` - create new branch
- [ ] Add `/blockbase branch --list` - list all branches
- [ ] Add `/blockbase checkout <branch>` - switch branch
- [ ] Update current branch pointer
- [ ] Test: Create branch, switch branches, verify branch pointer

### 5.3 Handle Branch-Specific Changes
- [ ] Store commits per branch
- [ ] Track which branch changes belong to
- [ ] When switching branches, show branch-specific commits
- [ ] Test: Make commits on different branches, verify isolation

---

## Step 6: Backend API Communication

### 6.1 Set Up HTTP Client
- [ ] Add HTTP client dependency to `build.gradle` (OkHttp or Java HttpClient)
- [ ] Create `ApiClient` class
- [ ] Add base URL configuration (config file or command argument)
- [ ] Add methods for:
  - `POST /api/repos` - create repository
  - `GET /api/repos/:id` - get repository
  - `POST /api/repos/:id/commits` - create commit
  - `GET /api/repos/:id/commits` - list commits

### 6.2 Implement Repository Creation on Backend
- [ ] Add `/blockbase remote add <url>` command
- [ ] Store remote URL in repo config
- [ ] On init, optionally create repo on backend
- [ ] Send repository data to backend API
- [ ] Test: Create repo locally, push to backend

### 6.3 Implement Push Command
- [ ] Add `/blockbase push` command
- [ ] Get all local commits not on remote
- [ ] Send commits to backend API
- [ ] Include all block change data
- [ ] Handle errors (network, authentication)
- [ ] Test: Make commits, push to backend, verify on backend

### 6.4 Add Authentication
- [ ] Add API key configuration
- [ ] Store API key in config file (`.blockbase/config.json`)
- [ ] Add API key to HTTP requests (header)
- [ ] Handle authentication errors
- [ ] Test: Push with valid/invalid API key

---

## Step 7: Visual Diffing - Sky Duplicate

### 7.1 Implement Diff Calculation
- [ ] Create `DiffCalculator` class
- [ ] Add method to compare two commits:
  - Added blocks (in new, not in old)
  - Removed blocks (in old, not in new)
  - Modified blocks (same position, different type)
- [ ] Return `Diff` object with categorized changes
- [ ] Test: Compare two commits, verify diff calculation

### 7.2 Implement Sky Duplicate Placement
- [ ] Create `DiffRenderer` class
- [ ] Add method to place duplicate blocks in sky:
  - Calculate build bounding box
  - Calculate offset (30 blocks above)
  - Place blocks at offset positions
- [ ] Track which blocks are "diff blocks" (for cleanup)
- [ ] Test: Place duplicate build in sky

### 7.3 Set Up Mixin for Block Rendering
- [ ] Add Mixin dependency to `build.gradle` (if not already present)
- [ ] Create `src/main/resources/blockbase.mixins.json` file
- [ ] Create mixin class: `src/main/java/com/blockbase/mixins/BlockRenderMixin.java`
- [ ] Hook into block rendering method
- [ ] Add mixin to `fabric.mod.json`
- [ ] Test: Mixin loads without errors

### 7.4 Implement Color Tinting via Mixin
- [ ] In `BlockRenderMixin`, check if block is a "diff block"
- [ ] Store diff status (added/removed/modified) for each block position
- [ ] Apply color tint based on status:
  - Green tint for added blocks
  - Red tint for removed blocks
  - Yellow tint for modified blocks
- [ ] Use Minecraft's color overlay system
- [ ] Test: Place diff blocks, verify color tints appear

### 7.5 Implement Diff Command
- [ ] Add `/blockbase diff <commit1> <commit2>` command
  - Default: Compare latest commit to previous
  - Calculate diff
  - Place duplicate in sky
  - Apply color tints
- [ ] Add `/blockbase diff clear` command
  - Remove all diff blocks from sky
  - Clear diff status tracking
- [ ] Test: Make changes, commit, run diff, verify sky duplicate with colors

---

## Step 8: AI Agent Integration

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

---

## Step 9: Pull Requests (Basic)

### 9.1 Create PR Data Structure
- [ ] Create `PullRequest` class:
  - PR ID
  - Title
  - Description
  - From branch
  - To branch
  - Status (open/merged/closed)
  - List of commit IDs
- [ ] Store PRs on backend (not in mod)

### 9.2 Implement PR Commands
- [ ] Add `/blockbase pr create <title>` command
  - Get current branch
  - Get target branch (default: main)
  - Send PR data to backend
- [ ] Add `/blockbase pr list` command
  - Fetch PRs from backend
  - Display in chat
- [ ] Add `/blockbase pr view <id>` command
  - Fetch PR details
  - Show diff
- [ ] Test: Create branch, make changes, create PR

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
- ✅ Can stage, commit, and push changes
- ✅ Can create and switch branches
- ✅ Can see visual diffing (sky duplicate with color tints)
- ✅ Can select blocks and ask AI questions
- ✅ Can create pull requests
- ✅ All commands work without crashes
- ✅ Demo-able end-to-end flow

---

## Notes

- Start with Step 1 and work sequentially
- Test after each major step
- Don't move to next step until current one works
- If stuck, we can debug together
- Focus on getting core features working first, polish later

