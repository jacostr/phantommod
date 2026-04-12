# PhantomMod

PhantomMod `v1.0.4` is a client-side Fabric mod for Minecraft `1.21.11`. It ships with a compact ClickGUI, configurable module settings, saved hotkeys, toast notifications, saved profiles, and a small HUD overlay.

## Included Modules

### Combat
- `AimAssist`
- `AutoBlock`
- `AutoClicker`
- `BlockHit`
- `Criticals`
- `HitSelect`
- `JumpReset`
- `NoHitDelay`
- `Reach`
- `RightClicker`
- `SilentAura`
- `Triggerbot`
- `Velocity`
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
- `AutoTotem`
- `FastPlace`
- `Freecam`
- `NameChanger`
- `NoFall`

### Render
- `ESP`
- `FullBright`
- `HealthBar`
- `HUD`
- `Indicators`

### SMP
- `AutoXPThrow`
- `BedESP`
- `ChestESP`
- `OreESP`
- `ShulkerESP`

## Controls

| Action | Default |
| --- | --- |
| Open ClickGUI | `M` |

Each module can also have its own hotkey assigned from the settings screen.

If you used an older PhantomMod build before this release, Minecraft may keep the previous ClickGUI keybind in its saved controls. If the GUI still opens on a different key, open Minecraft's Controls menu and rebind `Open Phantom GUI` to `M`.

## Configuration

PhantomMod stores its settings in `phantom-memory.properties` inside your Minecraft config directory. Module enabled state, hotkeys, and per-module settings are all persisted there.

Custom profiles are stored in `config/phantom-profiles/` and can be managed from the "Profiles" menu in the ClickGUI.

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

## v1.0.4 Notes

- Added the "SMP" tab for survival-oriented utilities.
- Added a 4-slot configurable Profile management UI to replace the legacy profiles.
- Integrated `SilentAura` and `NoHitDelay` into the combat modules.
- Refactored `ESP` to split out specific block ESPs into the SMP category (Chest, Ore, Bed, Shulker).

## Project Notes

- Entrypoint: `src/main/java/com/phantom/PhantomMod.java`
- Module registry: `src/main/java/com/phantom/module/ModuleManager.java`
- Config persistence: `src/main/java/com/phantom/config/ConfigManager.java`
- GUI screens: `src/main/java/com/phantom/gui/`

See `CONTRIBUTING.md` for the full reference.
