package net.partyaddon;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.partyaddon.init.KeyInit;
import net.partyaddon.init.RenderInit;
import net.partyaddon.network.PartyAddonClientPacket;

@Environment(EnvType.CLIENT)
public class PartyAddonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        RenderInit.init();
        PartyAddonClientPacket.init();
        KeyInit.init();
    }

}
