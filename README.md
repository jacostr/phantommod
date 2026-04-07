# PhantomMod

PhantomMod is a high-performance, lightweight Fabric client mod for Minecraft Java. It focuses on essential utility features and PvP quality-of-life enhancements without the bloat of larger clients.

## Features

### PvP & Movement
*   **AlwaysSprint**: Eliminates the need to double-tap or hold a sprint key. Automatically keeps you sprinting while moving forward.
*   **SpeedBridge**: A specialized assist that helps with bridging. It automates crouch-un-crouch timing to make bridging faster and safer.
*   **AutoTools**: Intelligently switches your active hotbar slot to the most efficient tool for the block you're looking at, or to your best weapon when targeting entities.

### Visuals & Render
*   **ESP (Extra Sensory Perception)**: Highlights nearby players, mobs, and animals with vibrant boxes that are visible through walls.
*   **FullBright**: Applies a permanent Night Vision effect, ensuring you can see clearly in caves or at night without using torches.
*   **HUD**: A compact, non-intrusive overlay in the top-right corner that displays your active modules, current FPS, and latency (Ping).

### Interface
*   **Click GUI**: A sleek, easy-to-use menu to toggle modules on the fly. Accessible via **Right Shift** by default.
*   **Notifications**: Small on-screen pop-ups that alert you whenever a module is enabled or disabled.

---

## Controls & Configuration

### Keybinds
You can customize all module hotkeys through the **Module Settings** (the "Opt" button in the Click GUI).

| Action | Default Key |
| :--- | :--- |
| **Open Click GUI** | `Right Shift` |
| **AlwaysSprint** | `V` |
| **SpeedBridge** | `X` |
| **ESP** | `Y` |
| **FullBright** | `B` |
| **AutoTools** | `R` |
| **HUD Toggle** | `H` |

### Configuration Saving
PhantomMod automatically saves your settings so you don't have to re-configure them every time you launch the game.

*   **File Name**: `phantom-memory.properties`
*   **Location**: Your Minecraft instance's `config` folder.
    *   *Windows*: `%appdata%\.minecraft\config\phantom-memory.properties`
    *   *macOS*: `~/Library/Application Support/minecraft/config/phantom-memory.properties`
*   **What is saved?**:
    *   **Hotkeys**: Any custom keys you've assigned to modules.
    *   **Enabled States**: If a module was active when you closed the game, it will automatically reactivate on startup.
    *   **Module Settings**: Individual settings like HUD FPS/Ping visibility or ESP filters.

---

## Installation & Building

### Requirements
- Minecraft Java Edition **1.21.11**
- [Fabric Loader](https://fabricmc.net/)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java **21**

### Installation
1. Ensure **Fabric Loader** is installed for Minecraft 1.21.11.
2. Download or build the **PhantomMod** jar.
3. Place the jar and the **Fabric API** jar into your `.minecraft/mods` folder.
4. Launch the game using the Fabric profile.

### Building from Source
If you want to build the mod yourself, use the included Gradle wrapper:
```bash
./gradlew build
```
The resulting jar will be located in `build/libs/`.

---

## License
PhantomMod is open-source. Feel free to modify, fork, or use it as a learning resource. Stay safe and have fun!
