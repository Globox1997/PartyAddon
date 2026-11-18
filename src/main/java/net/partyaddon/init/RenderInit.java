package net.partyaddon.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.libz.registry.TabRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.partyaddon.PartyAddonMain;
import net.partyaddon.screen.PartyScreen;
import net.partyaddon.screen.widget.PartyTab;
import net.partyaddon.util.PartyRenderHelper;

@Environment(EnvType.CLIENT)
public class RenderInit {

    public static final ResourceLocation PARTY_ADDON_TAB_ICON = PartyAddonMain.identifierOf("textures/gui/tab_icon.png");
    public static final ResourceLocation PARTY_ADDON_BACKGROUND = PartyAddonMain.identifierOf("textures/gui/background.png");
    public static final ResourceLocation PARTY_ADDON_GUI_ICONS = PartyAddonMain.identifierOf("textures/gui/icons.png");


    public static void init() {
        TabRegistry.registerInventoryTab(new PartyTab(PartyScreen.title, PARTY_ADDON_TAB_ICON, 3, PartyScreen.class));

        HudRenderCallback.EVENT.register((context, delta) -> {
            Minecraft client = Minecraft.getInstance();
            PartyRenderHelper.renderPartyHud(context, client);
        });
    }

}
