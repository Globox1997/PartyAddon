package net.partyaddon.util;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class NameHelper {

    private static final String ELLIPSIS = "..";

    public static Text getPlayerName(MinecraftClient client, UUID playerId, int maxWidth) {
        Text name = resolveDisplayName(client, playerId);
        if (name == null) {
            return Text.translatable("text.partyaddon.hud.empty_name");
        }
        return maxWidth > 0 ? trimToWidth(client, name, maxWidth) : name;
    }

    @Nullable
    private static Text resolveDisplayName(MinecraftClient client, UUID playerId) {
        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(playerId);
            if (entry != null) {
                Text display = entry.getDisplayName();
                if (display != null) {
                    return display;
                }
                return Team.decorateName(entry.getScoreboardTeam(), Text.literal(entry.getProfile().getName()));
            }
        }

        if (client.world != null) {
            PlayerEntity player = client.world.getPlayerByUuid(playerId);
            if (player != null) {
                return player.getDisplayName();
            }
        }

        return null;
    }

    private static Text trimToWidth(MinecraftClient client, Text name, int maxWidth) {
        String raw = name.getString();
        if (client.textRenderer.getWidth(raw) <= maxWidth) {
            return name;
        }

        int ellipsisWidth = client.textRenderer.getWidth(ELLIPSIS);
        int budget = Math.max(0, maxWidth - ellipsisWidth);
        String trimmed = client.textRenderer.trimToWidth(raw, budget) + ELLIPSIS;
        return Text.literal(trimmed);
    }

    public static boolean isPlayerUUIDNotNull(MinecraftClient client, UUID playerId) {
        return playerId != null && client.getNetworkHandler() != null && client.getNetworkHandler().getPlayerListEntry(playerId) != null;
    }
}