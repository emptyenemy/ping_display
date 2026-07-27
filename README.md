# PingDisplay

[![Build](https://github.com/emptyenemy/ping_display/actions/workflows/build.yml/badge.svg)](https://github.com/emptyenemy/ping_display/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.2%2B-brightgreen.svg)](https://papermc.io/)

Лёгкий плагин для Paper, который показывает пинг каждого игрока прямо в таб-листе (список игроков по клавише <kbd>Tab</kbd>). Значение подсвечивается цветом в зависимости от качества соединения.

*[English version](README.en.md)*

## Как выглядит

![Пинг игрока в таб-листе](screenshot.png)

Цвета по умолчанию:

| Пинг | Цвет |
|------|------|
| < 100 ms | 🟢 зелёный |
| 100–199 ms | 🟡 жёлтый |
| ≥ 200 ms | 🔴 красный |

Пороги и цвета настраиваются — см. [Настройку](#настройка).

## Требования

- **Minecraft:** 26.2+
- **Сервер:** Paper (или совместимый форк)
- **Java:** 25+

## Установка

1. Скачайте `PingDisplay-1.0.2.jar` с любой из площадок:
   - [GitHub Releases](https://github.com/emptyenemy/ping_display/releases)
   - [Hangar (PaperMC)](https://hangar.papermc.io/emptyenemy/PingDisplay)
   - [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/pingdisplay)
2. Положите файл в папку `plugins/` вашего сервера.
3. Перезапустите сервер.

При первом запуске плагин создаст `plugins/PingDisplay/config.yml` со значениями по умолчанию.

## Команды

Основная команда — `/pingdisplay`, короткий алиас — `/pd`.

| Команда | Описание | Право |
|---------|----------|-------|
| `/pingdisplay` | Свой текущий пинг | `pingdisplay.check` (по умолчанию у всех) |
| `/pingdisplay <игрок>` | Пинг указанного игрока | `pingdisplay.check` |
| `/pingdisplay @<игрок>` | То же, но аргумент всегда трактуется как ник | `pingdisplay.check` |
| `/pingdisplay on` \| `off` | Включить или выключить отображение в таб-листе | `pingdisplay.toggle` (op) |
| `/pingdisplay reload` | Перечитать `config.yml` без рестарта сервера | `pingdisplay.reload` (op) |
| `/pingdisplay help` | Список доступных команд | — |

Подкоманды имеют приоритет над никами. Если на сервере есть игрок с ником вроде `off` или
`reload`, посмотреть его пинг можно через `@`: `/pd @off`.

Состояние `on`/`off` сохраняется в `config.yml`, то есть переживает перезапуск сервера.

### Алиас `/ping`

Плагин намеренно не занимает имя `/ping` — оно часто уже принадлежит другому плагину
(например, EssentialsX). Если на вашем сервере оно свободно и вам так удобнее, добавьте
алиас сами в `commands.yml` в корне сервера:

```yaml
aliases:
  ping:
  - "pingdisplay $1-"
```

## Настройка

`plugins/PingDisplay/config.yml`:

```yaml
# Показывать ли пинг в таб-листе. Переключается на ходу: /pingdisplay on | off
enabled: true

ping-update-interval: 20  # Частота обновления пинга в тиках (20 тиков = 1 секунда)

# Минимальное изменение пинга в мс, при котором обновляется таб-лист.
# 0 — обновлять при любом изменении. Помогает не дёргать таб-лист из-за колебаний в 1-2 мс.
ping-update-threshold: 0

# Пороги окраски пинга, в миллисекундах
ping-threshold-good: 100    # ниже этого значения — зелёный
ping-threshold-medium: 200  # ниже этого значения — жёлтый, начиная с него — красный

# Что показывать в колонке таб-листа. %ping% заменяется на само значение.
# Например, "[%ping%] мс" даст [45] мс, а "%ping%" — просто 45
ping-format: "%ping% ms"

# Цвета пинга: название (например, green, yellow, red, aqua) или hex-код (#55FF55)
ping-color-good: green
ping-color-medium: yellow
ping-color-bad: red
```

| Параметр | Тип | По умолчанию | Описание |
|----------|-----|--------------|----------|
| `enabled` | bool | `true` | Показывать ли пинг. Меняется командой `/pingdisplay on` и `off`. |
| `ping-update-interval` | int | `20` | Интервал обновления в игровых тиках. Меньшее значение — более частое обновление и чуть выше нагрузка. |
| `ping-update-threshold` | int | `0` | Минимальное изменение пинга в мс, при котором таб-лист обновляется. `0` — обновлять при любом изменении. |
| `ping-threshold-good` | int | `100` | Порог в мс, ниже которого пинг подсвечивается «хорошим» цветом. |
| `ping-threshold-medium` | int | `200` | Порог в мс, ниже которого используется «средний» цвет; начиная с него — «плохой». |
| `ping-format` | string | `"%ping% ms"` | Содержимое колонки. `%ping%` обязателен и заменяется на значение. |
| `ping-color-good` | string | `green` | Цвет для пинга ниже `ping-threshold-good`. |
| `ping-color-medium` | string | `yellow` | Цвет для пинга ниже `ping-threshold-medium`. |
| `ping-color-bad` | string | `red` | Цвет для пинга от `ping-threshold-medium` и выше. |

Цвета принимают либо название из [палитры Minecraft](https://jd.papermc.io/adventure/net/kyori/adventure/text/format/NamedTextColor.html)
(`green`, `aqua`, `gold`, …), либо hex-код вида `#55FF55`.

Некорректные значения не роняют плагин: он пишет предупреждение в лог и использует значение
по умолчанию. Изменения применяются командой `/pingdisplay reload` или после перезапуска сервера.

## Сборка из исходников

Нужны JDK 25+ и Maven 3.9+:

```bash
git clone https://github.com/emptyenemy/ping_display.git
cd ping_display
mvn clean package
```

Готовый jar появится в `target/PingDisplay-1.0.2.jar`.

## Как это устроено

Плагин **не трогает ники игроков**. Пинг выводится отдельной колонкой таб-листа — через
scoreboard-объектив в слоте `PLAYER_LIST`. Благодаря этому префиксы LuckPerms, ники из
EssentialsX и прочее оформление имён остаются нетронутыми.

## Известные ограничения

- Слот `PLAYER_LIST` на сервере один. Если другой плагин уже показывает в нём что-то своё
  (килы, деньги, здоровье), одновременно с PingDisplay это работать не будет.
- Если другой плагин выдаёт игрокам персональные scoreboard'ы (так делает, например, TAB),
  объектив PingDisplay на главном scoreboard им не отобразится. В этом случае придётся
  выбрать что-то одно.

## Лицензия

[MIT](LICENSE) © emptyenemy
