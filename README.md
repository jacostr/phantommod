# PhantomMod

PhantomMod is a client-side Fabric mod for Minecraft `1.21.11`. It ships with a compact ClickGUI, configurable module settings, saved hotkeys, toast notifications, and a small HUD overlay.

## Included Modules

### Combat
- `AimAssist`
- `AutoBlock`
- `Criticals`
- `Reach`
- `Velocity`

### Movement
- `AlwaysSprint`
- `NoJumpDelay`
- `Scaffold`
- `SpeedBridge Assist`

### Player
- `AutoTools`
- `NoFall`

### Render
- `ESP`
- `FullBright`
- `HUD`

## Controls

| Action | Default |
| --- | --- |
| Open ClickGUI | `Right Shift` |

Each module can also have its own hotkey assigned from the settings screen.

## Configuration

PhantomMod stores its settings in `phantom-memory.properties` inside your Minecraft config directory. Module enabled state, hotkeys, and per-module settings are all persisted there.

## Installation

1. Install Fabric Loader for Minecraft `1.21.11`.
2. Install Fabric API for Minecraft `1.21.11`.
3. Place the PhantomMod jar in your `.minecraft/mods` folder.
4. Launch the Fabric profile.

## Build

Requirements:
- Java `21`
- Minecraft `1.21.11`
- Fabric Loader `0.16.10+`
- Fabric API for Minecraft `1.21.11`

Tested with:
- Fabric API `0.140.0+1.21.11`

Build with:

```bash
./gradlew build
```

The built jar is written to `build/libs/`.

## First Release Notes

- Tested against Minecraft `1.21.11`
- Requires Fabric Loader `0.16.10+`
- Tested with Fabric API `0.140.0+1.21.11`
- Settings are saved automatically to `phantom-memory.properties`

## Project Notes

- Entrypoint: `src/main/java/com/phantom/PhantomMod.java`
- Module registry: `src/main/java/com/phantom/module/ModuleManager.java`
- Config persistence: `src/main/java/com/phantom/config/ConfigManager.java`
- GUI screens: `src/main/java/com/phantom/gui/`

See `TECHNICAL.md` for the full architecture reference.
