package net.partyaddon.init;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.partyaddon.PartyAddonMain;
import net.partyaddon.compat.ClumpsCompat;
import net.partyaddon.group.PartyManager;

public class EventInit {

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((serverPlayNetworkHandler, packetSender, minecraftServer) -> {
            PartyManager.onJoin(serverPlayNetworkHandler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((serverPlayNetworkHandler, minecraftServer) -> {
            PartyManager.onDisconnect(serverPlayNetworkHandler.getPlayer());
        });
        ServerTickEvents.END_SERVER_TICK.register(PartyManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(minecraftServer -> PartyManager.clear());

        if (PartyAddonMain.isClumpsLoaded) {
            ClumpsCompat.init();
        }
    }

}
