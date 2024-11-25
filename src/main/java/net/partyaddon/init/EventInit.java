package net.partyaddon.init;

import com.blamejared.clumps.api.events.ClumpsEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.partyaddon.access.GroupLeaderAccess;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.group.GroupManager;
import net.partyaddon.network.PartyAddonServerPacket;

public class EventInit {

    private static boolean isClumpsLoaded = FabricLoader.getInstance().isModLoaded("clumps");

    public static void init() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            PartyAddonServerPacket.writeS2CSyncGroupManagerPacket(newPlayer, ((GroupManagerAccess) oldPlayer).getGroupManager());
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            PartyAddonServerPacket.writeS2CSyncGroupManagerPacket(newPlayer, ((GroupManagerAccess) oldPlayer).getGroupManager());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((serverPlayNetworkHandler, minecraftServer) -> {
            GroupManager.leaveGroup(serverPlayNetworkHandler.getPlayer(), false);
        });
        if (isClumpsLoaded) {
            ClumpsEvents.VALUE_EVENT.register(event -> {
                int amount = event.getValue();
                PlayerEntity player = event.getPlayer();

                if (ConfigInit.CONFIG.distributeVanillaXP && !((GroupManagerAccess) player).getGroupManager().getGroupPlayerIdList().isEmpty()) {
                    ((GroupLeaderAccess) player.getWorld().getPlayerByUuid(((GroupManagerAccess) player).getGroupManager().getGroupLeaderId())).addLeaderVanillaExperience(amount);
                    event.setValue(0);
                }
                return null;
            });
        }
    }

}
