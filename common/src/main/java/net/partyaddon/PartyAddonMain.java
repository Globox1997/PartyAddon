package net.partyaddon;

import net.minecraft.resources.Identifier;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.platform.Services;

public class PartyAddonMain {

    public static final String MOD_ID = "partyaddon";

    public static final boolean isClumpsLoaded = Services.PLATFORM.isModLoaded("clumps");

    public static void init() {
        ConfigInit.init();
    }

    public static Identifier identifierOf(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }
}
