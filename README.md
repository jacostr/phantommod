# PhantomMod

PhantomMod `v1.0.3` is a client-side Fabric mod for Minecraft `1.21.11`. It ships with a compact ClickGUI, configurable module settings, saved hotkeys, toast notifications, saved profiles, and a small HUD overlay.

## Included Modules

### Combat
- `AimAssist`
- `AutoBlock`
- `AutoClicker`
- `BlockHit`
- `Criticals`
- `HitSelect`
- `JumpReset`
- `Reach`
- `RightClicker`
- `Triggerbot`
- `WTap`

### Movement
- `AlwaysSprint`
- `NoJumpDelay`
- `SafeWalk`
- `Scaffold`
- `SpeedBridge Assist`

### Player
- `AntiAFK`
- `AntiBot`
- `AutoTools`
- `FastPlace`
- `NoFall`

### Render
- `ESP`
- `FullBright`
- `HUD`
- `Indicators`

## Controls

| Action | Default |
| --- | --- |
| Open ClickGUI | `M` |

Each module can also have its own hotkey assigned from the settings screen.

If you used an older PhantomMod build before this release, Minecraft may keep the previous ClickGUI keybind in its saved controls. If the GUI still opens on a different key, open Minecraft's Controls menu and rebind `Open Phantom GUI` to `M`.

## Configuration

PhantomMod stores its settings in `phantom-memory.properties` inside your Minecraft config directory. Module enabled state, hotkeys, and per-module settings are all persisted there.

Custom profiles are stored in `config/phantom-profiles/` and can be loaded back into the main config.

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

## v1.0.3 Notes

- Built for Minecraft `1.21.11`
- Requires Fabric Loader `0.16.10+`
- Tested with Fabric API `0.140.0+1.21.11`
- Settings are saved automatically to `phantom-memory.properties`
- `Velocity` is included in the module registry and available from the ClickGUI
- ESP hidden targets use a dedicated see-through line render pass for through-wall boxes

## Project Notes

- Entrypoint: `src/main/java/com/phantom/PhantomMod.java`
- Module registry: `src/main/java/com/phantom/module/ModuleManager.java`
- Config persistence: `src/main/java/com/phantom/config/ConfigManager.java`
- GUI screens: `src/main/java/com/phantom/gui/`

See `TECHNICAL-README.md` for the full architecture reference.
