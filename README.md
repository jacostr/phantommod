# PhantomMod

PhantomMod is a lightweight Fabric client mod for Minecraft Java focused on simple utility and PvP-style quality-of-life features.

## Features

- `AlwaysSprint`  
  Keeps sprinting active while moving.

- `SpeedBridge`  
  Manual bridge assist that helps with crouch timing while you bridge.

- `AutoTools`  
  Switches to the best hotbar tool for blocks and prefers a weapon when hitting entities.

- `ESP`  
  Highlights players, mobs, and animals with simple ESP visuals.

- `FullBright`  
  Improves visibility using a fullbright-style effect.

- `HUD`  
  Shows a small on-screen FPS and ping display.

- `Click GUI`  
  Open the in-game GUI to toggle modules and change supported settings.

- `Notifications`  
  Shows small pop-up notifications when modules are enabled or disabled.

## Controls

Default keybinds:

- `Right Shift` - Open Click GUI
- `V` - Toggle AlwaysSprint
- `X` - Toggle SpeedBridge
- `Y` - Toggle ESP
- `B` - Toggle FullBright
- `R` - Toggle AutoTools
- `H` - Toggle HUD

## Requirements

- Minecraft Java Edition `1.21.11`
- Fabric Loader
- Fabric API
- Java `21`

## Building

Build the mod with:

```bash
./gradlew build
```

The built jar will be in:

```text
build/libs/
```

Use the normal mod jar in your `mods` folder, not the `-sources.jar`.

## Installation

1. Install Fabric Loader for Minecraft `1.21.11`
2. Install Fabric API
3. Build the project or use a built jar
4. Put the main jar file into your Minecraft `mods` folder
5. Launch the game with the Fabric profile

## Notes

This project is intended as a lightweight client mod with a light UI and a small set of utility-focused modules.

## License

All opensorce, free to make changes.
