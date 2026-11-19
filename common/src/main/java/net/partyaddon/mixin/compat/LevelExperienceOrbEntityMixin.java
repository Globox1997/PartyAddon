package net.partyaddon.mixin.compat;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.levelz.access.ServerPlayerSyncAccess;
import net.levelz.entity.LevelExperienceOrbEntity;
import net.minecraft.server.level.ServerPlayer;
import net.partyaddon.group.PartyManager;
import net.partyaddon.init.ConfigInit;

@Mixin(LevelExperienceOrbEntity.class)
public abstract class LevelExperienceOrbEntityMixin {

    @WrapWithCondition(method = "/^lambda\\$playerTouch\\$\\d+$/", at = @At(value = "INVOKE", target = "Lnet/levelz/access/ServerPlayerSyncAccess;addLevelExperience(I)V"), remap = false)
    private static boolean onPlayerCollisionMixin(ServerPlayerSyncAccess instance, int experience) {
        return !(ConfigInit.CONFIG.distributeLevelZXP && instance instanceof ServerPlayer serverPlayerEntity && PartyManager.shareLevelZExperience(serverPlayerEntity, experience));
    }
}
