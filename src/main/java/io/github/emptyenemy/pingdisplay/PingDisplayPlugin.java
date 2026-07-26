package io.github.emptyenemy.pingdisplay;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PingDisplayPlugin extends JavaPlugin {

    private int pingUpdateInterval;

    @Override
    public void onEnable() {
        // Загружаем конфигурацию
        saveDefaultConfig();  // Если файл не существует, создаём его
        pingUpdateInterval = getConfig().getInt("ping-update-interval", 20);  // Загружаем интервал из конфига

        // Запуск задачи для обновления пинга с заданным интервалом
        startPingTask();
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

    @SuppressWarnings("deprecation")
    private void updatePlayerPing(Player player) {
        int ping = getPing(player);

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

    private int getPing(Player player) {
        return player.getPing();
    }
}
