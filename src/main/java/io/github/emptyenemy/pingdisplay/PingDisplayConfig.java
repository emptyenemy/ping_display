package io.github.emptyenemy.pingdisplay;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Разобранные и проверенные значения из config.yml.
 * Читается целиком через {@link #load}, дальше не меняется — при перезагрузке
 * плагин просто подменяет весь объект.
 */
public record PingDisplayConfig(
        boolean enabled,
        int updateInterval,
        int updateThreshold,
        int thresholdGood,
        int thresholdMedium,
        TextColor colorGood,
        TextColor colorMedium,
        TextColor colorBad,
        String format
) {

    // Показывать пинг сразу после запуска
    public static final boolean DEFAULT_ENABLED = true;

    // Интервал по умолчанию: 20 тиков = 1 секунда
    public static final int DEFAULT_UPDATE_INTERVAL = 20;

    // Минимальное изменение пинга по умолчанию: 0 — обновлять при любом изменении
    public static final int DEFAULT_UPDATE_THRESHOLD = 0;

    // Пороги по умолчанию, мс
    public static final int DEFAULT_THRESHOLD_GOOD = 100;
    public static final int DEFAULT_THRESHOLD_MEDIUM = 200;

    // Цвета по умолчанию: название из палитры NamedTextColor
    public static final String DEFAULT_COLOR_GOOD = "green";
    public static final String DEFAULT_COLOR_MEDIUM = "yellow";
    public static final String DEFAULT_COLOR_BAD = "red";

    // Шаблон содержимого ячейки: %ping% заменяется на само значение
    public static final String PING_PLACEHOLDER = "%ping%";
    public static final String DEFAULT_FORMAT = "%ping% ms";

    /**
     * Читает конфигурацию, подставляя значения по умолчанию вместо некорректных.
     * Про каждую подстановку сообщает через {@code warn}, чтобы владелец сервера
     * увидел в логе, что его значение не применилось.
     */
    public static PingDisplayConfig load(ConfigurationSection config, Consumer<String> warn) {
        // Период задачи должен быть положительным, иначе планировщик отклонит её
        int interval = config.getInt("ping-update-interval", DEFAULT_UPDATE_INTERVAL);
        if (interval < 1) {
            warn.accept("Значение ping-update-interval должно быть не меньше 1, получено "
                    + interval + ". Используется значение по умолчанию: " + DEFAULT_UPDATE_INTERVAL + ".");
            interval = DEFAULT_UPDATE_INTERVAL;
        }

        // Порог должен быть неотрицательным, иначе сравнение с разницей пинга не имеет смысла
        int updateThreshold = config.getInt("ping-update-threshold", DEFAULT_UPDATE_THRESHOLD);
        if (updateThreshold < 0) {
            warn.accept("Значение ping-update-threshold не может быть отрицательным, получено "
                    + updateThreshold + ". Используется значение по умолчанию: "
                    + DEFAULT_UPDATE_THRESHOLD + ".");
            updateThreshold = DEFAULT_UPDATE_THRESHOLD;
        }

        // Пороги окраски должны быть неотрицательны и идти по возрастанию, иначе она не имеет смысла
        int good = config.getInt("ping-threshold-good", DEFAULT_THRESHOLD_GOOD);
        int medium = config.getInt("ping-threshold-medium", DEFAULT_THRESHOLD_MEDIUM);
        if (good < 0 || medium < 0 || good >= medium) {
            warn.accept("Пороги ping-threshold-good/ping-threshold-medium заданы некорректно ("
                    + good + ", " + medium + "). Используются значения по умолчанию: "
                    + DEFAULT_THRESHOLD_GOOD + ", " + DEFAULT_THRESHOLD_MEDIUM + ".");
            good = DEFAULT_THRESHOLD_GOOD;
            medium = DEFAULT_THRESHOLD_MEDIUM;
        }

        // Без %ping% в шаблоне в ячейке не окажется самого значения
        String format = config.getString("ping-format", DEFAULT_FORMAT);
        if (format == null || !format.contains(PING_PLACEHOLDER)) {
            warn.accept("Значение ping-format должно содержать " + PING_PLACEHOLDER
                    + ", получено: \"" + format + "\". Используется формат по умолчанию: \""
                    + DEFAULT_FORMAT + "\".");
            format = DEFAULT_FORMAT;
        }

        return new PingDisplayConfig(
                config.getBoolean("enabled", DEFAULT_ENABLED),
                interval,
                updateThreshold,
                good,
                medium,
                readColor(config, warn, "ping-color-good", DEFAULT_COLOR_GOOD),
                readColor(config, warn, "ping-color-medium", DEFAULT_COLOR_MEDIUM),
                readColor(config, warn, "ping-color-bad", DEFAULT_COLOR_BAD),
                format
        );
    }

    /** Текст ячейки таб-листа для этого значения пинга. */
    public String render(int ping) {
        return format.replace(PING_PLACEHOLDER, String.valueOf(ping));
    }

    /** Цвет, которым показывается это значение пинга. */
    public TextColor colorFor(int ping) {
        if (ping < thresholdGood) {
            return colorGood;
        }
        if (ping < thresholdMedium) {
            return colorMedium;
        }
        return colorBad;
    }

    private static TextColor readColor(ConfigurationSection config, Consumer<String> warn,
                                       String key, String defaultName) {
        String value = config.getString(key, defaultName);
        TextColor color = value == null ? null : parseColor(value);
        if (color == null) {
            warn.accept("Цвет \"" + value + "\" для " + key
                    + " не распознан ни как название, ни как hex (#RRGGBB). "
                    + "Используется значение по умолчанию: " + defaultName + ".");
            return NamedTextColor.NAMES.value(defaultName);
        }
        return color;
    }

    // Принимает название цвета (например, green) или hex-код (#55FF55)
    static TextColor parseColor(String value) {
        TextColor named = NamedTextColor.NAMES.value(value.toLowerCase(Locale.ROOT));
        return named != null ? named : TextColor.fromHexString(value);
    }
}
