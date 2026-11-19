package net.partyaddon.compat;

import com.blamejared.clumps.api.events.ClumpsEvents;
import net.partyaddon.group.PartyManager;

// Separate class so Clumps classes only get loaded when Clumps is installed
public class ClumpsCompat {

    public static void init() {
        ClumpsEvents.VALUE_EVENT.register(event -> {
            if (PartyManager.shareClumpsExperience(event.getPlayer(), event.getValue())) {
                event.setValue(0);
            }
            return null;
        });
    }

}
