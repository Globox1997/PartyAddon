package net.partyaddon.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.partyaddon.network.PartyAddonClientPacket;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class KeyInit {
    public static KeyMapping screenKey = new KeyMapping("key.partyaddon.openpartyscreen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.partyaddon.keybind");

    public static void init() {
        // Registering
        KeyBindingHelper.registerKeyBinding(screenKey);
        // Callback
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (screenKey.consumeClick()) {
                PartyAddonClientPacket.writeC2SOpenPartyScreenPacket(client);
                return;
            }
        });
    }

}
