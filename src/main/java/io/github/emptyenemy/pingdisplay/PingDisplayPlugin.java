package io.github.emptyenemy.pingdisplay;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PingDisplayPlugin extends JavaPlugin {

    // Интервал по умолчанию: 20 тиков = 1 секунда
    private static final int DEFAULT_UPDATE_INTERVAL = 20;

    private int pingUpdateInterval;

    @Override
    public void onEnable() {
        // Загружаем конфигурацию
        saveDefaultConfig();  // Если файл не существует, создаём его
        pingUpdateInterval = readUpdateInterval();

        // Запуск задачи для обновления пинга с заданным интервалом
        startPingTask();
    }

    // setPlayerListName помечен устаревшим в пользу Adventure Component;
    // остаётся на нём, пока плагин собирается против API 1.21
    @SuppressWarnings("deprecation")
    @Override
    public void onDisable() {
        // Возвращаем игрокам обычные имена: иначе после выгрузки плагина
        // в таб-листе навсегда останется последнее показанное значение пинга.
        // null сбрасывает имя в таб-листе на обычный ник игрока
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListName(null);
        }
    }

    // Период задачи должен быть положительным, иначе планировщик отклонит её
    private int readUpdateInterval() {
        int interval = getConfig().getInt("ping-update-interval", DEFAULT_UPDATE_INTERVAL);
        if (interval < 1) {
            getLogger().warning("Значение ping-update-interval должно быть не меньше 1, получено "
                    + interval + ". Используется значение по умолчанию: " + DEFAULT_UPDATE_INTERVAL + ".");
            return DEFAULT_UPDATE_INTERVAL;
        }
        return interval;
    }

    // Задача для обновления пинга игроков
    private void startPingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerPing(player);
                }
            }
        }.runTaskTimer(this, 0L, pingUpdateInterval);  // Используем интервал из конфига
    }

    @SuppressWarnings("deprecation")  // см. onDisable: setPlayerListName и §-коды
    private void updatePlayerPing(Player player) {
        int ping = player.getPing();

        // Определяем цвет в зависимости от значения пинга
        String pingColor;
        if (ping < 100) {
            pingColor = "§a"; // Зелёный
        } else if (ping < 200) {
            pingColor = "§e"; // Жёлтый
        } else {
            pingColor = "§c"; // Красный
        }

        // Максимальная длина ника — 16 символов
        int maxNameLength = 16;

        // Форматирование строки с учётом длины имени и добавлением цветного пинга
        String format = String.format("%%-%ds %%s ms", maxNameLength);  // %s вместо %4d
        String displayName = String.format(format, player.getName(), pingColor + ping);

        // Установка нового имени в таб-листе
        player.setPlayerListName(displayName);
    }
}
