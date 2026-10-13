package net.partyaddon.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.partyaddon.group.PartyManager;
import net.partyaddon.init.ConfigInit;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {

    @WrapWithCondition(method = "onPlayerCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addExperience(I)V"))
    private boolean onPlayerCollisionMixin(PlayerEntity player, int experience) {
        return !(ConfigInit.CONFIG.distributeVanillaXP && player instanceof ServerPlayerEntity serverPlayerEntity && PartyManager.shareVanillaExperience(serverPlayerEntity, experience));
    }
}
