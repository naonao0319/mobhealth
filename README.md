# MobHealth

Minecraft (Paper 1.21.1) plugin that displays mob health and damage indicators using Text Displays.
Compatible with ProtocolLib.

## Features

- **HP Bar Display**: Shows the mob's current HP above their head when damaged.
- **Damage Indicators**: Displays damage values floating from the mob's location.
- **Configurable**: Customize messages, display formats, view distance, and blacklisted mobs.
- **Optimized**: Uses packet-based Text Displays (ProtocolLib) for high performance.
- **Toggleable**: Players can toggle the display on/off individually.

## Requirements

- **Paper 1.21.1** (or compatible 1.21.x server)
- **ProtocolLib** (Latest build for 1.21)

## Commands

| Command | Description | Permission | Default |
|---|---|---|---|
| `/health toggle` | Toggle the MobHealth display for yourself | `mobhealth.see` | `true` (Everyone) |
| `/health reload` | Reload the plugin configuration | `mobhealth.reload` | `op` (Operators) |

## Configuration

You can customize the plugin in `config.yml`.

```yaml
# Display Settings
display:
  hp-format: "&aHP: &a{hp}"
  damage-format: "&c- {damage}"
  view-distance: 16.0 # Max distance to see HP bars

# Blacklist
blacklist:
  entities:
    - ARMOR_STAND
```

## Installation

1. Download `mobhealth-1.0.0.jar`.
2. Place it in your server's `plugins` folder.
3. Make sure you have **ProtocolLib** installed.
4. Restart the server.

## Author

- naonao
