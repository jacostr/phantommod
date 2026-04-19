# PhantomMod v0.6.0 — Technical Reference

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
   - [Combat](#combat)
   - [Movement](#movement)
   - [Player](#player)
   - [Visuals In Player](#visuals-in-player)
   - [SMP](#smp)
6. [Design Decisions & Bypass Reasoning](#design-decisions--bypass-reasoning)
7. [Build & Installation](#build--installation)

---

## Overview & Goals

PhantomMod is a self-contained Fabric client mod. The primary goals are:
- **Minimal footprint** — no third-party libraries beyond Fabric API. Pure vanilla mappings.
- **Configurable detectability** — every automation module exposes sliders and presets so the user can tune risk vs. strength.

---

## Technology Stack

| Technology | Version | Why |
|---|---|---|
| **Minecraft Java** | 1.21.11 | Target version |
| **Fabric Loader** | Latest stable | Mod loader; lightweight, fast startup |
| **Fabric API** | Latest stable | Event hooks (tick, HUD render, world render) |
| **Fabric Loom** | 1.14.10 | Gradle build plugin; remapping + mixin processing |
| **SpongePowered Mixin** | Bundled | Bytecode injection into vanilla classes |
| **Java** | 21 | Required by Minecraft 1.21+ |
| **MojMap** | Bundled via Loom | Official Mojang obfuscation mappings |

**Why Fabric over Forge?**  
Fabric loads faster, has a smaller API surface, and mixins integrate more cleanly. Forge's event system adds overhead and is harder to keep bypass-safe since Forge itself modifies more vanilla code.

**Why MojMap/Loom?**  
Loom automatically remaps everything from obfuscated bytecode to readable MojMap names at build time. This means code like `mc.player.getDeltaMovement()` compiles directly without any manual obfuscation lookups.

---

## Project Layout

```
PhantomMod/
├── src/main/java/com/phantom/
│   ├── PhantomMod.java              ← Fabric mod entrypoint
│   ├── config/
│   │   ├── ConfigManager.java       ← Reads/writes phantom-memory.properties
│   │   └── ProfileManager.java      ← Manages named profile slots
│   ├── gui/
│   │   ├── ClickGUIScreen.java      ← Main module toggle overlay (M)
│   │   ├── ModuleSettingsScreen.java← Per-module settings with sliders/buttons
│   │   ├── NotificationManager.java ← Toast-style on-screen popups
│   │   ├── ProfileScreen.java       ← Save/load UI for 4 profile slots
│   │   └── widget/
│   │       └── PhantomSlider.java   ← Reusable slider widget
│   ├── mixin/
│   │   ├── ClientPacketListenerMixin.java   ← Hooks velocity packets (Velocity module)
│   │   ├── LivingEntityJumpAccessor.java    ← Accessor for private jump timer field
│   │   ├── LivingEntityJumpDelayMixin.java  ← Removes jump cooldown (NoJumpDelay)
│   │   └── MultiPlayerGameModeMixin.java    ← Attack hook (Criticals module)
│   ├── module/
│   │   ├── Module.java              ← Abstract base class for every module
│   │   ├── ModuleCategory.java      ← Enum: COMBAT, MOVEMENT, PLAYER, SMP
│   │   ├── ModuleManager.java       ← Registry; dispatches tick/render/keybind events
│   │   └── impl/
│   │       ├── combat/
│   │       │   ├── AimAssist.java   ← Smooth camera aim toward targets
│   │       │   ├── AutoBlock.java   ← Auto sword-block on hit
│   │       │   ├── AutoClicker.java ← Left click automation with presets
│   │       │   ├── BlockHit.java    ← Visual/hold-use block-hit behavior
│   │       │   ├── Criticals.java   ← Spoofed mini-jump critical hits
│   │       │   ├── HitSelect.java   ← Attack timing / selective hit gating
│   │       │   ├── JumpReset.java   ← Jump reset assist after taking hits
│   │       │   ├── NoHitDelay.java  ← Attack cooldown removal/reduction
│   │       │   ├── Reach.java       ← Extended entity/block reach
│   │       │   ├── RightClicker.java← Right click automation
│   │       │   ├── SilentAura.java  ← Stealth combat module without rotations
│   │       │   ├── Triggerbot.java  ← Auto attack when crosshair is on target
│   │       │   ├── Velocity.java    ← Knockback percentage reduction
│   │       │   └── WTap.java        ← Sprint reset on attack
│   │       ├── movement/
│   │       │   ├── AlwaysSprint.java    ← Sprint state enforcement
│   │       │   ├── NoJumpDelay.java     ← Jump cooldown removal
│   │       │   ├── SafeWalk.java        ← Edge protection while walking
│   │       │   ├── Scaffold.java        ← Under-feet block placement
│   │       │   └── SpeedBridge.java     ← Edge-detection bridging assist
│   │       ├── player/
│   │       │   ├── AntiAFK.java     ← Idle movement / input prevention
│   │       │   ├── AntiBot.java     ← Client-side bot filtering helper
│   │       │   ├── AutoTools.java   ← Auto tool/weapon swap
│   │       │   ├── AutoTotem.java   ← Auto offhand totem equip
│   │       │   ├── FastPlace.java   ← Reduced right-click place delay
│   │       │   ├── Freecam.java     ← Camera detach free-flying
│   │       │   └── NoFall.java      ← Fall damage prevention
│   │       ├── render/
│   │       │   ├── Arrows.java      ← Directional arrow indicators
│   │       │   ├── ESP.java         ← Entity ESP overlay
│   │       │   ├── FullBright.java  ← Gamma override for night vision
│   │       │   ├── HudModule.java   ← Corner info overlay
│   │       │   ├── Indicators.java  ← On-screen target / state indicators
│   │       │   ├── LatencyAlerts.java ← Ping spike notifications
│   │       │   └── Health.java      ← Entity health indicators (scalable bars)
│   │       └── smp/
│   │           ├── AutoXPThrow.java ← Fast XP bottle usage helper
│   │           ├── BedESP.java      ← Bed block ESP
│   │           ├── ChestESP.java    ← Chest block ESP
│   │           ├── OreESP.java      ← Ore block ESP
│   │           ├── OreFinder.java   ← Ore search helper
│   │           └── ShulkerESP.java  ← Shulker box ESP
├── src/main/resources/
│   ├── fabric.mod.json              ← Mod metadata, entrypoint declaration
│   └── phantom.mixins.json          ← Mixin registration file
└── build.gradle                     ← Loom + dependency configuration
```

---

## Architecture

### Entry Point

**`PhantomMod.java`** implements `ClientModInitializer` — Fabric's client-side lifecycle hook. This runs once when the game finishes loading.

**What it does:**
1. Creates the `ModuleManager` (which loads config and registers all modules)
2. Registers `M` as the GUI open key
3. Hooks into three Fabric API events:
   - `ClientTickEvents.END_CLIENT_TICK` — runs every game tick for module logic
   - `WorldRenderEvents.AFTER_ENTITIES` — runs in the 3D render cycle (used by ESP)
   - `HudRenderCallback.EVENT` — runs every frame on the 2D HUD layer (overlays, notifications)

**Why `END_CLIENT_TICK` instead of `START`?**  
By the end of the tick, input state (key presses, mouse clicks, entity interactions) has all been processed. Reading it at END means module logic always sees the final, stable state for that tick.

---

### Module System

Every feature is a `Module` subclass. The base class `Module.java` defines:

| Method | Purpose |
|---|---|
| `onEnable()` | Called once when the module is toggled on |
| `onDisable()` | Called once when toggled off; used to undo side effects |
| `onTick()` | Called every game tick while enabled |
| `onRender(context)` | Called every 3D frame while enabled |
| `onHudRender(graphics)` | Called every 2D HUD frame while enabled |
| `loadConfig(Properties)` | Reads saved values from the properties file |
| `saveConfig(Properties)` | Writes current values to the properties file |

**Config key format:**  
Module names are normalized to lowercase snake_case (e.g. `"AimAssist"` → `"aimassist"`). Each property is prefixed: `aimassist.key`, `aimassist.enabled`.

**Enable/disable flow:**  
`toggle()` → `setEnabled(bool)` → calls `onEnable()`/`onDisable()` + fires a `NotificationManager` toast + auto-saves config.

**`ModuleCategory`** is a simple enum (`COMBAT`, `MOVEMENT`, `PLAYER`, `SMP`) that controls which tab a module appears under in the ClickGUI.

**`ModuleManager`** is the registry. It:
- Constructs all module instances at startup
- Sorts them alphabetically within each category
- Delegates tick/render/keybind calls to all enabled modules
- Provides `getModuleByName(String)` for mixin lookups

---

### Config System

**`phantom-memory.properties`** is a standard Java `Properties` file stored in `.minecraft/config/`. It holds every module's enabled state, hotkey, and custom slider values in flat key=value format.

**ProfileManager** manages 4 custom profiles (`slot_0.properties` through `slot_3.properties` in `config/phantom-profiles/`) with user-editable names persisted in `phantom-profile-names.properties`.

**`ConfigManager`** has two static methods:
- `load(ModuleManager)` — reads the file and calls `module.loadConfig()` for each module
- `save(ModuleManager)` — collects all values, then writes the file atomically

**Why Properties instead of JSON?**  
Properties files are human-readable, easy to hand-edit, and require zero external libraries. JSON would need Gson or an equivalent, adding a dependency.

---

### Mixin System

Mixins use SpongePowered Mixin (bundled with Fabric) to inject code directly into compiled vanilla Minecraft classes — without modifying source. Loom processes the mixin JSON and applies bytecode transformations at launch.

All mixins are declared in **`phantom.mixins.json`**.

#### `ClientPacketListenerMixin`
- **Target:** `ClientPacketListener.handleSetEntityMotion`
- **Injection point:** `@At("RETURN")` — after the vanilla code has already applied the packet's motion to the player
- **What it does:** Reads the Velocity module's `kbPercent` slider and scales the player's `deltaMovement` vector by that fraction
- **Why RETURN not HEAD?** If we cancelled/intercepted at HEAD, the vanilla code wouldn't run at all — the player would receive a "ghost" velocity that servers might detect as suspicious. By hooking at RETURN, the server-intended velocity is applied first, then we scale it back. This looks like normal physics lag to the server.

#### `MultiPlayerGameModeMixin`
- **Target:** `MultiPlayerGameMode.attack`
- **Injection point:** `@At("HEAD")` — before the attack packet is sent
- **What it does:** Sends 4 `ServerboundMovePlayerPacket.Pos` packets that simulate a tiny bobbing motion, making the server register the next hit as a critical (player was briefly airborne)
- **Why packet spoofing?** Critical hits in vanilla require the player to be falling. Rather than literally making the player jump (visible, predictable), we spoof the positional state that the server uses to determine "was the player airborne during this attack"

#### `LivingEntityJumpDelayMixin` + `LivingEntityJumpAccessor`
- **Target:** `LivingEntity` jump timer field
- The Accessor interface exposes the private `jumpDelay` field through a generated getter
- The Mixin then zeroes it every tick when NoJumpDelay is enabled

---

### GUI System

#### `ClickGUIScreen`
The main overlay opened with M. Renders:
- Category tabs (Combat / Movement / Player / SMP) at the top
- A dedicated `HUD Settings` button in the bottom-right corner
- A search box to quickly filter modules by name
- Module rows with enable/disable buttons and a `≡` hamburger icon to open settings

Scroll is handled via `mouseScrolled` tracking an `int scrollOffset` clamped to `maxScroll`. All widget Y positions are offset by `-scrollOffset` in `rebuildUI()`.

#### `ModuleSettingsScreen`
Opened when clicking the `≡` icon on any module. Dynamically renders:
- "How to use" text from `module.getUsageGuide()`
- Module-specific widgets (sliders, toggle buttons) using `instanceof` pattern matching
- Hotkey binding row with `Set hotkey` / `Clear Hotkey`

Modules without configurable options, such as the simplified `Scaffold`, do not expose a settings screen in the ClickGUI.

**Why instanceof pattern matching?**  
It avoids needing a separate `SettingsProvider` interface or abstract factory per module. The screen knows about each module type and renders widgets accordingly. This is simpler for a small project — if modules grew to 50+, a provider pattern would scale better.

#### `PhantomSlider`
Extends `AbstractSliderButton` (vanilla widget). Maps a 0.0–1.0 internal slider value to a real double range (`min` to `max`). Displays integers cleanly (no `.00` for whole numbers).

#### `NotificationManager`
A static list of `Notification` records (message + expiry timestamp). `render()` is called from both the HUD render hook and over GUI screens to ensure toasts appear regardless of what's open.

---

## Module Reference

### Combat

#### `AimAssist`
- **How it works:** Every tick, scans all living entities within a configurable FOV cone and 5-block radius. Picks the closest by yaw angle. Smoothly interpolates player yaw/pitch toward the target's chest area using a fraction of the total angular delta (`delta / speedFactor`). Adds ±1.5° noise to defeat rotation-pattern detectors.
- **Trigger condition:** Player must be holding a Sword **and** holding Left Click (attack key)
- **Detectability:** Subtle — smooth movement looks human. Patterns emerge at high speed values.
- **Settings:** `Smoothing` (1–10, higher = slower), `FOV Limit` (10°–360°)

#### `AutoBlock`
- **How it works:** After every sword hit, virtually holds the `keyUse` (right-click) key for a configurable duration. On Hypixel, holding right-click with a sword triggers the 1.8-style "block-hitting" animation even in 1.21.
- **Trigger condition:** Player holds a sword in main hand and hits something
- **Detectability:** Blatant — blocking pattern is very consistent. Strength slider reduces hold duration to make it intermittent.
- **Settings:** `Strength` slider (0–100), preset buttons (Legit, Normal, Obvious)

#### `Criticals`
- **How it works:** Hooks `MultiPlayerGameMode.attack` via mixin. Before the attack packet is sent, fires 4 movement packets: `y+0.0625`, `y`, `y+0.0125`, `y` — a tiny simulated bounce the server interprets as airborne state, registering the hit as a critical.
- **Trigger condition:** Player attacks any entity
- **Detectability:** Blatant — the server sees positional packets immediately before every attack. The `Chance %` slider randomizes whether crits fire, making the pattern less consistent.
- **Settings:** `Crit Chance` (0.0–1.0)

#### `NoHitDelay`
- **How it works:** Uses `player.resetAttackStrengthTicker()` to remove the 1.9+ attack cooldown, allowing 1.8-style spam clicking.
- **Detectability:** Blatant — attack cadence patterns matching 1.8 on a 1.9+ server are easily flagged.
- **Settings:** Configurable hit chance (0% to 100%), tick delay override, and server-specific presets (Vanilla/Hypixel/Mineplex/Blatant).

#### `SilentAura`
- **How it works:** Replaces normal Aura by keeping the client camera completely stationary. Targets entities using `gameMode.attack()` directly. No rotation packets are sent making it much harder to detect visually or heuristically.
- **Detectability:** Blatant on strict configurations — attacking entities outside FOV or without moving look direction can be flagged.
- **Settings:** Attack CPS bounds, attack radius, target prioritizing (Yaw/Distance/Health), and entity filtering options.

#### `Reach`
- **How it works:** Uses Minecraft's built-in `Attributes` system (`ENTITY_INTERACTION_RANGE`, `BLOCK_INTERACTION_RANGE`) with `AttributeModifier` UUIDs that are added on enable and removed on disable.
- **Detectability:** Blatant — servers log hit distances. Values above ~3.5 blocks are flagged immediately on Hypixel.
- **Settings:** `Entity reach` slider (3.0–8.0), `Block reach` slider (4.5–10.0), presets (Legit, Normal, Obvious, Blatant)

#### `Velocity`
- **How it works:** Hooks `ClientPacketListener.handleSetEntityMotion` via mixin at `RETURN`. After vanilla applies the knockback vector, scales XYZ delta movement by `kbPercent`. At 90% the player still visibly flinches but absorbs significantly less distance.
- **Detectability:** Subtle at ≥70% — the client still receives and somewhat applies knockback. At 0% (None preset), the server sees motion applied that the client nullifies — detectable by acceleration analysis.
- **Settings:** `Knockback %` slider (0.0–1.0), presets (Legit 90%, Subtle 75%, Blatant 40%, None 0%)

---

### Movement

#### `AlwaysSprint`
- **How it works:** Sets `mc.player.setSprinting(true)` every tick if the player is moving forward and sprinting is normally allowed (not too hungry, not in liquid, etc.).
- **Detectability:** Safe — vanilla sprint behavior, just automated.

#### `NoJumpDelay`
- **How it works:** Uses a Mixin + Accessor pair to zero out `LivingEntity.jumpDelay` (the 10-tick cooldown that prevents bunny-hopping). The accessor exposes the private int field for writing.
- **Detectability:** Subtle — noticeable if the server measures jump frequency over time.

#### `Scaffold`
- **How it works:** Detects when the block below the player's feet is air, finds a hotbar block, switches to it with the synced inventory helper, places against a neighboring face, then switches back.
- **Detectability:** Blatant — automated under-feet placement is still easy for anti-cheat to pattern.
- **Settings:** None. `Scaffold` is intentionally kept as a plain regular scaffold in `v1.0.6`.

#### `SpeedBridge`
- **How it works:** Captures a bridge direction yaw on enable. Every tick, computes two vectors: backwards (away from bridge edge) and lateral. Checks if a block exists one step behind the player's feet. If the player is hanging over air, virtually holds `keyShift` (sneak) to prevent falling. When the block stack empties, `findNextBlockSlot()` scans the hotbar for the next `BlockItem` and calls `setSelectedSlot()`.
- **Block refill logic:** Scans slots `(current+1) % 9` through `(current+8) % 9` — always picks the next adjacent slot first (lowest rotation distance), wrapping around.
- **Detectability:** Safe/Subtle — sneak timing is the only automated action; placement is still fully manual.

---

### Player

#### `AutoTools`
- **How it works:** Reads `mc.hitResult` each tick. If it's a `BlockHitResult`, compares `stack.getDestroySpeed(blockState)` across all hotbar slots and calls `setSelectedSlot()` on the best one. For `EntityHitResult`, scores by item name (sword > mace > axe > trident).
- **Detectability:** Safe — tool swapping is a normal player action.

#### `NoFall`
- **How it works:** Sends `ServerboundMovePlayerPacket` with `onGround = true` whenever the player has significant downward velocity, preventing the server from calculating fall damage.
- **Detectability:** Blatant — NCP and Watchdog both track ground state vs. expected trajectory.

---

### Visuals In Player

#### `ESP`
- **How it works:** In `onRender()` (3D world render pass after entities are drawn), iterates nearby highlightable entities and renders wireframe boxes. Targets use a dedicated no-depth line render type so the boxes stay visible through walls.
- **Detectability:** Safe — purely client-side visual; the server never sees it.
- **Settings:** Toggle players / mobs / animals independently, and `Through Walls`.

#### `Health`
- **How it works:** Renders vertical health indicators beside entities during the 3D world render pass. Uses billboarded quads (or thick lines) that face the camera. The scale can be adjusted via a slider to control the physical width of the bar.
- **Detectability:** Safe — purely client-side visual.
- **Settings:** `Through Walls`, `Show Invisible`, and `Bar Scale` slider (0.5–10.0).

#### `Arrows`
- **How it works:** Renders directional arrow indicators pointing toward nearby entities or projectiles, with configurable radius and distance display.
- **Detectability:** Safe — purely client-side visual.

#### `LatencyAlerts`
- **How it works:** Monitors ping from `mc.getConnection()` and fires toast notifications when ping exceeds a threshold or spikes.
- **Detectability:** Safe — client-side only.

#### `Freecam`
- **How it works:** On enable, saves the player's position and freezes movement input. Polls `KeyMapping.isDown()` on WASD/Jump/Shift to move a virtual camera position each tick. The player body stays frozen at the original position.
- **Detectability:** Safe — the server doesn't receive movement packets while the player is "frozen".

#### `FullBright`
- **How it works:** Saves `mc.options.gamma` on enable, sets it to `16.0` (well above the normal max of 1.0, which forces full ambient light rendering), restores on disable.
- **Detectability:** Safe — gamma is a client-only graphics option.

#### `HudModule`
- **How it works:** In `onHudRender()`, draws a sorted list of currently-enabled module names in the top-right corner. Optionally draws FPS, current Ping from `mc.getConnection()`, and CPS.
- **Detectability:** Safe — HUD is client-side only.

#### `ProfileScreen`
- **How it works:** Manages 4 saved config slots with editable names, explicit save/load actions, and overwrite confirmation before replacing an existing slot.
- **Detectability:** Safe — client-side GUI only.

---

### SMP

#### Block ESPs (`ChestESP`, `OreESP`, `BedESP`, `ShulkerESP`)
- **How it works:** Separated from entity ESP into survival-specific categories. Iterates chunks based on range using varying timers (e.g. 1-second delay for block iteration to avoid lag) and renders wireframes.
- **Detectability:** Safe — client-side rendering.

#### `AutoXPThrow`
- **How it works:** Bypasses normal right-click delay by aggressively interacting via packet/controller logic while XP bottles are held. Switch speed handles auto-inventory cycling.
- **Detectability:** Moderate — rapid slot interaction can generate irregular inventory packets.

---

## Design Decisions & Bypass Reasoning

### Why not cancel knockback packets entirely?
Cancelling `ClientboundSetEntityMotionPacket` at HEAD makes the server think the client received velocity but the client reports zero movement to NCP checks. Scaling at RETURN means the client *does* receive velocity and the reduced movement is plausible as normal physics.

### Why add noise to AimAssist rotations?
Watchdog and NCP both score rotation streams for inhuman consistency. If every tick snaps exactly toward the target's center, the variance is 0.0 — impossible for a human. Adding ±1.5° per tick from `Math.random()` gives a realistic jitter distribution.

### Why use `ServerboundMovePlayerPacket` for Criticals instead of actually jumping?
A real jump changes the player's Y position visibly and is affected by momentum. It also sends a full movement update that must match subsequent packets. Spoof-packets only affect server state for one tick and don't require the client to actually be in the air — much harder to correlate.

### Why `setSelectedSlot()` instead of sending `ServerboundSetCarriedItemPacket` for slot swaps?
`setSelectedSlot()` updates both the client inventory and sends the packet automatically. Manually sending the packet while mismatching client state can cause desync.

### Why Properties file instead of JSON for config?
Zero external dependencies. Java's built-in `Properties` gives us `load()`, `store()`, and comment support out of the box. The file is easily hand-editable and survives crashes (we write atomically using `Files.newOutputStream`).

---

## Build & Installation

### Requirements
- Java 21
- Minecraft Java 1.21.11 with Fabric Loader installed
- Fabric API in your mods folder

### Build
```bash
./gradlew build
```
Output jar: `build/libs/PhantomMod-*.jar`

### Install
1. Drop the jar into `.minecraft/mods/`
2. Drop Fabric API jar into `.minecraft/mods/`
3. Launch with the Fabric profile

### Config file location
- **Windows:** `%appdata%\.minecraft\config\phantom-memory.properties`
- **macOS:** `~/Library/Application Support/minecraft/config/phantom-memory.properties`
- **Linux:** `~/.minecraft/config/phantom-memory.properties`
