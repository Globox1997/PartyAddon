package net.partyaddon.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.partyaddon.PartyAddonMain;
import net.partyaddon.network.PartyAddonClientPacket;

@Environment(EnvType.CLIENT)
public class KeyInit {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(PartyAddonMain.identifierOf("keybind"));
    public static KeyMapping screenKey = new KeyMapping("key.partyaddon.openpartyscreen", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY);

    public static void init() {
        KeyMapping.Category.register(CATEGORY.id());
        KeyMappingHelper.registerKeyMapping(screenKey);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (screenKey.consumeClick()) {
                PartyAddonClientPacket.writeC2SOpenPartyScreenPacket(client);
                return;
            }
        });
    }

}
