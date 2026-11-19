package net.partyaddon.init;

import net.libz.registry.TabRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.partyaddon.PartyAddonMain;
import net.partyaddon.screen.PartyScreen;
import net.partyaddon.screen.widget.PartyTab;
import net.partyaddon.util.PartyRenderHelper;

public class RenderInit {

    public static final Identifier PARTY_ADDON_TAB_ICON = PartyAddonMain.identifierOf("textures/gui/tab_icon.png");
    public static final Identifier PARTY_ADDON_BACKGROUND = PartyAddonMain.identifierOf("textures/gui/background.png");
    public static final Identifier PARTY_ADDON_GUI_ICONS = PartyAddonMain.identifierOf("textures/gui/icons.png");
    public static final Identifier PARTY_HUD = PartyAddonMain.identifierOf("party_hud");


    public static void registerTabs() {
        TabRegistry.registerInventoryTab(new PartyTab(PartyScreen.title, PARTY_ADDON_TAB_ICON, 3, PartyScreen.class));
    }

    public static void renderPartyHud(GuiGraphicsExtractor context) {
        PartyRenderHelper.renderPartyHud(context, Minecraft.getInstance());
    }

}
