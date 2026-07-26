# PingDisplay

[![Build](https://github.com/emptyenemy/ping_display/actions/workflows/build.yml/badge.svg)](https://github.com/emptyenemy/ping_display/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen.svg)](https://papermc.io/)

A lightweight Paper/Spigot plugin that shows each player's ping directly in the tab list, colour-coded by connection quality.

*[Русская версия](README.md)*

## Preview

![Player ping in the tab list](screenshot.jpeg)

| Ping | Colour |
|------|--------|
| < 100 ms | 🟢 green |
| 100–199 ms | 🟡 yellow |
| ≥ 200 ms | 🔴 red |

## Requirements

- **Minecraft:** 1.21.x
- **Server:** Paper (or a Spigot/Bukkit-compatible fork)
- **Java:** 21+

## Installation

1. Download `PingDisplay-1.0.1.jar` from the [releases page](https://github.com/emptyenemy/ping_display/releases).
2. Drop it into your server's `plugins/` folder.
3. Restart the server.

On first start the plugin creates `plugins/PingDisplay/config.yml` with default values.

## Configuration

`plugins/PingDisplay/config.yml`:

```yaml
# Ping refresh rate in ticks (20 ticks = 1 second)
ping-update-interval: 20
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `ping-update-interval` | int | `20` | Tab list refresh interval in game ticks. Lower values refresh more often at a slightly higher cost. |

Changes take effect after a server restart.

## Building from source

Requires JDK 21+ and Maven 3.9+:

```bash
git clone https://github.com/emptyenemy/ping_display.git
cd ping_display
mvn clean package
```

The jar is produced at `target/PingDisplay-1.0.1.jar`.

## Roadmap

The plugin is intentionally minimal. Planned next:

- support for current Minecraft versions;
- in-game config reload command, no restart required;
- configurable thresholds and colours;
- configurable display format instead of the hard-coded one;
- better compatibility with other tab list plugins.

## License

[MIT](LICENSE) © emptyenemy
