package net.partyaddon.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.partyaddon.network.PartyAddonClientPacket;
import xaero.map.controls.ControlsHandler;
import xaero.map.gui.GuiMap;

@Environment(EnvType.CLIENT)
@Mixin(ControlsHandler.class)
public class ControlsHandlerMixin {

//    @Inject(method = "keyDown", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", ordinal = 0, shift = Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
//    private void keyDownMixin(KeyBinding kb, boolean tickEnd, boolean isRepeat, CallbackInfo info, MinecraftClient mc) {
//        if (mc.currentScreen != null && mc.currentScreen instanceof GuiMap) {
//            PartyAddonClientPacket.writeC2SSyncGroupMemberPacket(mc);
//        }
//    }
}
