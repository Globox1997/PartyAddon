package net.partyaddon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.partyaddon.init.CommandInit;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.init.EventInit;
import net.partyaddon.network.PartyAddonServerPacket;

public class PartyAddonMain implements ModInitializer {

    public static final boolean isClumpsLoaded = FabricLoader.getInstance().isModLoaded("clumps");

    @Override
    public void onInitialize() {
        ConfigInit.init();
        EventInit.init();
        CommandInit.init();
        PartyAddonServerPacket.init();
    }

    public static ResourceLocation identifierOf(String name) {
        return ResourceLocation.fromNamespaceAndPath("partyaddon", name);
    }
}
