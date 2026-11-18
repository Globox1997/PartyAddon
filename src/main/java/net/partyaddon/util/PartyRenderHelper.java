package net.partyaddon.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.partyaddon.group.ClientParty;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.init.RenderInit;
import net.partyaddon.network.packet.MemberHealthPacket.MemberHealth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

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

    public static void renderPartyHud(GuiGraphicsExtractor context, Minecraft client) {
        if (client.player == null || client.gui.hud.isHidden() || !ConfigInit.CONFIG.showGroupHud) {
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

            width = Math.max(width, client.font.width(name));
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

        int color = ARGB.white(opacity);

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(ConfigInit.CONFIG.hudPosX, context.guiHeight() / 2.0f + ConfigInit.CONFIG.hudPosY);
        matrices.scale(scale, scale);

        // left border
        context.blit(RenderPipelines.GUI_TEXTURED, RenderInit.PARTY_ADDON_GUI_ICONS, 0, 0, 0, 64, 5, 5, 5, 5, 256, 256, color);
        context.blit(RenderPipelines.GUI_TEXTURED, RenderInit.PARTY_ADDON_GUI_ICONS, 0, 5, 0, 70, 5, height, 5, 5, 256, 256, color);
        context.blit(RenderPipelines.GUI_TEXTURED, RenderInit.PARTY_ADDON_GUI_ICONS, 0, height + 5, 0, 76, 5, 5, 5, 5, 256, 256, color);
        // middle
        context.blit(RenderPipelines.GUI_TEXTURED, RenderInit.PARTY_ADDON_GUI_ICONS, 5, 0, 6, 64, width, 5, 5, 5, 256, 256, color);
        context.blit(RenderPipelines.GUI_TEXTURED, RenderInit.PARTY_ADDON_GUI_ICONS, 5, 5, 6, 70, width, height, 5, 5, 256, 256, color);
        context.blit(RenderPipelines.GUI_TEXTURED, RenderInit.PARTY_ADDON_GUI_ICONS, 5, height + 5, 6, 76, width, 5, 5, 5, 256, 256, color);
        // right border
        context.blit(RenderPipelines.GUI_TEXTURED, RenderInit.PARTY_ADDON_GUI_ICONS, width + 5, 0, 12, 64, 5, 5, 5, 5, 256, 256, color);
        context.blit(RenderPipelines.GUI_TEXTURED, RenderInit.PARTY_ADDON_GUI_ICONS, width + 5, 5, 12, 70, 5, height, 5, 5, 256, 256, color);
        context.blit(RenderPipelines.GUI_TEXTURED, RenderInit.PARTY_ADDON_GUI_ICONS, width + 5, height + 5, 12, 76, 5, 5, 5, 5, 256, 256, color);

        int y = 5;
        for (int i = 0; i < names.size(); i++) {
            context.text(client.font, names.get(i), 5, y, 0xE6C0C0C0, false);
            y += fontHeight + 1;

            HeartLayout heartLayout = heartLayouts.get(i);
            if (heartLayout != null) {
                drawHealthHearts(context, heartLayout, 5, y, color);
                y += heartLayout.height();
            }
            y += PLAYER_SPACING;
        }

        matrices.popMatrix();
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

    private static void drawHealthHearts(GuiGraphicsExtractor context, HeartLayout heartLayout, int x, int y, int color) {
        float maxHealth = heartLayout.memberHealth().maxHealth();
        float health = Mth.clamp(heartLayout.memberHealth().health(), 0.0f, maxHealth);
        int fullHearts = Mth.floor(health / 2.0f);
        boolean halfHeart = (health - fullHearts * 2.0f) >= 1.0f;

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(heartLayout.scale(), heartLayout.scale());

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
            context.blit(RenderPipelines.GUI_TEXTURED, RenderInit.PARTY_ADDON_GUI_ICONS, drawX, drawY, u, HEART_V, HEART_SIZE, HEART_SIZE, HEART_SIZE, HEART_SIZE, 256, 256, color);
        }

        matrices.popMatrix();
    }

}
