# PhantomMod

| **Latest Version** | v1.0.6 | **Release Ready** |
| **Target MC**     | 1.21.11 | **Fabric 1.21.11** |

PhantomMod `v1.0.6` is a client-side Fabric mod for Minecraft `1.21.11`. It ships with a compact ClickGUI, per-module settings where applicable, saved hotkeys, toast notifications, saved profiles, and a configurable HUD overlay.

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
- `WaterClutch`
- `WTap`
- `WeaponCycler`

### Movement
- `AlwaysSprint`
- `NoJumpDelay`
- `SafeWalk`
- `Scaffold`
- `SpeedBridge`

### Player
- `AntiAFK`
- `AntiBot`
- `Arrows`
- `AutoTools`
- `AutoTotem`
- `ESP`
- `FastPlace`
- `Freecam`
- `FullBright`
- `HUD`
- `Indicators`
- `LatencyAlerts`
- `Health`
- `NoFall`

### SMP
- `AutoXPThrow`
- `BedESP`
- `ChestESP`
- `OreESP`
- `OreFinder`
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

## v1.0.6 Notes

- **Health Module Refactor**: Replaced `Nametags` and old `HealthBar` with a dedicated `Health` module featuring billboarded, scaling vertical bars.
- **Optimization**: Simplified `ESP` to strictly handle entity hitboxes, improving performance.
- **Triggerbot Update**: Added target mode (Players, Mobs, or Both).
- **Cleanup**: Removed `Tracers`, `AutoGapple`, and simplified `Scaffold`.
- **UI/UX**: Moved HUD settings to a dedicated bottom-right button and added profile overwrite confirmations.

### Visuals
- **Health**: Vertical indicators with billboarded scaling bars (replaces old Nametags).
- **ESP**: High-performance entity highlighting (hitboxes).
- **Arrows**: Directional indicators for nearby threats.
- **FullBright**: Gamma override for night vision.
- **HUD**: Configurable corner info display.
- **Indicators**: Visual state and target markers.
- **LatencyAlerts**: Ping spike notifications.

## Project Notes

- Entrypoint: `src/main/java/com/phantom/PhantomMod.java`
- Module registry: `src/main/java/com/phantom/module/ModuleManager.java`
- Config persistence: `src/main/java/com/phantom/config/ConfigManager.java`
- GUI screens: `src/main/java/com/phantom/gui/`

See `CONTRIBUTING.md` for the full reference.
