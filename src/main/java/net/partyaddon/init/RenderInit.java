package net.partyaddon.init;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.libz.registry.TabRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.partyaddon.PartyAddonMain;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.screen.PartyScreen;
import net.partyaddon.screen.widget.PartyTab;
import net.partyaddon.util.NameHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class RenderInit {

    public static final Identifier PARTY_ADDON_GUI_ICONS = PartyAddonMain.identifierOf("textures/gui/icons.png");
    public static final Identifier PARTY_ADDON_BACKGROUND = PartyAddonMain.identifierOf("textures/gui/background.png");
    public static final Identifier PARTY_ADDON_TAB_ICON = PartyAddonMain.identifierOf("textures/gui/tab_icon.png");

    public static void init() {
        TabRegistry.registerInventoryTab(new PartyTab(PartyScreen.title, PARTY_ADDON_TAB_ICON, 3, PartyScreen.class));

        HudRenderCallback.EVENT.register((context, delta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && ConfigInit.CONFIG.showGroupHud && !((GroupManagerAccess) client.player).getGroupManager().getGroupPlayerIdList().isEmpty()) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, ConfigInit.CONFIG.hudOpacity);
                RenderSystem.enableBlend();
                // RenderSystem.defaultBlendFunc();

                List<UUID> groupPlayerIdList = ((GroupManagerAccess) client.player).getGroupManager().getGroupPlayerIdList();
                List<Text> groupPlayerNames = new ArrayList<Text>();

                int maxWidth = ConfigInit.CONFIG.hudMaxWidth;
                int newMaxWidth = 0;
                for (int i = 0; i < groupPlayerIdList.size(); i++) {
                    Text playerName = NameHelper.getPlayerName(client, groupPlayerIdList.get(i), maxWidth);
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

                // int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight
                int scaledHeight = context.getScaledWindowHeight();
                // left border
                context.drawTexture(PARTY_ADDON_GUI_ICONS, 0 + otherXPos, scaledHeight / 2 + otherYPos, 5, 5, 0, 64, 5, 5, 256, 256);
                context.drawTexture(PARTY_ADDON_GUI_ICONS, 0 + otherXPos, scaledHeight / 2 + 5 + otherYPos, 5, height, 0, 70, 5, 5, 256, 256);
                context.drawTexture(PARTY_ADDON_GUI_ICONS, 0 + otherXPos, scaledHeight / 2 + height + 5 + otherYPos, 5, 5, 0, 76, 5, 5, 256, 256);
                // middle
                context.drawTexture(PARTY_ADDON_GUI_ICONS, 5 + otherXPos, scaledHeight / 2 + otherYPos, maxWidth, 5, 6, 64, 5, 5, 256, 256);
                context.drawTexture(PARTY_ADDON_GUI_ICONS, 5 + otherXPos, scaledHeight / 2 + 5 + otherYPos, maxWidth, height, 6, 70, 5, 5, 256, 256);
                context.drawTexture(PARTY_ADDON_GUI_ICONS, 5 + otherXPos, scaledHeight / 2 + height + 5 + otherYPos, maxWidth, 5, 6, 76, 5, 5, 256, 256);
                // right border
                context.drawTexture(PARTY_ADDON_GUI_ICONS, maxWidth + 5 + otherXPos, scaledHeight / 2 + otherYPos, 5, 5, 12, 64, 5, 5, 256, 256);
                context.drawTexture(PARTY_ADDON_GUI_ICONS, maxWidth + 5 + otherXPos, scaledHeight / 2 + 5 + otherYPos, 5, height, 12, 70, 5, 5, 256, 256);
                context.drawTexture(PARTY_ADDON_GUI_ICONS, maxWidth + 5 + otherXPos, scaledHeight / 2 + height + 5 + otherYPos, 5, 5, 12, 76, 5, 5, 256, 256);

                // this.client.textRenderer.draw(matrices, Text.translatable("text.partyaddon.hud.group"), 5, this.scaledHeight / 2 + 5, 0xE6C0C0C0);
                // int o = 13;
                int o = 0;
                for (int i = 0; i < groupPlayerNames.size(); i++) {
                    // this.client.textRenderer.getWidth(this.client.player.getName().getString());
                    // this.client.textRenderer.draw(matrices,
                    // NameHelper.getPlayerName(client.world, client.textRenderer, ((GroupManagerAccess) this.client.player).getGroupManager().getGroupPlayerIdList().get(i)), 5,
                    // this.scaledHeight / 2 + 5 + o, 0xE6C0C0C0);
                    // String playerName = world.getEntityById(playerId).getName().getString();
                    // if (textRenderer.getWidth(playerName) > 55)
                    // playerName = playerName.substring(0, 9) + "..";

                    context.drawText(client.textRenderer, groupPlayerNames.get(i), 5 + otherXPos, scaledHeight / 2 + 5 + o + otherYPos, 0xE6C0C0C0, false);
                    o += 13;
                }
                RenderSystem.disableBlend();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                // System.out.println(scaledWidth);
            }
        });
    }

}
