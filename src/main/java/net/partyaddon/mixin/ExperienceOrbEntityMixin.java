package net.partyaddon.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.partyaddon.group.PartyManager;
import net.partyaddon.init.ConfigInit;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbEntityMixin {

    @WrapWithCondition(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"))
    private boolean onPlayerCollisionMixin(Player player, int experience) {
        return !(ConfigInit.CONFIG.distributeVanillaXP && player instanceof ServerPlayer serverPlayerEntity && PartyManager.shareVanillaExperience(serverPlayerEntity, experience));
    }
}
