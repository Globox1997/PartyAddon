package net.partyaddon.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.init.RenderInit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class PartyRenderHelper {

    private static final int HEART_WIDTH = 9;
    private static final int HEART_OVERLAP = 3;
    private static final int HEART_SPACING = HEART_WIDTH - HEART_OVERLAP;

    private static final int HEART_ROW_OVERLAP = 3;
    private static final int MAX_HEART_ROWS = 2;
    private static final int PLAYER_SPACING = 4;

    public static void renderPartyHud(DrawContext context, MinecraftClient client) {
        if (client.player == null || !ConfigInit.CONFIG.showGroupHud) {
            return;
        }

        GroupManagerAccess access = (GroupManagerAccess) client.player;
        List<UUID> allIds = access.getGroupManager().getGroupPlayerIdList();
        if (allIds.isEmpty()) {
            return;
        }

        List<UUID> groupPlayerIdList = new ArrayList<>();
        for (UUID uuid : allIds) {
            if (ConfigInit.CONFIG.hideOwnPlayer && uuid.equals(client.player.getUuid())) {
                continue;
            }
            groupPlayerIdList.add(uuid);
        }
        if (groupPlayerIdList.isEmpty()) {
            return;
        }

        List<Text> groupPlayerNames = new ArrayList<>();
        int maxWidth = ConfigInit.CONFIG.hudMaxWidth;
        int newMaxWidth = 0;
        for (UUID uuid : groupPlayerIdList) {
            Text playerName = NameHelper.getPlayerName(client, uuid, maxWidth);
            newMaxWidth = Math.max(newMaxWidth, client.textRenderer.getWidth(playerName));
            groupPlayerNames.add(playerName);
        }
        if (newMaxWidth < maxWidth) {
            maxWidth = newMaxWidth;
        }
        maxWidth = Math.max(maxWidth, HEART_WIDTH);

        int[] heartRows = new int[groupPlayerIdList.size()];
        int totalHeight = 0;
        for (int i = 0; i < groupPlayerIdList.size(); i++) {
            UUID uuid = groupPlayerIdList.get(i);
            PlayerEntity p = client.world != null ? client.world.getPlayerByUuid(uuid) : null;
            int rows = getHeartRowCount(p, maxWidth);
            heartRows[i] = rows;
            int heartBlockHeight = heartBlockHeight(rows);
            totalHeight += client.textRenderer.fontHeight + 1 + heartBlockHeight + PLAYER_SPACING;
        }
        totalHeight -= PLAYER_SPACING;
        int height = totalHeight;

        int otherXPos = ConfigInit.CONFIG.hudPosX;
        int otherYPos = ConfigInit.CONFIG.hudPosY;
        float scale = ConfigInit.CONFIG.hudScale;
        float opacity = (float) ConfigInit.CONFIG.hudOpacity / 100f;

        int scaledHeight = context.getScaledWindowHeight();

        context.getMatrices().push();
        context.getMatrices().translate(otherXPos, scaledHeight / 2.0f + otherYPos, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
        RenderSystem.enableBlend();

        // left border
        context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, 0, 0, 5, 5, 0, 64, 5, 5, 256, 256);
        context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, 0, 5, 5, height, 0, 70, 5, 5, 256, 256);
        context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, 0, height + 5, 5, 5, 0, 76, 5, 5, 256, 256);
        // middle
        context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, 5, 0, maxWidth, 5, 6, 64, 5, 5, 256, 256);
        context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, 5, 5, maxWidth, height, 6, 70, 5, 5, 256, 256);
        context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, 5, height + 5, maxWidth, 5, 6, 76, 5, 5, 256, 256);
        // right border
        context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, maxWidth + 5, 0, 5, 5, 12, 64, 5, 5, 256, 256);
        context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, maxWidth + 5, 5, 5, height, 12, 70, 5, 5, 256, 256);
        context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, maxWidth + 5, height + 5, 5, 5, 12, 76, 5, 5, 256, 256);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int o = 0;
        for (int i = 0; i < groupPlayerNames.size(); i++) {
            UUID playerId = groupPlayerIdList.get(i);

            context.drawText(client.textRenderer, groupPlayerNames.get(i), 5, 5 + o, 0xE6C0C0C0, false);

            PlayerEntity groupMember = client.world != null ? client.world.getPlayerByUuid(playerId) : null;
            drawHealthHearts(context, groupMember, 5, 5 + o + client.textRenderer.fontHeight + 1, maxWidth, opacity);

            int heartBlockHeight = heartBlockHeight(heartRows[i]);
            o += client.textRenderer.fontHeight + 1 + heartBlockHeight + PLAYER_SPACING;
        }

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        context.getMatrices().pop();
    }

    private static void drawHealthHearts(DrawContext context, PlayerEntity player, int x, int y, int maxWidth, float opacity) {
        if (player == null || !player.isAlive()) {
            return;
        }

        float maxHealth = player.getMaxHealth();
        float health = MathHelper.clamp(player.getHealth(), 0.0f, maxHealth);

        int maxHearts = MathHelper.ceil(maxHealth / 2.0f);
        int fullHearts = MathHelper.floor(health / 2.0f);
        boolean halfHeart = (health - fullHearts * 2.0f) >= 1.0f;

        final int HEART_FULL_U = 0;
        final int HEART_HALF_U = 9;
        final int HEART_EMPTY_U = 18;
        final int HEART_V = 132;

        final int heartSize = 9;
        final int overlap = 3;
        final int spacing = heartSize - overlap;

        int heartsPerRow = maxHearts;
        float heartScale = 1.0f;
        int requiredWidth = heartSize + (heartsPerRow - 1) * spacing;

        if (requiredWidth > maxWidth) {
            heartsPerRow = MathHelper.ceil(maxHearts / 2.0f);
            requiredWidth = heartSize + (heartsPerRow - 1) * spacing;
            if (requiredWidth > maxWidth) {
                heartScale = (float) maxWidth / (float) requiredWidth;
            }
        }

        int scaledHeartSize = Math.max(1, Math.round(heartSize * heartScale));
        int scaledSpacing = Math.max(1, Math.round(spacing * heartScale));
        int scaledRowSpacing = Math.max(1, scaledHeartSize - Math.round(HEART_ROW_OVERLAP * heartScale));

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);

        for (int i = 0; i < maxHearts; i++) {
            int row = i / heartsPerRow;
            int column = i % heartsPerRow;
            int drawX = x + column * scaledSpacing;
            int drawY = y + row * scaledRowSpacing;

            int u;
            if (i < fullHearts) {
                u = HEART_FULL_U;
            } else if (i == fullHearts && halfHeart) {
                u = HEART_HALF_U;
            } else {
                u = HEART_EMPTY_U;
            }

            context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, drawX, drawY, heartSize, heartSize, u, HEART_V, heartSize, heartSize, 256, 256);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static int heartBlockHeight(int rows) {
        if (rows <= 0) return 0;
        return HEART_WIDTH + (rows - 1) * (HEART_WIDTH - HEART_ROW_OVERLAP);
    }

    private static int getHeartRowCount(PlayerEntity player, int maxWidth) {
        if (player == null || !player.isAlive()) {
            return 0;
        }
        int maxHearts = MathHelper.ceil(player.getMaxHealth() / 2.0f);
        if (maxHearts <= 0) {
            return 0;
        }
        int perRow = calculateHeartsPerRow(maxHearts, maxWidth);
        return MathHelper.ceil((float) maxHearts / perRow);
    }

    private static int calculateHeartsPerRow(int maximumHearts, int maxWidth) {
        if (maximumHearts <= 0) {
            return 1;
        }
        int oneRowWidth = calculateHeartRowWidth(maximumHearts);
        if (oneRowWidth <= maxWidth) {
            return maximumHearts;
        }
        return MathHelper.ceil((float) maximumHearts / MAX_HEART_ROWS);
    }

    private static int calculateHeartRowWidth(int heartCount) {
        if (heartCount <= 0) {
            return 0;
        }

        return HEART_WIDTH + (heartCount - 1) * HEART_SPACING;
    }

}
