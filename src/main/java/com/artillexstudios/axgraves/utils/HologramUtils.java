package com.artillexstudios.axgraves.utils;

import com.artillexstudios.axapi.hologram.page.HologramPage;
import com.artillexstudios.axapi.hologram.page.TextDisplayHologramPage;
import com.artillexstudios.axapi.packetentity.PacketEntity;
import com.artillexstudios.axapi.packetentity.meta.entity.TextDisplayMeta;
import com.artillexstudios.axapi.utils.logging.LogUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class HologramUtils {

    // "transparent", "default" (vanilla dark box) or a hex ARGB (or RGB) color
    public static void applyBackground(@NotNull TextDisplayMeta meta, @NotNull String background) {
        if (background.equalsIgnoreCase("default")) {
            meta.defaultBackground(true);
            return;
        }

        meta.defaultBackground(false);
        meta.backgroundColor(parseBackground(background));
    }

    private static int parseBackground(String background) {
        if (background.equalsIgnoreCase("transparent")) return 0;

        String hex = background.replace("#", "");
        if (hex.length() == 6 || hex.length() == 8) {
            try {
                long argb = Long.parseLong(hex, 16);
                // RRGGBB has no alpha, so it's fully opaque
                return (int) (hex.length() == 6 ? argb | 0xFF000000L : argb);
            } catch (NumberFormatException ignored) {
            }
        }

        LogUtils.error("invalid hologram background: {} (defaulting to transparent)", background);
        return 0;
    }

    // the yaw at which a fixed text display's front (readable side) points to the given direction
    public static float getYaw(@NotNull String facing) {
        return switch (facing.toUpperCase()) {
            case "SOUTH" -> 0f;
            case "WEST" -> 90f;
            case "NORTH" -> 180f;
            case "EAST" -> -90f;
            default -> {
                LogUtils.error("invalid hologram facing: {} (defaulting to NORTH)", facing);
                yield 180f;
            }
        };
    }

    // axapi keeps the tracked entity of a text hologram page private, and its tracking distance (32 blocks by default)
    // is otherwise the cap for how far away a hologram can be seen
    public static void setViewDistance(@NotNull HologramPage<?, ?> page, int blocks) {
        try {
            Field field = TextDisplayHologramPage.class.getDeclaredField("textDisplay");
            field.setAccessible(true);
            ((PacketEntity) field.get(page)).viewDistance(blocks);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LogUtils.error("failed to set the view distance of a hologram", ex);
        }
    }
}
