package net.partyaddon.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.libz.registry.TabRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.partyaddon.PartyAddonMain;
import net.partyaddon.screen.PartyScreen;
import net.partyaddon.screen.widget.PartyTab;
import net.partyaddon.util.PartyRenderHelper;

@Environment(EnvType.CLIENT)
public class RenderInit {

    public static final Identifier PARTY_ADDON_TAB_ICON = PartyAddonMain.identifierOf("textures/gui/tab_icon.png");
    public static final Identifier PARTY_ADDON_BACKGROUND = PartyAddonMain.identifierOf("textures/gui/background.png");
    public static final Identifier PARTY_ADDON_GUI_ICONS = PartyAddonMain.identifierOf("textures/gui/icons.png");
    public static final Identifier PARTY_HUD = PartyAddonMain.identifierOf("party_hud");


    public static void init() {
        TabRegistry.registerInventoryTab(new PartyTab(PartyScreen.title, PARTY_ADDON_TAB_ICON, 3, PartyScreen.class));

        HudElementRegistry.addLast(PARTY_HUD, (context, deltaTracker) -> {
            Minecraft client = Minecraft.getInstance();
            PartyRenderHelper.renderPartyHud(context, client);
        });
    }

}
