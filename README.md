# PhantomMod

| **Latest Version** | v1.0.7 | **Release Ready** |
| **Target MC**     | 1.21.11 | **Fabric 1.21.11** |

PhantomMod `v1.0.7` is a client-side Fabric mod for Minecraft `1.21.11`. It ships with a compact ClickGUI, per-module settings where applicable, saved hotkeys, toast notifications, saved profiles, and a configurable HUD overlay.

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
| Open ClickGUI | `RIGHT_SHIFT` |

Each module can also have its own hotkey assigned from the settings screen.

If you need to change the ClickGUI open bind to a different key, simply open Minecraft's native `Options -> Controls -> Key Binds` menu, scroll down to the `PhantomMod` (or `main`) category, and rebind `Open Phantom GUI` to whatever you prefer.

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

## v1.0.7 Notes

- **Nametags Fix**: Resolved rendering bug that caused nametags to vanish or jitter. Added smooth interpolation for entity positions.
- **Bug Audit & Stability**: 
    - Fixed floating health text rendering in the `Health` module.
    - Improved `AutoGG` thread-safety for reliable message dispatch.
    - Expanded `AimAssist` to support Axes, Maces, and Tridents.
    - Hardened config parsing to prevent startup crashes from malformed data.
- **TNTTimer Update**: Re-implemented interpolation for TNT fuse timers for smoother visuals.
- **Clutch Foundation**: Integrated full logic for the `Clutch` module (currently hidden from UI).

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
