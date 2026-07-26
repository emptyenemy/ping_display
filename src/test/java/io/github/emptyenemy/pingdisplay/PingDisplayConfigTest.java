package io.github.emptyenemy.pingdisplay;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PingDisplayConfigTest {

    private final List<String> warnings = new ArrayList<>();

    private PingDisplayConfig load(String yaml) {
        return PingDisplayConfig.load(
                YamlConfiguration.loadConfiguration(new StringReader(yaml)),
                warnings::add
        );
    }

    @Test
    void emptyConfigFallsBackToDefaultsSilently() {
        PingDisplayConfig config = load("");

        assertTrue(config.enabled());
        assertEquals(PingDisplayConfig.DEFAULT_UPDATE_INTERVAL, config.updateInterval());
        assertEquals(PingDisplayConfig.DEFAULT_UPDATE_THRESHOLD, config.updateThreshold());
        assertEquals(PingDisplayConfig.DEFAULT_THRESHOLD_GOOD, config.thresholdGood());
        assertEquals(PingDisplayConfig.DEFAULT_THRESHOLD_MEDIUM, config.thresholdMedium());
        assertTrue(warnings.isEmpty(), "Пустой конфиг — это штатный случай, предупреждать не о чем");
    }

    @Test
    void readsCustomValues() {
        PingDisplayConfig config = load("""
                ping-update-interval: 40
                ping-update-threshold: 5
                ping-threshold-good: 50
                ping-threshold-medium: 150
                """);

        assertEquals(40, config.updateInterval());
        assertEquals(5, config.updateThreshold());
        assertEquals(50, config.thresholdGood());
        assertEquals(150, config.thresholdMedium());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void rejectsNonPositiveInterval() {
        assertEquals(PingDisplayConfig.DEFAULT_UPDATE_INTERVAL, load("ping-update-interval: 0").updateInterval());
        assertEquals(1, warnings.size());
    }

    @Test
    void rejectsNegativeUpdateThreshold() {
        assertEquals(PingDisplayConfig.DEFAULT_UPDATE_THRESHOLD, load("ping-update-threshold: -1").updateThreshold());
        assertEquals(1, warnings.size());
    }

    @Test
    void rejectsThresholdsOutOfOrder() {
        PingDisplayConfig config = load("""
                ping-threshold-good: 300
                ping-threshold-medium: 100
                """);

        assertEquals(PingDisplayConfig.DEFAULT_THRESHOLD_GOOD, config.thresholdGood());
        assertEquals(PingDisplayConfig.DEFAULT_THRESHOLD_MEDIUM, config.thresholdMedium());
        assertEquals(1, warnings.size());
    }

    @Test
    void parsesNamedAndHexColors() {
        PingDisplayConfig config = load("""
                ping-color-good: AQUA
                ping-color-medium: "#123456"
                """);

        assertEquals(NamedTextColor.AQUA, config.colorGood());
        assertEquals(TextColor.fromHexString("#123456"), config.colorMedium());
        assertEquals(NamedTextColor.RED, config.colorBad());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void rejectsUnknownColor() {
        PingDisplayConfig config = load("ping-color-good: not-a-colour");

        assertEquals(NamedTextColor.GREEN, config.colorGood());
        assertEquals(1, warnings.size());
    }

    @Test
    void rendersDefaultFormat() {
        assertEquals("45 ms", load("").render(45));
    }

    @Test
    void rendersCustomFormat() {
        assertEquals("[45] мс", load("ping-format: \"[%ping%] мс\"").render(45));
        assertEquals("45", load("ping-format: \"%ping%\"").render(45));
        assertEquals("пинг 45 сейчас", load("ping-format: \"пинг %ping% сейчас\"").render(45));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void rejectsFormatWithoutPingPlaceholder() {
        assertEquals(PingDisplayConfig.DEFAULT_FORMAT, load("ping-format: \"нет плейсхолдера\"").format());
        assertEquals(1, warnings.size());
    }

    @Test
    void readsDisabledFlag() {
        assertFalse(load("enabled: false").enabled());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void picksColorByThresholds() {
        PingDisplayConfig config = load("");

        assertEquals(NamedTextColor.GREEN, config.colorFor(0));
        assertEquals(NamedTextColor.GREEN, config.colorFor(99));
        // Граница включительно относится к следующему цвету
        assertEquals(NamedTextColor.YELLOW, config.colorFor(100));
        assertEquals(NamedTextColor.YELLOW, config.colorFor(199));
        assertEquals(NamedTextColor.RED, config.colorFor(200));
        assertEquals(NamedTextColor.RED, config.colorFor(5000));
    }
}
