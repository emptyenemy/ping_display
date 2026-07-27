# PingDisplay

[![Build](https://github.com/emptyenemy/ping_display/actions/workflows/build.yml/badge.svg)](https://github.com/emptyenemy/ping_display/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.2%2B-brightgreen.svg)](https://papermc.io/)

A lightweight Paper plugin that shows each player's ping directly in the tab list, colour-coded by connection quality.

*[Русская версия](README.md)*

## Preview

![Player ping in the tab list](screenshot.png)

Default colours:

| Ping | Colour |
|------|--------|
| < 100 ms | 🟢 green |
| 100–199 ms | 🟡 yellow |
| ≥ 200 ms | 🔴 red |

Thresholds and colours are configurable — see [Configuration](#configuration).

## Requirements

- **Minecraft:** 26.2+
- **Server:** Paper (or a compatible fork)
- **Java:** 25+

## Installation

1. Download `PingDisplay-1.0.2.jar` from any of:
   - [GitHub Releases](https://github.com/emptyenemy/ping_display/releases)
   - [Hangar (PaperMC)](https://hangar.papermc.io/emptyenemy/PingDisplay)
   - [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/pingdisplay)
2. Drop it into your server's `plugins/` folder.
3. Restart the server.

On first start the plugin creates `plugins/PingDisplay/config.yml` with default values.

## Commands

The main command is `/pingdisplay`, with `/pd` as a short alias.

| Command | Description | Permission |
|---------|-------------|------------|
| `/pingdisplay` | Your own ping | `pingdisplay.check` (everyone by default) |
| `/pingdisplay <player>` | That player's ping | `pingdisplay.check` |
| `/pingdisplay @<player>` | Same, but the argument is always treated as a name | `pingdisplay.check` |
| `/pingdisplay on` \| `off` | Enable or disable the tab list display | `pingdisplay.toggle` (op) |
| `/pingdisplay reload` | Re-read `config.yml` without restarting | `pingdisplay.reload` (op) |
| `/pingdisplay help` | List available commands | — |

Subcommands take priority over player names. If someone is called `off` or `reload`, check
their ping with `@`: `/pd @off`.

The `on`/`off` state is written to `config.yml`, so it survives a server restart.

### A `/ping` alias

The plugin deliberately does not claim the `/ping` name — it usually belongs to another
plugin already (EssentialsX, for one). If it's free on your server and you prefer it, add the
alias yourself in `commands.yml` in the server root:

```yaml
aliases:
  ping:
  - "pingdisplay $1-"
```

## Configuration

`plugins/PingDisplay/config.yml`:

```yaml
# Whether to show ping in the tab list. Toggle at runtime: /pingdisplay on | off
enabled: true

ping-update-interval: 20  # Refresh rate in ticks (20 ticks = 1 second)

# Minimum ping change in ms required to refresh the tab list.
# 0 — refresh on any change. Useful to avoid churn from 1-2 ms jitter.
ping-update-threshold: 0

# Ping colour thresholds, in milliseconds
ping-threshold-good: 100    # below this value — green
ping-threshold-medium: 200  # below this value — yellow, at or above — red

# What to show in the tab list column. %ping% is replaced with the value.
# For example, "[%ping%] ms" gives [45] ms, and "%ping%" gives just 45
ping-format: "%ping% ms"

# Ping colours: a name (green, yellow, red, aqua, …) or a hex code (#55FF55)
ping-color-good: green
ping-color-medium: yellow
ping-color-bad: red
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | bool | `true` | Whether ping is shown. Toggled with `/pingdisplay on` and `off`. |
| `ping-update-interval` | int | `20` | Refresh interval in game ticks. Lower values refresh more often at a slightly higher cost. |
| `ping-update-threshold` | int | `0` | Minimum ping change in ms that triggers a refresh. `0` — refresh on any change. |
| `ping-threshold-good` | int | `100` | Ping in ms below which the "good" colour is used. |
| `ping-threshold-medium` | int | `200` | Ping in ms below which the "medium" colour is used; at or above it — "bad". |
| `ping-format` | string | `"%ping% ms"` | Column contents. `%ping%` is required and is replaced with the value. |
| `ping-color-good` | string | `green` | Colour for ping below `ping-threshold-good`. |
| `ping-color-medium` | string | `yellow` | Colour for ping below `ping-threshold-medium`. |
| `ping-color-bad` | string | `red` | Colour for ping at `ping-threshold-medium` and above. |

Colours accept either a name from the [Minecraft palette](https://jd.papermc.io/adventure/net/kyori/adventure/text/format/NamedTextColor.html)
(`green`, `aqua`, `gold`, …) or a hex code like `#55FF55`.

Invalid values never break the plugin: it logs a warning and falls back to the default.
Changes take effect via `/pingdisplay reload` or after a server restart.

## Building from source

Requires JDK 25+ and Maven 3.9+:

```bash
git clone https://github.com/emptyenemy/ping_display.git
cd ping_display
mvn clean package
```

The jar is produced at `target/PingDisplay-1.0.2.jar`.

## How it works

The plugin **does not touch player names**. Ping is rendered as a separate tab list column,
via a scoreboard objective in the `PLAYER_LIST` slot. As a result LuckPerms prefixes,
EssentialsX nicknames and any other name styling stay intact.

## Known limitations

- There is only one `PLAYER_LIST` slot per server. If another plugin already displays
  something there (kills, balance, health), it cannot coexist with PingDisplay.
- If another plugin assigns players their own scoreboards (TAB does this, for example),
  PingDisplay's objective on the main scoreboard won't be visible to them. In that case
  you have to pick one or the other.

## License

[MIT](LICENSE) © emptyenemy
