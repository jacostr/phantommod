# PhantomMod v0.9.0 — Technical Reference

> A Fabric 1.21.11 client-side mod for Minecraft Java Edition.

---

## Table of Contents
1. [Overview & Goals](#overview--goals)
2. [Technology Stack](#technology-stack)
3. [Project Layout](#project-layout)
4. [Architecture](#architecture)
   - [Entry Point](#entry-point)
   - [Module System](#module-system)
   - [Config System](#config-system)
   - [Mixin System](#mixin-system)
   - [GUI System](#gui-system)
5. [Module Reference](#module-reference)
6. [Design Decisions](#design-decisions)
7. [Build & Installation](#build--installation)

---

## Overview & Goals

PhantomMod is a self-contained Fabric client mod with these goals:
- **Minimal footprint** — no third-party libraries beyond Fabric API
- **Clean codebase** — well-documented, readable structure
- **Configurable detectability** — every automation module exposes sliders and presets

---

## Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| **Minecraft Java** | 1.21.11 | Target version |
| **Fabric Loader** | 0.16.10+ | Mod loader |
| **Fabric API** | 0.140.0+1.21.11 | Event hooks |
| **Fabric Loom** | 1.14.10 | Build plugin + mixin processing |
| **Mixin** | Bundled via Loom | Bytecode injection |
| **Java** | 21 | Required by Minecraft 1.21+ |
| **MojMap** | Bundled via Loom | Obfuscation mappings |

---

## Project Layout

```
PhantomMod/
├── src/main/java/com/phantom/
│   ├── PhantomMod.java              ← Fabric entrypoint + client commands
│   ├── module/
│   │   ├── Module.java              ← Abstract base class
│   │   ├── ModuleCategory.java      ← COMBAT, MOVEMENT, PLAYER, RENDER, SMP
│   │   ├── ModuleManager.java       ← Registry + tick/render/keybind dispatch
│   │   └── impl/
│   │       ├── combat/
│   │       │   ├── AimAssist.java   ← Smooth camera aim
│   │       │   ├── AutoClicker.java ← Left click automation
│   │       │   ├── BowAimbot.java  ← Bow projectile aim
│   │       │   ├── BlockHit.java    ← Sword block-hit
│   │       │   ├── Criticals.java   ← Packet-based crits
│   │       │   ├── HitSelect.java   ← Attack timing gate
│   │       │   ├── JumpReset.java   ← Jump reset assist
│   │       │   ├── NoHitDelay.java  ← Cooldown bypass
│   │       │   ├── Reach.java       ← Extended reach
│   │       │   ├── RightClicker.java← Right click auto
│   │       │   ├── SilentAura.java  ← Stealth combat
│   │       │   ├── Triggerbot.java  ← Auto attack on target
│   │       │   ├── Velocity.java    ← Knockback reduction
│   │       │   ├── WaterClutch.java ← Auto bucket swap
│   │       │   ├── WTap.java        ← Sprint reset
│   │       │   └── WeaponCycler.java← Best weapon swap
│   │       ├── movement/
│   │       │   ├── AlwaysSprint.java    ← Sprint enforcement
│   │       │   ├── NoJumpDelay.java     ← Jump cooldown removal
│   │       │   ├── SafeWalk.java        ← Edge protection
│   │       │   ├── Scaffold.java        ← Under-feet placement
│   │       │   └── SpeedBridge.java     ← Bridge + tower assist
│   │       ├── player/
│   │       │   ├── AntiAFK.java     ← Idle prevention
│   │       │   ├── AntiBot.java     ← Bot filtering
│   │       │   ├── AutoTools.java   ← Tool swap
│   │       │   ├── AutoTotem.java   ← Totem equip
│   │       │   ├── AutoXPThrow.java ← XP throwing
│   │       │   ├── FastPlace.java   ← Fast place delay
│   │       │   └── LatencyAlerts.java ← Ping alerts
│   │       ├── render/
│   │       │   ├── BedESP.java      ← Bed highlighting
│   │       │   ├── ESP.java         ← Entity hitboxes
│   │       │   ├── FullBright.java  ← Gamma override
│   │       │   ├── Health.java      ← Health bars
│   │       │   ├── HudModule.java   ← HUD overlay
│   │       │   ├── Indicators.java  ← Target markers
│   │       │   ├── TimeChanger.java ← Time override
│   │       │   └── Trajectories.java← Projectile prediction
│   │       └── smp/
│   │           ├── ChestESP.java    ← Chest highlighting
│   │           ├── OreESP.java      ← Ore highlighting
│   │           ├── OreFinder.java   ← Ore search
│   │           └── ShulkerESP.java  ← Shulker highlighting
│   ├── config/
│   │   ├── ConfigManager.java      ← phantom-memory.properties
│   │   └── ProfileManager.java     ← Profile slots
│   ├── gui/
│   │   ├── ClickGUIScreen.java     ← Main glassy UI
│   │   ├── ModuleSettingsScreen.java← Per-module settings
│   │   ├── ProfileScreen.java      ← Profile management
│   │   ├── NotificationManager.java← Toast notifications
│   │   └── framework/             ← Modern UI widgets
│   ├── mixin/
│   │   ├── ClientPacketListenerMixin.java  ← Velocity packet hook
│   │   ├── MultiPlayerGameModeMixin.java  ← Criticals hook
│   │   ├── LivingEntityJumpDelayMixin.java← NoJumpDelay hook
│   │   ├── ItemInHandRendererMixin.java   ← Reach hook
│   │   ├── EntityRendererMixin.java       ← ESP rendering
│   │   ├── MinecraftClientMixin.java      ← Right-click delay
│   │   ├── LevelMixin.java                ← World hooks
│   │   ├── TitleScreenMixin.java           ← Title screen
│   │   └── MinecraftClientAccessor.java    ← Accessor interface
│   ├── render/
│   │   └── EntityOutlineRender.java← Entity outlines
│   └── util/
│       ├── AnimationUtil.java
│       ├── ESPColor.java
│       ├── InventoryUtil.java
│       └── RenderUtil.java
├── src/main/resources/
│   ├── fabric.mod.json
│   └── phantom.mixins.json
└── build.gradle
```

---

## Architecture

### Entry Point

**`PhantomMod.java`** implements `ClientModInitializer`. It:
1. Creates `ModuleManager` (loads config, registers modules)
2. Registers RIGHT_SHIFT as GUI open key (H for HUD)
3. Hooks into `ClientTickEvents.END_CLIENT_TICK`, `WorldRenderEvents.AFTER_ENTITIES`, and `HudRenderCallback.EVENT`
4. Registers `/bridge` client commands for preset switching

---

### Module System

Base class `Module.java` defines:

| Method | Purpose |
|---|---|
| `onEnable()` | Called when toggled on |
| `onDisable()` | Called when toggled off |
| `onTick()` | Every game tick |
| `onRender(context)` | Every 3D frame |
| `onHudRender(graphics)` | Every 2D HUD frame |
| `loadConfig/saveConfig` | Properties persistence |

**Config key format:** `modulename.enabled`, `modulename.key`

**Team Detection:** Base `Module` provides `isTeammateTarget(Entity)` checking vanilla team alliances and armor colors.

---

### Config System

**`phantom-memory.properties`** in `.minecraft/config/` — flat key=value format.

**`ConfigManager`** provides:
- `load(ModuleManager)` — reads file, calls `module.loadConfig()`
- `save(ModuleManager)` — rebuilds Properties, writes atomically
- `apply(ModuleManager, Properties, boolean)` — applies profile with lifecycle hooks
- `readProfile/writeProfile` — profile slot I/O

**Why Properties?** Zero dependencies, human-readable, crash-safe atomic writes.

---

### Mixin System

All mixins declared in **`phantom.mixins.json`**:

| Mixin | Target | Purpose |
|---|---|---|
| `ClientPacketListenerMixin` | `handleSetEntityMotion` | Velocity scaling at RETURN |
| `MultiPlayerGameModeMixin` | `attack` | Critical hit packet spoofing |
| `LivingEntityJumpDelayMixin` | `tick` | Zero jump cooldown |
| `ItemInHandRendererMixin` | `render` | Reach extension |
| `EntityRendererMixin` | `render` | ESP entity outlines |
| `MinecraftClientMixin` | `tick` | Right-click delay tracking |
| `LevelMixin` | Various | World hooks |
| `TitleScreenMixin` | Various | Title screen hooks |

---

### GUI System

#### `ClickGUIScreen`
Premium liquid glassy aesthetic:
- Sidebar-based category navigation (Combat / Movement / Player / Render / SMP)
- Search box to filter modules
- Module rows with enable toggles and settings access
- `Profiles` button in sidebar

#### `ModuleSettingsScreen`
Dynamically renders widgets using `instanceof` pattern matching:
- Sliders, toggles, preset buttons
- Hotkey binding with Set/Clear

#### Modern UI Framework (`framework/`)
- **`ModernButton`** — Rounded glass-effect buttons
- **`ModernSlider`** — Translucent slider with tooltip
- **`ModernToggle`** — On/off switch
- **`ModernTextField`** — Glass-effect text input

#### `NotificationManager`
Toast notifications with configurable position (TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT).

#### `ProfileScreen`
4 profile slots with editable names, explicit save/load, overwrite confirmation.

---

## Module Reference

### Combat Highlights

#### `AimAssist`
- Scans entities in FOV cone with configurable radius
- Smooth yaw/pitch interpolation with ±1.5° noise for anti-detection
- Target areas: Center/Head/Feet
- Target modes: Yaw/Distance
- Supports Swords, Axes, Maces, Tridents

#### `Velocity`
- Hooks `handleSetEntityMotion` at RETURN
- Scales `deltaMovement` by `kbPercent`
- Subtle at ≥70%, blatant at 0%

#### `SilentAura`
- Uses `gameMode.attack()` directly
- No rotation packets sent
- CPS bounds and radius settings

#### `SpeedBridge`
- Bridge assist with edge detection
- Tower mode (hold jump + right-click while stationary)
- Presets: Legit, Normal, Obvious, Blatant
- HUD indicator showing TOWER/FLAT mode

### Render Highlights

#### `HUD`
- Top-right (or left) corner info display
- Toggleable: module list, FPS, ping, CPS
- Notification position control
- Uses Minecraft uniform font

#### `ESP`
- Team-colored hitbox rendering
- Team detection via vanilla `isAlliedTo()` and armor colors
- Separate toggles for players/mobs/animals
- Through walls toggle

---

## Design Decisions

### Why packet spoofing for Criticals?
Spoofing `ServerboundMovePlayerPacket` with ground-state packets is harder to detect than actual jumping — the server sees position updates, not physical Y movement.

### Why Properties over JSON?
Zero dependencies. Java's built-in Properties handles everything we need with crash-safe atomic writes.

### Why `instanceof` pattern matching in settings?
Scales well for the current module count without needing a provider interface per module.

---

## Build & Installation

### Requirements
- Java 21
- Minecraft 1.21.11 + Fabric Loader 0.16.10+
- Fabric API 0.140.0+1.21.11

### Build
```bash
./gradlew build
```
Output: `build/libs/PhantomMod-*.jar`

### Install
1. Drop jar into `.minecraft/mods/`
2. Drop Fabric API into `.minecraft/mods/`
3. Launch with Fabric profile

### Config Locations
- **Windows:** `%appdata%\.minecraft\config\phantom-memory.properties`
- **macOS:** `~/Library/Application Support/minecraft/config/phantom-memory.properties`
- **Linux:** `~/.minecraft/config/phantom-memory.properties`