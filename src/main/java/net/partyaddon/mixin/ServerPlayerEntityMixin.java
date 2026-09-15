package net.partyaddon.mixin;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.partyaddon.access.GroupLeaderAccess;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.network.PartyAddonServerPacket;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements GroupLeaderAccess {

    @Shadow
    private int syncedExperience;

    @Unique
    private int collectedVanillaXP = 0;

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void copyFromMixin(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo info) {
        ((GroupManagerAccess) this).setGroupManager(((GroupManagerAccess) oldPlayer).getGroupManager());
        PartyAddonServerPacket.writeS2CSyncGroupManagerPacket((ServerPlayerEntity) (Object) this, ((GroupManagerAccess) oldPlayer).getGroupManager());
    }

    @Override
    public void addLeaderVanillaExperience(int experience) {
        this.collectedVanillaXP += experience;
        List<UUID> groupPlayerIdList = ((GroupManagerAccess) this).getGroupManager().getGroupPlayerIdList();
        if (!groupPlayerIdList.isEmpty()) {
            if (groupPlayerIdList.size() <= this.collectedVanillaXP) {
                int sharingExperience = this.collectedVanillaXP / groupPlayerIdList.size();
                for (int i = 0; i < groupPlayerIdList.size(); i++) {
                    if (this.getWorld().getPlayerByUuid(groupPlayerIdList.get(i)) == null) {
                        continue;
                    }
                    ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) this.getWorld().getPlayerByUuid(groupPlayerIdList.get(i));
                    ((GroupLeaderAccess) serverPlayerEntity).addVanillaExperience(sharingExperience);
                }
                this.collectedVanillaXP -= (sharingExperience * groupPlayerIdList.size());
            }
        }
    }

    @Override
    public void addVanillaExperience(int experience) {
        super.addExperience(experience);
        this.syncedExperience = -1;
    }

}
