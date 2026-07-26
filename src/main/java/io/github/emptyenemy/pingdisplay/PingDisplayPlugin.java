package io.github.emptyenemy.pingdisplay;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PingDisplayPlugin extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    private static final String OBJECTIVE_NAME = "pingdisplay";

    private static final String PERM_CHECK = "pingdisplay.check";
    private static final String PERM_TOGGLE = "pingdisplay.toggle";
    private static final String PERM_RELOAD = "pingdisplay.reload";

    private PingDisplayConfig config;
    private Objective objective;
    private ScheduledTask pingTask;

    // Последний показанный пинг игрока: не трогаем таб-лист, если значение не изменилось
    private final Map<UUID, Integer> lastPings = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();  // Если файл не существует, создаём его
        config = PingDisplayConfig.load(getConfig(), getLogger()::warning);

        getCommand("pingdisplay").setExecutor(this);
        getCommand("pingdisplay").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);

        if (config.enabled()) {
            startDisplay();
        } else {
            getLogger().info("Отображение пинга выключено (enabled: false). "
                    + "Включить: /pingdisplay on");
        }
    }

    @Override
    public void onDisable() {
        stopDisplay();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Иначе записи об отключившихся игроках копятся и в памяти, и в самом объективе
        lastPings.remove(event.getPlayer().getUniqueId());
        if (objective != null) {
            objective.getScoreboard().resetScores(event.getPlayer().getName());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Без аргументов — свой пинг
        if (args.length == 0) {
            return handlePing(sender, null);
        }

        // Ник можно явно пометить @, чтобы он не спорил с названиями подкоманд
        if (args[0].startsWith("@")) {
            return handlePing(sender, args[0].substring(1));
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> handleToggle(sender, true);
            case "off" -> handleToggle(sender, false);
            case "reload" -> handleReload(sender);
            case "help" -> handleHelp(sender);
            default -> handlePing(sender, args[0]);
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        List<String> options = new ArrayList<>(List.of("on", "off", "reload", "help"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            options.add(player.getName());
        }
        return filterByPrefix(options, args[0]);
    }

    private static List<String> filterByPrefix(List<String> options, String prefix) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                matches.add(option);
            }
        }
        return matches;
    }

    // Пинг и так виден всем в таб-листе, поэтому смотреть чужой отдельным правом не ограничиваем
    private boolean handlePing(CommandSender sender, String targetName) {
        if (!sender.hasPermission(PERM_CHECK)) {
            sendNoPermission(sender);
            return true;
        }

        Player target;
        if (targetName == null || targetName.isEmpty()) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Из консоли нужно указать игрока: /pingdisplay <игрок>",
                        NamedTextColor.RED));
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                // Ник — это ветка по умолчанию, поэтому сюда попадает и опечатка в подкоманде
                sender.sendMessage(Component.text("Игрок " + targetName + " не в сети.", NamedTextColor.RED)
                        .append(Component.text(" Список команд: /pingdisplay help", NamedTextColor.GRAY)));
                return true;
            }
        }

        int ping = Math.max(target.getPing(), 0);
        Component prefix = target.equals(sender)
                ? Component.text("Ваш пинг: ")
                : Component.text("Пинг игрока " + target.getName() + ": ");

        sender.sendMessage(prefix
                .append(Component.text(ping).color(config.colorFor(ping)))
                .append(Component.text(" ms")));
        return true;
    }

    // Показываем только то, что отправитель действительно может выполнить
    private boolean handleHelp(CommandSender sender) {
        // Версия, автор и сайт берутся из plugin.yml, чтобы не дублировать их в коде
        sender.sendMessage(Component.text(
                "PingDisplay v" + getPluginMeta().getVersion(), NamedTextColor.GREEN)
                .append(Component.text(" by @" + String.join(", @", getPluginMeta().getAuthors()),
                        NamedTextColor.GRAY)));

        if (sender.hasPermission(PERM_CHECK)) {
            sendHelpLine(sender, "/pingdisplay", "свой пинг");
            sendHelpLine(sender, "/pingdisplay <игрок>", "пинг игрока");
            sendHelpLine(sender, "/pingdisplay @<игрок>", "пинг игрока с ником вроде off или reload");
        }
        if (sender.hasPermission(PERM_TOGGLE)) {
            sendHelpLine(sender, "/pingdisplay on | off", "включить или выключить отображение в таб-листе");
        }
        if (sender.hasPermission(PERM_RELOAD)) {
            sendHelpLine(sender, "/pingdisplay reload", "перечитать config.yml");
        }
        return true;
    }

    private static void sendHelpLine(CommandSender sender, String usage, String description) {
        sender.sendMessage(Component.text(usage, NamedTextColor.YELLOW)
                .append(Component.text(" — " + description, NamedTextColor.GRAY)));
    }

    private boolean handleToggle(CommandSender sender, boolean enable) {
        if (!sender.hasPermission(PERM_TOGGLE)) {
            sendNoPermission(sender);
            return true;
        }

        if (config.enabled() == enable) {
            sender.sendMessage(Component.text("Отображение пинга уже "
                    + (enable ? "включено." : "выключено."), NamedTextColor.YELLOW));
            return true;
        }

        // Пишем в config.yml, чтобы состояние пережило перезапуск сервера
        getConfig().set("enabled", enable);
        saveConfig();
        config = PingDisplayConfig.load(getConfig(), getLogger()::warning);

        if (enable) {
            startDisplay();
        } else {
            stopDisplay();
        }

        sender.sendMessage(Component.text("Отображение пинга "
                + (enable ? "включено." : "выключено."), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(PERM_RELOAD)) {
            sendNoPermission(sender);
            return true;
        }

        reloadConfig();
        config = PingDisplayConfig.load(getConfig(), getLogger()::warning);

        // Пороги, цвета и интервал могли поменяться — пересобираем отображение с нуля
        stopDisplay();
        if (config.enabled()) {
            startDisplay();
        }

        sender.sendMessage(Component.text("Конфигурация PingDisplay перезагружена.", NamedTextColor.GREEN));
        return true;
    }

    private static void sendNoPermission(CommandSender sender) {
        sender.sendMessage(Component.text("Недостаточно прав.", NamedTextColor.RED));
    }

    // Пинг показывается ванильной колонкой таб-листа, а не через подмену ника:
    // так имена игроков остаются за сервером и другими плагинами
    private void startDisplay() {
        Scoreboard scoreboard = getServer().getScoreboardManager().getMainScoreboard();

        // Объектив с прошлого запуска мог пережить перезагрузку плагина
        Objective existing = scoreboard.getObjective(OBJECTIVE_NAME);
        if (existing != null) {
            existing.unregister();
        }

        objective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME, Criteria.DUMMY, Component.text("Ping"));
        objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);

        startPingTask();
    }

    // Снимаем объектив: иначе в таб-листе навсегда останутся
    // последние показанные значения пинга
    private void stopDisplay() {
        if (pingTask != null) {
            pingTask.cancel();
            pingTask = null;
        }
        if (objective != null) {
            objective.unregister();
            objective = null;
        }
        lastPings.clear();
    }

    // Планировщик Paper/Folia: на обычном Paper задача выполняется в главном потоке,
    // на Folia — в глобальном регионе. Начальная задержка должна быть больше нуля.
    private void startPingTask() {
        pingTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
                this,
                task -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        updatePlayerPing(player);
                    }
                },
                1L,
                config.updateInterval()
        );
    }

    private void updatePlayerPing(Player player) {
        // Сразу после подключения getPing() может ненадолго вернуть отрицательное значение
        int ping = Math.max(player.getPing(), 0);
        UUID uuid = player.getUniqueId();

        // Пропускаем обновление, если пинг не изменился совсем или изменился меньше порога
        Integer lastDisplayedPing = lastPings.get(uuid);
        if (lastDisplayedPing != null) {
            int diff = Math.abs(ping - lastDisplayedPing);
            if (diff == 0 || diff < config.updateThreshold()) {
                return;
            }
        }
        lastPings.put(uuid, ping);

        Score score = objective.getScore(player.getName());
        score.setScore(ping);
        // fixed вместо styled: ваниль рисует голое число, а так в ячейку
        // попадает весь шаблон целиком, вместе с припиской вроде " ms"
        score.numberFormat(NumberFormat.fixed(
                Component.text(config.render(ping)).color(config.colorFor(ping))));
    }
}
