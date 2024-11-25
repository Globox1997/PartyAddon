package net.partyaddon.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.partyaddon.access.GroupLeaderAccess;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.init.ConfigInit;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin extends Entity {

    @Shadow
    private int pickingCount = 1;

    public ExperienceOrbEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "onPlayerCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addExperience(I)V"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onPlayerCollisionMixin(PlayerEntity player, CallbackInfo info, ServerPlayerEntity serverPlayerEntity, int i) {
        if (ConfigInit.CONFIG.distributeVanillaXP && !((GroupManagerAccess) player).getGroupManager().getGroupPlayerIdList().isEmpty()
                && ((GroupManagerAccess) player).getGroupManager().getGroupLeaderId() != null
                && player.getWorld().getPlayerByUuid(((GroupManagerAccess) player).getGroupManager().getGroupLeaderId()) != null) {
            ((GroupLeaderAccess) player.getWorld().getPlayerByUuid(((GroupManagerAccess) player).getGroupManager().getGroupLeaderId())).addLeaderVanillaExperience(i);
            --this.pickingCount;
            if (this.pickingCount == 0) {
                this.discard();
            }
            info.cancel();
        }
    }
}
