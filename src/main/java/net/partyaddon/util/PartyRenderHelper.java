package net.partyaddon.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.partyaddon.group.ClientParty;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.init.RenderInit;
import net.partyaddon.network.packet.MemberHealthPacket.MemberHealth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class PartyRenderHelper {

    private static final int HEART_SIZE = 9;
    private static final int HEART_SPACING = HEART_SIZE - 3;
    private static final int HEART_ROW_SPACING = HEART_SIZE - 3;
    private static final int MAX_HEART_ROWS = 2;
    private static final int PLAYER_SPACING = 4;

    private static final int HEART_FULL_U = 0;
    private static final int HEART_HALF_U = 9;
    private static final int HEART_EMPTY_U = 18;
    private static final int HEART_V = 132;

    private record HeartLayout(MemberHealth memberHealth, int maxHearts, int perRow, float scale, int width, int height) {
    }

    public static void renderPartyHud(GuiGraphics context, Minecraft client) {
        if (client.player == null || client.options.hideGui || !ConfigInit.CONFIG.showGroupHud) {
            return;
        }

        List<UUID> allIds = ClientParty.getGroupPlayerIds();
        if (allIds.isEmpty()) {
            return;
        }

        int maxWidth = Math.max(HEART_SIZE, ConfigInit.CONFIG.hudMaxWidth);
        int fontHeight = client.font.lineHeight;

        List<Component> names = new ArrayList<>(allIds.size());
        List<HeartLayout> heartLayouts = new ArrayList<>(allIds.size());
        int width = HEART_SIZE;
        int height = 0;
        for (UUID uuid : allIds) {
            if (ConfigInit.CONFIG.hideOwnPlayer && uuid.equals(client.player.getUUID())) {
                continue;
            }
            Component name = NameHelper.getPlayerName(client, uuid, maxWidth);
            HeartLayout heartLayout = layoutHearts(getMemberHealth(client, uuid), maxWidth);

            if (!names.isEmpty()) {
                height += PLAYER_SPACING;
            }
            names.add(name);
            heartLayouts.add(heartLayout);

            width =Math.max(width, client.font.width(name));
            if (heartLayout != null) {
                width = Math.max(width, heartLayout.width());
            }
            height += fontHeight + 1 + (heartLayout != null ? heartLayout.height() : 0);
        }
        if (names.isEmpty()) {
            return;
        }
        width = Math.min(width, maxWidth);

        float scale = ConfigInit.CONFIG.hudScale;
        float opacity = (float) ConfigInit.CONFIG.hudOpacity / 100f;

        PoseStack matrices = context.pose();
        matrices.pushPose();
        matrices.translate(ConfigInit.CONFIG.hudPosX, context.guiHeight() / 2.0f + ConfigInit.CONFIG.hudPosY, 0);
        matrices.scale(scale, scale, 1.0f);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);

        // left border
        context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, 0, 0, 5, 5, 0, 64, 5, 5, 256, 256);
        context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, 0, 5, 5, height, 0, 70, 5, 5, 256, 256);
        context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, 0, height + 5, 5, 5, 0, 76, 5, 5, 256, 256);
        // middle
        context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, 5, 0, width, 5, 6, 64, 5, 5, 256, 256);
        context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, 5, 5, width, height, 6, 70, 5, 5, 256, 256);
        context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, 5, height + 5, width, 5, 6, 76, 5, 5, 256, 256);
        // right border
        context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, width + 5, 0, 5, 5, 12, 64, 5, 5, 256, 256);
        context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, width + 5, 5, 5, height, 12, 70, 5, 5, 256, 256);
        context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, width + 5, height + 5, 5, 5, 12, 76, 5, 5, 256, 256);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int y = 5;
        for (int i = 0; i < names.size(); i++) {
            context.drawString(client.font, names.get(i), 5, y, 0xE6C0C0C0, false);
            y += fontHeight + 1;

            HeartLayout heartLayout = heartLayouts.get(i);
            if (heartLayout != null) {
                drawHealthHearts(context, heartLayout, 5, y, opacity);
                y += heartLayout.height();
            }
            y += PLAYER_SPACING;
        }

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        matrices.popPose();
    }

    @Nullable
    private static MemberHealth getMemberHealth(Minecraft client, UUID playerId) {
        Player player = client.level != null ? client.level.getPlayerByUUID(playerId) : null;
        MemberHealth memberHealth = player != null ? new MemberHealth(playerId, player.getHealth(), player.getMaxHealth()) : ClientParty.getMemberHealth(playerId);
        if (memberHealth == null || memberHealth.health() <= 0.0f || memberHealth.maxHealth() <= 0.0f) {
            return null;
        }
        return memberHealth;
    }

    @Nullable
    private static HeartLayout layoutHearts(@Nullable MemberHealth memberHealth, int maxWidth) {
        if (memberHealth == null) {
            return null;
        }
        int maxHearts = Mth.ceil(memberHealth.maxHealth() / 2.0f);
        if (maxHearts <= 0) {
            return null;
        }
        int perRow = maxHearts;
        if (getHeartRowWidth(perRow) > maxWidth) {
            perRow = Mth.ceil((float) maxHearts / MAX_HEART_ROWS);
        }
        int rows = Mth.ceil((float) maxHearts / perRow);

        int unscaledWidth = getHeartRowWidth(perRow);
        int unscaledHeight = HEART_SIZE + (rows - 1) * HEART_ROW_SPACING;
        float scale = unscaledWidth > maxWidth ? (float) maxWidth / (float) unscaledWidth : 1.0f;

        int width = Math.min(maxWidth, Mth.ceil(unscaledWidth * scale));
        int height = Mth.ceil(unscaledHeight * scale);
        return new HeartLayout(memberHealth, maxHearts, perRow, scale, width, height);
    }

    private static int getHeartRowWidth(int heartCount) {
        return HEART_SIZE + (heartCount - 1) * HEART_SPACING;
    }

    private static void drawHealthHearts(GuiGraphics context, HeartLayout heartLayout, int x, int y, float opacity) {
        float maxHealth = heartLayout.memberHealth().maxHealth();
        float health = Mth.clamp(heartLayout.memberHealth().health(), 0.0f, maxHealth);
        int fullHearts = Mth.floor(health / 2.0f);
        boolean halfHeart = (health - fullHearts * 2.0f) >= 1.0f;

        PoseStack matrices = context.pose();
        matrices.pushPose();
        matrices.translate(x, y, 0);
        matrices.scale(heartLayout.scale(), heartLayout.scale(), 1.0f);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);

        for (int i = 0; i < heartLayout.maxHearts(); i++) {
            int u;
            if (i < fullHearts) {
                u = HEART_FULL_U;
            } else if (i == fullHearts && halfHeart) {
                u = HEART_HALF_U;
            } else {
                u = HEART_EMPTY_U;
            }
            int drawX = (i % heartLayout.perRow()) * HEART_SPACING;
            int drawY = (i / heartLayout.perRow()) * HEART_ROW_SPACING;
            context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, drawX, drawY, HEART_SIZE, HEART_SIZE, u, HEART_V, HEART_SIZE, HEART_SIZE, 256, 256);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        matrices.popPose();
    }

}
