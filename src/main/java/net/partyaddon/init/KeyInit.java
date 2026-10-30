package net.partyaddon.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.partyaddon.screen.PartyScreen;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class KeyInit {
    public static KeyBinding screenKey = new KeyBinding("key.partyaddon.openpartyscreen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.partyaddon.keybind");

    public static void init() {
        // Registering
        KeyBindingHelper.registerKeyBinding(screenKey);
        // Callback
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (screenKey.wasPressed()) {
                client.setScreen(new PartyScreen());
                return;
            }
        });
    }

}
