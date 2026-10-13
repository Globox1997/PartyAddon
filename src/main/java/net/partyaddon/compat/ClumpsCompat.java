package net.partyaddon.compat;

import com.blamejared.clumps.api.events.ClumpsEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.partyaddon.group.PartyManager;
import net.partyaddon.init.ConfigInit;

// Separate class so Clumps classes only get loaded when Clumps is installed
public class ClumpsCompat {

    public static void init() {
        ClumpsEvents.VALUE_EVENT.register(event -> {
            if (ConfigInit.CONFIG.distributeVanillaXP && event.getPlayer() instanceof ServerPlayerEntity serverPlayerEntity
                    && PartyManager.shareVanillaExperience(serverPlayerEntity, event.getValue())) {
                event.setValue(0);
            }
            return null;
        });
    }

}
