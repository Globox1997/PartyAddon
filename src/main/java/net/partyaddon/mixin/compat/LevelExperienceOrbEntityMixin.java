package net.partyaddon.mixin.compat;

import java.util.Map;

import net.levelz.access.ServerPlayerSyncAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.levelz.entity.LevelExperienceOrbEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.partyaddon.access.GroupLeaderAccess;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.init.ConfigInit;

@Mixin(LevelExperienceOrbEntity.class)
public abstract class LevelExperienceOrbEntityMixin extends Entity {

    public LevelExperienceOrbEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    // lambda injection
    @Redirect(method = "lambda$onPlayerCollision$4", at = @At(value = "INVOKE", target = "Lnet/levelz/access/ServerPlayerSyncAccess;addLevelExperience(I)V"))
    private static void onPlayerCollisionMixin(ServerPlayerSyncAccess instance, int oldValue, PlayerEntity player, Integer value, Integer amount) {
        if (ConfigInit.CONFIG.distributeLevelZXP && !((GroupManagerAccess) player).getGroupManager().getGroupPlayerIdList().isEmpty()
                && ((GroupManagerAccess) player).getGroupManager().getGroupLeaderId() != null
                && player.getWorld().getPlayerByUuid(((GroupManagerAccess) player).getGroupManager().getGroupLeaderId()) != null) {
            ((GroupLeaderAccess) player.getWorld().getPlayerByUuid(((GroupManagerAccess) player).getGroupManager().getGroupLeaderId())).addLeaderLevelZExperience(value * amount);
        } else {
            instance.addLevelExperience(value * amount);
        }
    }

    @Shadow
    private Map<Integer, Integer> getClumpedMap() {
        return null;
    }
}
