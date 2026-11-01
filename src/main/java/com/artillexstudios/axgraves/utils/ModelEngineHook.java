package com.artillexstudios.axgraves.utils;

import com.artillexstudios.axapi.utils.logging.LogUtils;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static com.artillexstudios.axgraves.AxGraves.CONFIG;

// isolated in its own class so ModelEngine's classes are never touched unless isAvailable() is checked first
public class ModelEngineHook {
    private static final String GLOW_TEAM_PREFIX = "axgraves_glow_";

    public static boolean isAvailable() {
        return CONFIG.getBoolean("model-engine.enabled", false) && Bukkit.getPluginManager().isPluginEnabled("ModelEngine");
    }

    @Nullable
    public static TextDisplay spawn(Location location) {
        Location spawnLocation = location.clone().add(
                CONFIG.getFloat("model-engine.offset.x", 0f),
                CONFIG.getFloat("model-engine.offset.y", 0f),
                CONFIG.getFloat("model-engine.offset.z", 0f)
        );

        // TextDisplay has no vanilla hitbox/interaction of its own, so it's a clean, invisible anchor
        // for ModelEngine to render onto - ModelEngine does its own interaction detection regardless
        TextDisplay anchor = spawnLocation.getWorld().spawn(spawnLocation, TextDisplay.class, display -> {
            display.setText("");
            display.setGravity(false);
            display.setInvulnerable(true);
            display.setPersistent(false);
            display.setSeeThrough(true);
        });

        String modelId = CONFIG.getString("model-engine.model-id", "graves");
        try {
            ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(anchor);
            ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
            modeledEntity.addModel(activeModel, true);

            applyGlow(anchor);

            String idleAnimation = CONFIG.getString("model-engine.idle-animation", "");
            if (idleAnimation != null && !idleAnimation.isBlank()) {
                activeModel.getAnimationHandler().playAnimation(idleAnimation, 0.2, 0.2, 1.0, true);
            }
        } catch (Exception ex) {
            LogUtils.error("failed to spawn ModelEngine model '{}' for a grave", modelId, ex);
            anchor.remove();
            return null;
        }

        return anchor;
    }

    public static void remove(@Nullable TextDisplay anchor) {
        if (anchor == null) return;

        try {
            ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(anchor);
            if (modeledEntity != null) modeledEntity.destroy();
        } catch (Exception ignored) {
        }

        anchor.remove();
    }

    private static void applyGlow(TextDisplay anchor) {
        if (!CONFIG.getBoolean("model-engine.glow.enabled", false)) return;

        anchor.setGlowing(true);

        // vanilla's glow outline color is tied to scoreboard team color, which only supports the 16 legacy
        // chat colors - the configured hex is matched to the closest one
        ChatColor color = nearestChatColor(CONFIG.getString("model-engine.glow.color", "FFFFFF"));

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = GLOW_TEAM_PREFIX + color.name();
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            team.setColor(color);
        }

        team.addEntry(anchor.getUniqueId().toString());
    }

    private static ChatColor nearestChatColor(String hex) {
        int rgb;
        try {
            rgb = Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException ex) {
            return ChatColor.WHITE;
        }

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        ChatColor closest = ChatColor.WHITE;
        int closestDistance = Integer.MAX_VALUE;

        for (Map.Entry<ChatColor, int[]> entry : LEGACY_COLOR_RGB.entrySet()) {
            int[] c = entry.getValue();
            int dr = r - c[0];
            int dg = g - c[1];
            int db = b - c[2];
            int distance = dr * dr + dg * dg + db * db;

            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entry.getKey();
            }
        }

        return closest;
    }

    private static final Map<ChatColor, int[]> LEGACY_COLOR_RGB = Map.ofEntries(
            Map.entry(ChatColor.BLACK, new int[]{0x00, 0x00, 0x00}),
            Map.entry(ChatColor.DARK_BLUE, new int[]{0x00, 0x00, 0xAA}),
            Map.entry(ChatColor.DARK_GREEN, new int[]{0x00, 0xAA, 0x00}),
            Map.entry(ChatColor.DARK_AQUA, new int[]{0x00, 0xAA, 0xAA}),
            Map.entry(ChatColor.DARK_RED, new int[]{0xAA, 0x00, 0x00}),
            Map.entry(ChatColor.DARK_PURPLE, new int[]{0xAA, 0x00, 0xAA}),
            Map.entry(ChatColor.GOLD, new int[]{0xFF, 0xAA, 0x00}),
            Map.entry(ChatColor.GRAY, new int[]{0xAA, 0xAA, 0xAA}),
            Map.entry(ChatColor.DARK_GRAY, new int[]{0x55, 0x55, 0x55}),
            Map.entry(ChatColor.BLUE, new int[]{0x55, 0x55, 0xFF}),
            Map.entry(ChatColor.GREEN, new int[]{0x55, 0xFF, 0x55}),
            Map.entry(ChatColor.AQUA, new int[]{0x55, 0xFF, 0xFF}),
            Map.entry(ChatColor.RED, new int[]{0xFF, 0x55, 0x55}),
            Map.entry(ChatColor.LIGHT_PURPLE, new int[]{0xFF, 0x55, 0xFF}),
            Map.entry(ChatColor.YELLOW, new int[]{0xFF, 0xFF, 0x55}),
            Map.entry(ChatColor.WHITE, new int[]{0xFF, 0xFF, 0xFF})
    );
}
