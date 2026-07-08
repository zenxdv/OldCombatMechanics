# Old Combat Mechanics

**Old Combat Mechanics** is a configurable Minecraft PvP plugin designed to bring 1.7/1.8-style combat behavior to modern Minecraft servers.

It is built for server owners who want to run modern, secure Minecraft versions while keeping faster, classic-style PvP mechanics for practice servers, BedWars, minigames, PvP arenas, and network-wide combat servers.

> **Beta notice:** This plugin is currently in public testing. It builds, loads, and runs on modern Paper, but it has not yet been fully tested across every possible PvP setup, anticheat, minigame plugin, protection plugin, or server fork.

---

## Features

* Classic-style fast melee combat
* Attack cooldown reduction through public Bukkit/Spigot APIs
* Configurable melee damage handling
* Optional cooldown-damage normalization
* Configurable knockback
* Sprint-hit knockback multiplier
* Optional sprint reset behavior
* Optional sweep-attack cancellation
* Fishing rod PvP pull behavior
* Projectile damage and knockback adjustments
* Optional PvE support
* Player-only combat by default
* Global settings
* World enable and disable lists
* Per-world overrides
* Weapon-specific overrides
* Projectile-specific controls
* Entity-category controls for PvE
* Configurable messages
* Useful reload and status commands
* Config validation with console warnings
* No NMS
* No CraftBukkit internals
* No packet hacks
* No version-specific server code

---

## Supported Versions

Old Combat Mechanics is designed for:

| Platform | Support                     |
| -------- | --------------------------- |
| Paper    | Minecraft 26.1 through 26.2 |
| Spigot   | Minecraft 26.1 through 26.2 |

The plugin is built against the shared Bukkit/Spigot API surface so it can run on both Spigot and Paper.

Paper-specific APIs are intentionally avoided so the core plugin stays portable and stable.

---

## Java Requirement

This plugin targets modern Minecraft server versions and requires:

```text
Java 25+
```

Use the same Java version required by your Minecraft server software.

---

## Current Testing Status

Old Combat Mechanics is currently released as a **public beta**.

The plugin has been verified to:

* Build successfully
* Load on Paper 26.1.2
* Register commands
* Load and cache configuration
* Apply startup and reload logic
* Run without NMS, internals, or packet-level hooks

However, combat plugins depend heavily on real-world testing. Knockback feel, plugin compatibility, anticheat behavior, and minigame interaction may vary depending on your server setup.

Please report issues if you find bugs, strange PvP behavior, or compatibility problems.

Helpful reports include:

* Server software and version
* Old Combat Mechanics version
* Other combat, anticheat, minigame, or protection plugins installed
* Your relevant config settings
* Steps to reproduce the issue
* Console errors, if any
* A short video or clip for knockback or PvP-feel issues

---

## Important Limitations

Old Combat Mechanics does **not** claim to perfectly recreate Minecraft 1.7 or 1.8 combat internals.

Modern Minecraft combat is different at the server and client level, so some classic mechanics are approximated using stable Bukkit/Spigot APIs.

Approximated mechanics include:

* Exact 1.7/1.8 hit timing
* Exact client swing animation behavior
* Exact vanilla 1.8 knockback math
* Exact sprint-reset behavior
* Some shield and modern combat edge cases
* Client-side attack indicator behavior

The goal is to provide a stable, configurable, old-style PvP experience on modern servers without relying on fragile internals.

---

## Installation

1. Download the plugin JAR.
2. Stop your server.
3. Place the JAR into your server’s `plugins` folder.
4. Start the server.
5. Edit the generated config file:

```text
plugins/OldCombatMechanics/config.yml
```

6. Reload the plugin:

```text
/oldcombat reload
```

or restart the server.

---

## Commands

| Command                    | Description                          |
| -------------------------- | ------------------------------------ |
| `/oldcombat`               | Shows plugin help                    |
| `/oldcombat reload`        | Reloads the plugin configuration     |
| `/oldcombat status`        | Shows global plugin status           |
| `/oldcombat world <world>` | Shows effective settings for a world |
| `/ocm`                     | Alias for `/oldcombat`               |

---

## Permissions

| Permission         | Description                                               |
| ------------------ | --------------------------------------------------------- |
| `oldcombat.admin`  | Grants access to all admin commands                       |
| `oldcombat.reload` | Allows `/oldcombat reload`                                |
| `oldcombat.status` | Allows `/oldcombat status` and `/oldcombat world <world>` |

Server operators have these permissions by default.

---

## Default Behavior

By default, Old Combat Mechanics focuses on **player-vs-player combat**.

PvE is disabled by default so the plugin does not unexpectedly change survival, mob farms, villagers, bosses, or adventure gameplay.

Default behavior:

```yaml
scope:
  player-only: true
  pve-enabled: false
```

This is recommended for PvP networks, practice servers, BedWars servers, and minigame servers.

---

## World Control

The plugin supports global world control.

You can disable specific worlds:

```yaml
world-control:
  enabled-worlds: []
  disabled-worlds:
    - world_nether
    - world_the_end
```

Or only enable specific worlds:

```yaml
world-control:
  enabled-worlds:
    - pvp
    - bedwars
  disabled-worlds: []
```

Do not hardcode world behavior in the plugin. Use the config to control where combat changes apply.

---

## Per-World Overrides

Worlds inherit global settings unless overridden.

Example:

```yaml
worlds:
  practice:
    melee:
      knockback:
        horizontal: 0.42
        vertical: 0.36

  bedwars:
    rods:
      enabled: true
    melee:
      sprint-knockback-multiplier: 1.20
```

This lets one plugin support different combat tuning across a network.

---

## Weapon Overrides

Weapon-specific settings can be used to tune certain items differently.

Example:

```yaml
weapons:
  DIAMOND_SWORD:
    damage:
      multiplier: 1.0
      flat-bonus: 0.0
    knockback:
      horizontal: 0.38
      vertical: 0.36

  STICK:
    damage:
      multiplier: 0.0
      flat-bonus: 0.0
    knockback:
      horizontal: 0.65
      vertical: 0.35
```

Invalid materials are ignored with a console warning.

---

## PvE Support

PvE support is optional and disabled by default.

To enable PvE:

```yaml
scope:
  player-only: false
  pve-enabled: true
  pve-entity-categories:
    - MONSTER
    - ANIMAL
    - VILLAGER
    - GOLEM
    - OTHER
```

For PvP-focused networks, it is usually better to leave PvE disabled unless you specifically want old-style combat mechanics against mobs.

---

## Testing the Plugin

Recommended test setup:

1. Use a clean Paper or Spigot test server.
2. Install only Old Combat Mechanics first.
3. Join with two players.
4. Put both players in Survival mode.
5. Test melee, sprint hits, rods, bows, reloads, and world settings.
6. Then test with your actual minigame, anticheat, and protection plugins.

Do not test combat behavior in Creative mode. Creative mode can hide normal damage and combat behavior.

Useful commands:

```text
/gamemode survival <player>
/oldcombat status
/oldcombat world world
/oldcombat reload
```

---

## Compatibility Notes

Old Combat Mechanics is designed to respect other plugins.

It avoids changing combat when another plugin has already cancelled the relevant event.

This is important for:

* Spawn protection
* WorldGuard-style regions
* BedWars plugins
* Practice plugins
* Minigame plugins
* Anticheats
* Lobby protection systems

Even so, every server stack is different. Test carefully before using the plugin on a production network.

---

## Building from Source

Requirements:

* Java 25+
* Gradle wrapper included in the project
* IntelliJ IDEA or another Java IDE

Build with:

```bash
./gradlew clean test build
```

On Windows PowerShell:

```powershell
.\gradlew.bat clean test build
```

The built JAR will be located at:

```text
build/libs/
```

---

## Project Structure

```text
OldCombatMechanics/
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
├── CHANGELOG.md
├── LICENSE
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── net/
│   │   │       └── mosspad/
│   │   │           └── oldcombatmechanics/
│   │   └── resources/
│   │       ├── plugin.yml
│   │       └── config.yml
│   └── test/
│       └── java/
│           └── net/
│               └── mosspad/
│                   └── oldcombatmechanics/
```

---

## Development Goals

Old Combat Mechanics is meant to stay focused.

It is not intended to become a giant all-in-one hub, minigame, or server-core plugin.

The goal is:

* Reliable combat mechanics
* Clean configuration
* Safe public APIs
* Good defaults
* Easy maintenance
* Clear limitations
* Compatibility with modern secure servers

---

## Bug Reports

Please report bugs through the GitHub Issues tab or the SpigotMC discussion page.

Include as much information as possible:

```text
Server software:
Server version:
Plugin version:
Other plugins:
Relevant config:
What happened:
What you expected:
Steps to reproduce:
Console errors:
```

For knockback or PvP-feel issues, a short video is extremely helpful.

---

## Planned Improvements

Potential future improvements may include:

* More default config presets
* Better command output formatting
* More weapon-specific examples
* More projectile tuning options
* More compatibility testing with popular PvP plugins
* More real-player PvP balance testing

Suggestions are welcome.

---

## License

This project is released under the MIT License.

See the `LICENSE` file for details.

---

## Disclaimer

Old Combat Mechanics is not affiliated with Mojang, Microsoft, SpigotMC, PaperMC, or any other Minecraft server software project.

Minecraft is a trademark of Microsoft Corporation.
