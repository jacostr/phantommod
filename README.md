# PhantomMod

PhantomMod is a high-performance, lightweight Fabric client mod for Minecraft Java. It focuses on essential utility features, Hypixel-tailored PvP modules, and quality-of-life enhancements without the bloat of larger clients.

## Features

### Combat ⚔️
*   **AimAssist**: Smoothly adjusts your camera towards targets while you hold Left Click with a Sword. Bypasses Watchdog rotations.
*   **AutoBlock**: Automatically blocks natively after you hit an entity with a Sword to significantly reduce incoming damage, bypassing 1.8 Watchdog checks.
*   **Criticals**: Forces critical hits natively by spoofing mini-jumps before attacks. Configurable percentage chance to avoid flagging anti-cheats.
*   **Reach**: Extends your attack and interaction range individually.
*   **Velocity**: Intercepts Server Knockback and mathematically scales resistance to avoid 100% canceling flags.

### Movement 🏃
*   **AlwaysSprint**: Eliminates the need to double-tap or hold a sprint key.
*   **NoJumpDelay**: Removes the vanilla 10-tick delay between jumps when holding the jump key.
*   **Scaffold**: Automatically places blocks under you allowing for quick tower or legit bridging.
*   **SpeedBridge**: A specialized assist that automates crouch-un-crouch timing to make bridging faster and safer.

### Player 👤
*   **AutoTools**: Auto switches your active hotbar slot to the most efficient tool for the block you're mining, or to your best weapon when targeting entities.
*   **NoFall**: Spoofs being on the ground to prevent fall damage.

### Render & UI 🎨
*   **Click GUI**: A sleek, easy-to-use menu to toggle modules on the fly. Accessible via **Right Shift** by default. Re-designed with immersive scroll sliders.
*   **ESP**: Highlights nearby players, mobs, and animals with vibrant boxes that are visible through walls.
*   **FullBright**: Applies a permanent Night Vision effect, ensuring you can see clearly without torches.
*   **HUD**: A compact, non-intrusive overlay in the top-right corner that displays your active modules, current FPS, and Ping.

---

## Controls & Configuration

### GUI and Modules
The Click GUI categorizes all modules intuitively across **Combat**, **Movement**, **Player**, and **Render** tabs. Use the settings (≡ icon) next to any module to bind a customized activation hotkey, configure detectability limits, adjust graphical sliders, and change module strengths.

| Action | Default Key |
| :--- | :--- |
| **Open Click GUI** | `Right Shift` |

### Configuration Saving Integration
PhantomMod securely saves and persists all UI layout changes, slider inputs, detectability configuration offsets, and active hotkeys across reboots.

*   **File Name**: `phantom-memory.properties`
*   **Location**: Your Minecraft instance's `config` folder.

---

## Installation & Building

### Requirements
- Minecraft Java Edition **1.21.1 / 1.21.11**
- [Fabric Loader](https://fabricmc.net/)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java **21**

### Building from Source
If you want to build the mod yourself, use the included Gradle wrapper:
```bash
./gradlew build
```
The resulting jar will be located in `build/libs/`.

---

## License
PhantomMod is open-source. Feel free to modify, fork, or use it as a learning resource. Stay safe and have fun!
