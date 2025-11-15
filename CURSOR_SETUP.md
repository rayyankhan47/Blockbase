# Setting Up Java Development in Cursor IDE

## Prerequisites

1. **Java 17** - Already installed (you mentioned you have Java/Maven)
2. **Cursor IDE** - You're already using it!

## Required Extensions

Install these in Cursor (Extensions panel, Ctrl+Shift+X or Cmd+Shift+X):

1. **Extension Pack for Java** (by Microsoft)
   - This includes:
     - Language Support for Java
     - Debugger for Java
     - Test Runner for Java
     - Maven for Java
     - Project Manager for Java
     - Visual Studio IntelliCode

2. **Gradle for Java** (by Microsoft) - Optional but helpful

## Setup Steps

### 1. Install Java Extensions
- Open Cursor
- Press `Cmd+Shift+X` (Mac) or `Ctrl+Shift+X` (Windows/Linux)
- Search for "Extension Pack for Java"
- Click Install
- This will install all the Java tools you need

### 2. Open the Mod Project
- In Cursor, go to File → Open Folder
- Navigate to `codejam15/mod/` (after you generate the Fabric template)
- Cursor will detect it's a Java/Gradle project
- It will ask to install Java language server - click Yes

### 3. Wait for Indexing
- Cursor will index the Java files (first time takes a few minutes)
- You'll see "Java Projects" in the sidebar
- Wait for "Indexing..." to finish

## What You Can Do in Cursor

✅ **Code Editing**
- Full Java syntax highlighting
- Auto-completion
- Error detection (red squiggles)
- Auto-imports (when you use a class, it suggests imports)

✅ **Running Gradle Commands**
- Open integrated terminal: `` Ctrl+` `` (backtick) or View → Terminal
- Run: `./gradlew build`
- Run: `./gradlew runClient` (to test mod)

✅ **Debugging**
- Set breakpoints (click left of line numbers)
- Press F5 to start debugging
- Or use "Run and Debug" panel

✅ **File Navigation**
- `Cmd+P` (Mac) or `Ctrl+P` (Windows) to quickly open files
- `Cmd+Click` on class names to jump to definition
- `F12` to go to definition

✅ **Git Integration**
- Built-in Git support
- Can commit, push, etc. from Cursor

## Running the Mod

### Build the Mod
```bash
cd mod
./gradlew build
```

### Find the JAR
After building, the JAR will be in:
```
mod/build/libs/blockbase-1.0.0.jar
```

### Test the Mod
```bash
cd mod
./gradlew runClient
```
This will launch Minecraft with your mod loaded.

### Or Copy to Minecraft
1. Build: `./gradlew build`
2. Copy JAR: `cp build/libs/blockbase-1.0.0.jar ~/.minecraft/mods/`
3. Launch Minecraft normally

## Cursor vs IntelliJ

**What Cursor Can Do (Same as IntelliJ):**
- ✅ Code editing with autocomplete
- ✅ Error detection
- ✅ Running Gradle commands
- ✅ Debugging
- ✅ Git integration
- ✅ File navigation

**What IntelliJ Does Better:**
- More advanced refactoring tools
- Better Gradle UI (but command line works fine)
- More Java-specific features

**What Cursor Does Better:**
- **AI assistance (that's why you're using it!)**
- Lighter weight
- Better for multi-language projects
- You're already comfortable with it

## Tips for Java in Cursor

1. **Auto-imports**: When you type a class name, Cursor will suggest imports. Press `Cmd+.` (Mac) or `Ctrl+.` (Windows) to see suggestions.

2. **Quick Fix**: If you see a red error, hover over it and click the lightbulb or press `Cmd+.` to see fixes.

3. **Terminal**: Use the integrated terminal for Gradle commands. It's already in the right directory.

4. **Java Projects Panel**: Check the sidebar for "Java Projects" - shows your project structure.

5. **Problems Panel**: View → Problems shows all errors/warnings.

## Troubleshooting

**If Java extensions don't work:**
- Make sure Java 17 is in your PATH: `java -version`
- Restart Cursor after installing extensions
- Check Output panel (View → Output) for Java language server errors

**If Gradle commands don't work:**
- Make sure you're in the `mod/` directory
- Try `chmod +x gradlew` to make gradlew executable
- Use `./gradlew` (with `./`) not just `gradlew`

**If imports don't work:**
- Right-click in file → "Organize Imports"
- Or use `Cmd+Shift+O` (Mac) or `Ctrl+Shift+O` (Windows)

## Bottom Line

**Yes, you can absolutely code everything in Cursor!** 

The Java extensions give you all the essential features. The main difference is you'll use the terminal for Gradle commands instead of clicking buttons, but that's actually more transparent and gives you better control.

Plus, you get AI assistance, which is way more valuable for a hackathon!

