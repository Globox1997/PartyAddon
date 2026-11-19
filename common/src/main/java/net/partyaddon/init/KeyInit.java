package net.partyaddon.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.partyaddon.PartyAddonMain;
import net.partyaddon.network.PartyAddonClientPacket;

public class KeyInit {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(PartyAddonMain.identifierOf("keybind"));
    public static KeyMapping screenKey = new KeyMapping("key.partyaddon.openpartyscreen", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY);

    public static void onClientTick(Minecraft client) {
        if (screenKey.consumeClick()) {
            PartyAddonClientPacket.writeC2SOpenPartyScreenPacket(client);
        }
    }

}
