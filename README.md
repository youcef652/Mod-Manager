# Mod Manager

Client-side Fabric mod for Minecraft Java Edition 1.21.11.

This project adds a lightweight mod manager UI to the title screen so you can search, install, update, and manage mods, resource packs, shader packs, and data packs directly from the game.

## Features

- Search and download content from Modrinth and CurseForge
- Separate handling for different content types:
  - Mods
  - Resource packs
  - Shader packs
  - Data packs
- View installed files and manage them from inside the game
- Check for updates on installed mods
- Restart Minecraft from the mod UI after changes
- Mobile-friendly interface designed for smaller screens

## Requirements

- Minecraft Java Edition 1.21.11
- Fabric Loader 0.19.5
- Java 21
- Fabric API for 1.21.11

## Build the mod

From the project root, run:

```bash
./gradlew build
```

The generated mod jar will be created here:

```text
build/libs/modmanager-1.0.0.jar
```

## Install

1. Make sure Minecraft is running Fabric for version 1.21.11.
2. Copy the jar file into your Minecraft mods folder.
3. Start the game and open the Title Screen.
4. Click the Mod Manager button in the top-left area of the screen.

Typical install location:

```text
.minecraft/mods/
```

## Usage

After launching the game:

1. Open the Mod Manager from the title screen.
2. Choose a content type from the menu.
3. Search for a mod or pack.
4. Select a result and download it.
5. Use the manager screens to update, rename, or remove installed content.
6. If needed, use the Restart Minecraft option after installing or changing files.

## CurseForge API key

CurseForge support may require an API key.

- You can add it in the mod settings screen.
- You can also set it as an environment variable named:

```bash
CURSEFORGE_API_KEY
```

If no valid key is present, CurseForge requests may fail or return authorization errors.

## Notes

- This is a client-side mod intended for use with Fabric.
- The interface is optimized for mobile and smaller screens.
- For best results, test on a desktop environment for full runtime validation, since mobile/Android-based launchers may have platform-specific limitations.

## License

This project is provided as-is for personal and modding use. Add your preferred license before publishing or distributing it publicly.

## Repository

- GitHub: https://github.com/youcef652/Mod-Manager

## Quick start

```bash
git clone https://github.com/youcef652/Mod-Manager.git
cd Mod-Manager
./gradlew build
```

Then place the jar from `build/libs/` into your Minecraft mods folder and launch the game.
