# PhantomMod

<p align="center">
  <img src=".github/screenshots/clickgui.png" alt="PhantomMod ClickGUI" width="800">
</p>

| **Latest Version** | v0.7.0 | **Release Ready** |
| **Target MC**     | 1.21.11 | **Fabric 1.21.11** |

PhantomMod `v0.7.0` is a client-side Fabric mod for Minecraft `1.21.11`. It features a premium liquid glassy ClickGUI with sidebar navigation, per-module settings with sliders and presets, saved hotkeys, toast notifications, profile management, and a configurable HUD overlay.

## Features

- **Premium ClickGUI** — Liquid glassy aesthetic with category tabs, search filtering, and per-module settings
- **Profile System** — Save and load up to 4 custom configuration profiles
- **HUD Overlay** — Real-time display of enabled modules, FPS, ping, and CPS
- **Toast Notifications** — Non-intrusive on-screen confirmations for module toggles
- **Client Commands** — `/bridge` commands for quick SpeedBridge preset switching
- **Silent-by-Default Logging** — Debug logging hidden unless explicitly enabled

## Modules

### Combat
- `AimAssist`
- `AutoClicker`
- `BowAimbot`
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

### Player / Visuals
- `AntiAFK`
- `AntiBot`
- `AutoTools`
- `AutoTotem`
- `ESP`
- `FastPlace`
- `FullBright`
- `Health`
- `HUD`
- `Indicators`
- `LatencyAlerts`
- `Trajectories`
- `TimeChanger`
- `BedESP`

### SMP
- `AutoXPThrow`
- `ChestESP`
- `OreESP`
- `OreFinder`
- `ShulkerESP`

## Controls

| Action | Default |
| --- | --- |
| Open ClickGUI | `RIGHT_SHIFT` |
| SpeedBridge Presets | `/bridge legit` / `normal` / `obvious` / `blatant` |
| SpeedBridge Tower Mode | `/bridge tower` / `flat` |

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

## v0.7.0 Notes

- **Stealth & Logging**: Silent-by-default logger; activity hidden unless Debug Console or Log File is enabled in HUD settings. File logging to `phantom.log` for troubleshooting.
- **Velocity Fixing**: Thread-safe network hook using the new `Vec3` packet format for 1.21.11.
- **Combat Overhaul**: AimAssist and AutoClicker now support Axes, Maces, and Tridents.
- **SpeedBridge Polish**: Fixed `towerModeEnabled` logic and removed unimplemented "Predictive" settings.
- **AntiBot Tuning**: Name length thresholds exposed in GUI configuration.

### Visuals
- **Health**: Vertical billboarded health bars (replaces old Nametags).
- **ESP**: High-performance entity highlighting (hitboxes).
- **Trajectories**: Projectile path prediction and landing indicators.
- **FullBright**: Gamma override for night vision.
- **HUD**: Configurable corner info display with FPS/ping/CPS.
- **Indicators**: Visual state and target markers.
- **LatencyAlerts**: Ping spike notifications.

## Project Structure

```
PhantomMod/
├── src/main/java/com/phantom/
│   ├── PhantomMod.java              ← Fabric mod entrypoint + commands
│   ├── module/
│   │   ├── ModuleManager.java      ← Registry + event dispatch
│   │   └── impl/
│   │       ├── combat/            ← Combat modules
│   │       ├── movement/          ← Movement modules
│   │       ├── player/            ← Player utility modules
│   │       ├── render/            ← Visual/HUD modules
│   │       └── smp/               ← SMP-specific modules
│   ├── config/
│   │   ├── ConfigManager.java     ← phantom-memory.properties persistence
│   │   └── ProfileManager.java     ← Profile slot management
│   ├── gui/
│   │   ├── ClickGUIScreen.java    ← Main UI with glassy aesthetic
│   │   ├── ModuleSettingsScreen.java← Per-module settings
│   │   ├── ProfileScreen.java      ← Profile management UI
│   │   ├── NotificationManager.java← Toast notifications
│   │   └── framework/            ← Modern UI widgets
│   └── mixin/
│       ├── ClientPacketListenerMixin.java ← Velocity packet hook
│       ├── MultiPlayerGameModeMixin.java  ← Criticals hook
│       └── LivingEntityJumpDelayMixin.java ← NoJumpDelay hook
└── src/main/resources/
    ├── fabric.mod.json
    └── phantom.mixins.json
```

See `CONTRIBUTING.md` for the full technical reference.
