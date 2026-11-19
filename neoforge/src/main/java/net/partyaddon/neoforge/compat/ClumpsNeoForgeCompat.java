package net.partyaddon.neoforge.compat;

import com.blamejared.clumps.api.events.ValueEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.partyaddon.group.PartyManager;

public class ClumpsNeoForgeCompat {

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ValueEvent.class, event -> {
            if (PartyManager.shareClumpsExperience(event.getPlayer(), event.getValue())) {
                event.setValue(0);
            }
        });
    }

}
