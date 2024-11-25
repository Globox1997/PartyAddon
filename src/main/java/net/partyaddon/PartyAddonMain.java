package net.partyaddon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.partyaddon.init.CommandInit;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.init.EventInit;
import net.partyaddon.network.PartyAddonServerPacket;

public class PartyAddonMain implements ModInitializer {

    public static final boolean isJobsAddonLoaded = FabricLoader.getInstance().isModLoaded("jobsaddon");
    public static final boolean isLevelZLoaded = FabricLoader.getInstance().isModLoaded("levelz");

    @Override
    public void onInitialize() {
        ConfigInit.init();
        EventInit.init();
        CommandInit.init();
        PartyAddonServerPacket.init();
    }

    public static Identifier identifierOf(String name) {
        return Identifier.of("partyaddon", name);
    }
}
