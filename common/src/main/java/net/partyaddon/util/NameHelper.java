package net.partyaddon.util;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

public final class NameHelper {

    private static final String ELLIPSIS = "..";

    public static Component getPlayerName(Minecraft client, UUID playerId, int maxWidth) {
        Component name = resolveDisplayName(client, playerId);
        if (name == null) {
            return Component.translatable("text.partyaddon.hud.empty_name");
        }
        return maxWidth > 0 ? trimToWidth(client, name, maxWidth) : name;
    }

    @Nullable
    private static Component resolveDisplayName(Minecraft client, UUID playerId) {
        if (client.getConnection() != null) {
            PlayerInfo entry = client.getConnection().getPlayerInfo(playerId);
            if (entry != null) {
                Component display = entry.getTabListDisplayName();
                if (display != null) {
                    return display;
                }
                return PlayerTeam.formatNameForTeam(entry.getTeam(), Component.literal(entry.getProfile().name()));
            }
        }

        if (client.level != null) {
            Player player = client.level.getPlayerByUUID(playerId);
            if (player != null) {
                return player.getDisplayName();
            }
        }

        return null;
    }

    private static Component trimToWidth(Minecraft client, Component name, int maxWidth) {
        String raw = name.getString();
        if (client.font.width(raw) <= maxWidth) {
            return name;
        }

        int ellipsisWidth = client.font.width(ELLIPSIS);
        int budget = Math.max(0, maxWidth - ellipsisWidth);
        String trimmed = client.font.plainSubstrByWidth(raw, budget) + ELLIPSIS;
        return Component.literal(trimmed);
    }

    public static boolean isPlayerUUIDNotNull(Minecraft client, UUID playerId) {
        return playerId != null && client.getConnection() != null && client.getConnection().getPlayerInfo(playerId) != null;
    }
}