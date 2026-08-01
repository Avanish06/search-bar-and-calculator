# Fabric Inventory Search & Calculator Overlay Mod

This is a Minecraft Fabric client-side mod that adds a bottom-docked search bar and live calculator to all container screens.

## Features
1. **Search Mode**: Typing a plain query dims all slots whose item doesn't match, and shows a live running total of matching item counts. Supports `&&` logic and full Lore scanning.
2. **Calculator Mode**: Typing `=` followed by a math expression evaluates it live and shows the result inline as ghost text, supporting `+ - * /`, decimals, and `k`/`m` shorthand.

## Requirements
- **Minecraft**: 26.1.2
- **Fabric Loader**: >=0.18.4
- **Fabric API**: Required
- **Java**: 25
- **Mappings**: Mojang Official Mappings (Mojmaps)

## Build Instructions
This project uses Fabric Loom and Gradle. To build the mod in your development environment, open a terminal in this directory and run:

```bash
./gradlew build
```

## Testing in a Dev Environment
To launch a Minecraft client with the mod loaded directly from the source code, run:

```bash
./gradlew runClient
```

## Configuration
The mod generates a configuration file at `config/invsearch.json` with the following options:
- `enabled`: (default: `true`) Master toggle for the mod.
- `rememberLastQuery`: (default: `true`) Persist the last search string across screen opens for the session.
- `includePlayerInventory`: (default: `true`) Whether matching/dimming applies to the player's own inventory slots within the same screen.
- `dimOpacity`: (default: `153`) The opacity of the dark overlay on non-matching slots (0-255).
