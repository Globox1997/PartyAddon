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

    private static final int MAX_HEART_ROWS = 2;

    public static void renderPartyHud(DrawContext context, MinecraftClient client) {
        if (client.player != null && ConfigInit.CONFIG.showGroupHud && !((GroupManagerAccess) client.player).getGroupManager().getGroupPlayerIdList().isEmpty()) {

            List<UUID> groupPlayerIdList = ((GroupManagerAccess) client.player).getGroupManager().getGroupPlayerIdList();
            List<Text> groupPlayerNames = new ArrayList<Text>();

            int maxWidth = ConfigInit.CONFIG.hudMaxWidth;
            int newMaxWidth = 0;
            for (UUID uuid : groupPlayerIdList) {
                Text playerName = NameHelper.getPlayerName(client, uuid, maxWidth);
                if (newMaxWidth < client.textRenderer.getWidth(playerName)) {
                    newMaxWidth = client.textRenderer.getWidth(playerName);
                }
                groupPlayerNames.add(playerName);
            }
            if (newMaxWidth < maxWidth) {
                maxWidth = newMaxWidth;
            }
            int height = groupPlayerIdList.size() * 13 - 5;
            int otherXPos = ConfigInit.CONFIG.hudPosX;
            int otherYPos = ConfigInit.CONFIG.hudPosY;

            float scale = ConfigInit.CONFIG.hudScale;
            float opacity = ConfigInit.CONFIG.hudOpacity;

            int scaledHeight = context.getScaledWindowHeight();

            context.getMatrices().push();

            context.getMatrices().translate(otherXPos, scaledHeight / 2.0f + otherYPos, 0);
            context.getMatrices().scale(scale, scale, 1.0f);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
            RenderSystem.enableBlend();

            // int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight
            // left border
            context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, otherXPos, scaledHeight / 2 + otherYPos, 5, 5, 0, 64, 5, 5, 256, 256);
            context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, otherXPos, scaledHeight / 2 + 5 + otherYPos, 5, height, 0, 70, 5, 5, 256, 256);
            context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, otherXPos, scaledHeight / 2 + height + 5 + otherYPos, 5, 5, 0, 76, 5, 5, 256, 256);
            // middle
            context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, 5 + otherXPos, scaledHeight / 2 + otherYPos, maxWidth, 5, 6, 64, 5, 5, 256, 256);
            context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, 5 + otherXPos, scaledHeight / 2 + 5 + otherYPos, maxWidth, height, 6, 70, 5, 5, 256, 256);
            context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, 5 + otherXPos, scaledHeight / 2 + height + 5 + otherYPos, maxWidth, 5, 6, 76, 5, 5, 256, 256);
            // right border
            context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, maxWidth + 5 + otherXPos, scaledHeight / 2 + otherYPos, 5, 5, 12, 64, 5, 5, 256, 256);
            context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, maxWidth + 5 + otherXPos, scaledHeight / 2 + 5 + otherYPos, 5, height, 12, 70, 5, 5, 256, 256);
            context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, maxWidth + 5 + otherXPos, scaledHeight / 2 + height + 5 + otherYPos, 5, 5, 12, 76, 5, 5, 256, 256);

            int o = 0;
            for (int i = 0; i < groupPlayerNames.size(); i++) {
                UUID playerId = groupPlayerIdList.get(i);

                context.drawText(client.textRenderer, groupPlayerNames.get(i), 5 + otherXPos, scaledHeight / 2 + 5 + o + otherYPos, 0xE6C0C0C0, false);
                PlayerEntity groupMember = client.world != null ? client.world.getPlayerByUuid(playerId) : null;
                drawHealthHearts(context, client, groupMember, 5 + otherXPos, scaledHeight / 2 + 5 + o + otherYPos + client.textRenderer.fontHeight + 1, opacity);

                o += 13 + 9;
            }
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.getMatrices().pop();

        }
    }

    private static void drawHealthHearts(DrawContext context, MinecraftClient client, PlayerEntity player, int x, int y, float opacity) {
        if (player == null || !player.isAlive()) {
            return;
        }
        float maxHealth = player.getMaxHealth();
        float health = MathHelper.clamp(player.getHealth(), 0.0f, maxHealth);

        int maxHearts = MathHelper.ceil(maxHealth / 2.0f);
        int fullHearts = MathHelper.floor(health / 2.0f);
        boolean halfHeart = (health - fullHearts * 2.0f) >= 1.0f;

        final int HEART_FULL_U = 0, HEART_FULL_V = 132;
        final int HEART_HALF_U = 9, HEART_HALF_V = 132;
        final int HEART_EMPTY_U = 18, HEART_EMPTY_V = 132;

        int heartWidth = 9;
        int overlap = 3;
        int spacing = heartWidth - overlap;

        for (int i = 0; i < maxHearts; i++) {
            int u, v;
            if (i < fullHearts) {
                u = HEART_FULL_U;
                v = HEART_FULL_V;
            } else if (i == fullHearts && halfHeart) {
                u = HEART_HALF_U;
                v = HEART_HALF_V;
            } else {
                u = HEART_EMPTY_U;
                v = HEART_EMPTY_V;
            }
            context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, x + i * spacing, y, u, v, 9, 9, 256, 256);
        }
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
