# AI Build Generation - Implementation Guide

## Overview

This guide explains how to make Gemini generate simple builds and apply them in Minecraft.

## Approach: Structured JSON Output

### Step 1: Prompt Engineering

We need to get Gemini to output structured JSON with block placements.

**Example Prompt:**
```
You are a Minecraft building assistant. The user wants you to build: [description]

Current context (selected blocks):
- Block at (10, 64, 20): minecraft:stone
- Block at (11, 64, 20): minecraft:air

Output your response as JSON in this exact format:
{
  "actions": [
    {
      "type": "place",
      "x": 10,
      "y": 64,
      "z": 20,
      "block": "minecraft:redstone_wire",
      "state": {}
    },
    {
      "type": "place",
      "x": 11,
      "y": 64,
      "z": 20,
      "block": "minecraft:repeater",
      "state": {
        "facing": "north",
        "delay": 2
      }
    },
    {
      "type": "remove",
      "x": 12,
      "y": 64,
      "z": 20
    }
  ],
  "explanation": "I'm placing a redstone wire and repeater to create a 2-tick delay circuit."
}

IMPORTANT:
- Only output valid JSON, no markdown code blocks
- Use absolute coordinates
- Block names must be valid Minecraft block IDs (e.g., "minecraft:redstone_wire")
- For state properties, use Minecraft NBT format
- Validate all placements before suggesting
```

### Step 2: Response Parsing

**Backend (Python) - Parse Gemini Response:**

```python
import json
import re
from typing import List, Dict, Optional

def parse_ai_response(response: str) -> Optional[Dict]:
    """
    Parse Gemini's JSON response.
    Handles cases where response might have markdown code blocks.
    """
    # Remove markdown code blocks if present
    response = response.strip()
    if response.startswith("```json"):
        response = response[7:]  # Remove ```json
    if response.startswith("```"):
        response = response[3:]   # Remove ```
    if response.endswith("```"):
        response = response[:-3]  # Remove closing ```
    response = response.strip()
    
    try:
        data = json.loads(response)
        return data
    except json.JSONDecodeError as e:
        # Try to extract JSON from text
        json_match = re.search(r'\{.*\}', response, re.DOTALL)
        if json_match:
            try:
                return json.loads(json_match.group())
            except:
                pass
        raise ValueError(f"Failed to parse JSON: {e}")

def validate_actions(actions: List[Dict], world_bounds: Dict) -> List[Dict]:
    """
    Validate block placement actions.
    - Check coordinates are within world bounds
    - Validate block names
    - Check for conflicts
    """
    valid_actions = []
    
    for action in actions:
        # Validate action type
        if action.get("type") not in ["place", "remove", "modify"]:
            continue
            
        # Validate coordinates
        x, y, z = action.get("x"), action.get("y"), action.get("z")
        if not all(isinstance(coord, (int, float)) for coord in [x, y, z]):
            continue
            
        # Check world bounds (example)
        if not (world_bounds["min_x"] <= x <= world_bounds["max_x"]):
            continue
        # ... similar for y, z
        
        # Validate block name
        if action.get("type") == "place":
            block_name = action.get("block", "")
            if not block_name.startswith("minecraft:"):
                continue
                
        valid_actions.append(action)
    
    return valid_actions
```

### Step 3: Send to Mod

**Backend API Endpoint:**

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI()

class BuildRequest(BaseModel):
    description: str
    context_blocks: List[Dict]  # Selected blocks from player
    world_bounds: Dict  # Optional: bounds of build area

@app.post("/api/ai/build")
async def generate_build(request: BuildRequest):
    """
    Generate build instructions from AI.
    """
    # Format prompt with context
    prompt = format_build_prompt(
        description=request.description,
        context_blocks=request.context_blocks
    )
    
    # Query Gemini
    response = await gemini_client.generate(prompt)
    
    # Parse response
    try:
        parsed = parse_ai_response(response)
        actions = parsed.get("actions", [])
        explanation = parsed.get("explanation", "")
    except Exception as e:
        raise HTTPException(400, f"Failed to parse AI response: {e}")
    
    # Validate actions
    valid_actions = validate_actions(actions, request.world_bounds)
    
    return {
        "actions": valid_actions,
        "explanation": explanation,
        "total_blocks": len(valid_actions)
    }
```

### Step 4: Apply in Mod

**Mod (Java) - Apply Block Changes:**

```java
package com.blockbase;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

public class BuildApplier {
    
    public static class BlockAction {
        public String type;  // "place", "remove", "modify"
        public int x, y, z;
        public String block;  // e.g., "minecraft:redstone_wire"
        public Map<String, Object> state;  // Block state properties
    }
    
    /**
     * Apply AI-generated build actions to the world.
     */
    public static void applyBuild(ServerWorld world, List<BlockAction> actions) {
        for (BlockAction action : actions) {
            BlockPos pos = new BlockPos(action.x, action.y, action.z);
            
            switch (action.type) {
                case "place":
                    placeBlock(world, pos, action.block, action.state);
                    break;
                case "remove":
                    removeBlock(world, pos);
                    break;
                case "modify":
                    modifyBlock(world, pos, action.block, action.state);
                    break;
            }
        }
    }
    
    private static void placeBlock(ServerWorld world, BlockPos pos, 
                                   String blockId, Map<String, Object> state) {
        // Parse block ID
        Block block = Registry.BLOCK.get(new Identifier(blockId));
        if (block == null) {
            System.err.println("Invalid block: " + blockId);
            return;
        }
        
        // Get default block state
        BlockState blockState = block.getDefaultState();
        
        // Apply state properties (e.g., facing, delay for repeaters)
        if (state != null) {
            blockState = applyBlockState(blockState, state);
        }
        
        // Place block
        world.setBlockState(pos, blockState);
    }
    
    private static BlockState applyBlockState(BlockState state, 
                                             Map<String, Object> properties) {
        // Apply properties like facing, delay, etc.
        // This is Minecraft-specific - need to handle each block type
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Example: Handle repeater delay
            if (key.equals("delay") && state.getBlock() instanceof RepeaterBlock) {
                int delay = (int) value;
                state = state.with(RepeaterBlock.DELAY, Math.max(1, Math.min(4, delay)));
            }
            
            // Example: Handle facing direction
            if (key.equals("facing") && state.contains(Properties.HORIZONTAL_FACING)) {
                Direction dir = Direction.byName(value.toString());
                if (dir != null) {
                    state = state.with(Properties.HORIZONTAL_FACING, dir);
                }
            }
            
            // Add more property handlers as needed
        }
        
        return state;
    }
    
    private static void removeBlock(ServerWorld world, BlockPos pos) {
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }
}
```

### Step 5: Command Integration

**Mod Command:**

```java
public class BlockbaseCommands {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("blockbase")
            .then(CommandManager.literal("ai")
                .then(CommandManager.literal("build")
                    .then(CommandManager.argument("description", StringArgumentType.greedyString())
                        .executes(context -> {
                            String description = StringArgumentType.getString(context, "description");
                            return executeBuild(context.getSource(), description);
                        })
                    )
                )
            )
        );
    }
    
    private static int executeBuild(ServerCommandSource source, String description) {
        // Get selected blocks (from AiContextManager)
        List<BlockData> contextBlocks = AiContextManager.getSelectedBlocks(source.getPlayer());
        
        // Call backend API
        BuildResponse response = ApiClient.generateBuild(description, contextBlocks);
        
        if (response.actions.isEmpty()) {
            source.sendFeedback(new LiteralText("AI couldn't generate a build."), false);
            return 0;
        }
        
        // Ask for confirmation
        source.sendFeedback(new LiteralText(
            String.format("AI wants to place %d blocks. Type '/blockbase ai build confirm' to apply.",
                         response.actions.size())
        ), false);
        
        // Store pending build
        PendingBuildManager.setPendingBuild(source.getPlayer(), response);
        
        return 1;
    }
    
    private static int confirmBuild(ServerCommandSource source) {
        BuildResponse build = PendingBuildManager.getPendingBuild(source.getPlayer());
        if (build == null) {
            source.sendFeedback(new LiteralText("No pending build."), false);
            return 0;
        }
        
        // Apply build
        ServerWorld world = source.getWorld();
        BuildApplier.applyBuild(world, build.actions);
        
        source.sendFeedback(new LiteralText(
            String.format("Applied build: %s", build.explanation)
        ), false);
        
        return 1;
    }
}
```

## Example: Simple Build

**User Command:**
```
/blockbase ai build "place a 3x3 square of redstone wire"
```

**Gemini Response (JSON):**
```json
{
  "actions": [
    {"type": "place", "x": 10, "y": 64, "z": 20, "block": "minecraft:redstone_wire"},
    {"type": "place", "x": 11, "y": 64, "z": 20, "block": "minecraft:redstone_wire"},
    {"type": "place", "x": 12, "y": 64, "z": 20, "block": "minecraft:redstone_wire"},
    {"type": "place", "x": 10, "y": 64, "z": 21, "block": "minecraft:redstone_wire"},
    {"type": "place", "x": 11, "y": 64, "z": 21, "block": "minecraft:redstone_wire"},
    {"type": "place", "x": 12, "y": 64, "z": 21, "block": "minecraft:redstone_wire"},
    {"type": "place", "x": 10, "y": 64, "z": 22, "block": "minecraft:redstone_wire"},
    {"type": "place", "x": 11, "y": 64, "z": 22, "block": "minecraft:redstone_wire"},
    {"type": "place", "x": 12, "y": 64, "z": 22, "block": "minecraft:redstone_wire"}
  ],
  "explanation": "Placed a 3x3 grid of redstone wire starting at (10, 64, 20)"
}
```

**Result:**
- Backend parses JSON
- Validates actions
- Sends to mod
- Mod applies block placements
- User sees 3x3 redstone wire grid

## Limitations & Challenges

### 1. Coordinate System
- Gemini doesn't know player's exact position
- **Solution**: Use relative coordinates or player's position as origin
- **Better**: Let user select a "build origin" block

### 2. Block State Complexity
- Different blocks have different state properties
- **Solution**: Start with simple blocks (redstone wire, basic blocks)
- Expand to complex blocks (repeaters, comparators) later

### 3. Validation
- Need to check if placement is valid (not in air, etc.)
- **Solution**: Validate in backend before sending to mod
- Mod should also validate before placing

### 4. Error Handling
- What if Gemini outputs invalid JSON?
- What if block doesn't exist?
- **Solution**: Try-catch, fallback to text explanation

## MVP Implementation Strategy

### Phase 1: Simple Blocks Only
- Start with: `redstone_wire`, `stone`, `dirt`, `wood`, `glass`
- No complex state properties
- Just place/remove blocks

### Phase 2: Add Block States
- Support `facing` for directional blocks
- Support `delay` for repeaters
- Support basic properties

### Phase 3: Complex Structures
- Multi-block structures
- Redstone circuits
- Timing considerations

## Safety Features

1. **Confirmation Required**
   - Always ask user to confirm before applying
   - Show preview of what will be placed

2. **Bounds Checking**
   - Limit build area (e.g., 50x50x50 blocks)
   - Don't allow building outside bounds

3. **Undo Capability**
   - Store original blocks before applying
   - Allow `/blockbase ai build undo`

4. **Validation**
   - Check block names are valid
   - Check coordinates are reasonable
   - Don't place blocks in invalid positions

## Testing Strategy

1. **Simple Test**: "Place 5 redstone wire blocks in a line"
2. **Medium Test**: "Create a 2x2 square of stone"
3. **Complex Test**: "Build a simple redstone clock"

Start simple, expand gradually!

