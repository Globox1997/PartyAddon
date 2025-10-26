package net.partyaddon.mixin;

import net.levelz.access.LevelManagerAccess;
import net.levelz.level.LevelManager;
import net.levelz.level.PlayerSkill;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.group.GroupManager;
import net.partyaddon.init.ConfigInit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Mixin(value = LevelManager.class, remap = false)
public abstract class LevelManagerMixin {
    @Shadow
    public abstract PlayerEntity getPlayerEntity();

    @Unique
    private static final ThreadLocal<Boolean> inMixin = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getPlayerSkills", at = @At("RETURN"), cancellable = true, remap = false)
    private void modifySkillLevel(CallbackInfoReturnable<Map<Integer, PlayerSkill>> cir) {
        if (!ConfigInit.CONFIG.sharedSkillLevels) return;
        if (inMixin.get()) return; // prevent recursive inject
        try {
            inMixin.set(true);
            Map<Integer, PlayerSkill> currentPlayerSkills = cir.getReturnValue();

            GroupManager groupManager = ((GroupManagerAccess) (Object) this.getPlayerEntity()).getGroupManager();
            List<UUID> playerList = groupManager.getGroupPlayerIdList();
            if (playerList.size() < 2) {
                return;
            }
            if (this.getPlayerEntity().getWorld().isClient()) return;
            MinecraftServer server = this.getPlayerEntity().getWorld().getServer();
            if (server == null) return;


            for (ServerPlayerEntity serverPlayerEntity : getPlayersFromUUIDs(server, playerList)) {
                LevelManager otherPlayerManager = ((LevelManagerAccess) serverPlayerEntity).getLevelManager();
                if (otherPlayerManager == null) continue;

                Map<Integer, PlayerSkill> otherPlayerSkills = otherPlayerManager.getPlayerSkills();
                for (Map.Entry<Integer, PlayerSkill> entry : otherPlayerSkills.entrySet()) {

                    int currentPlayerLevel = currentPlayerSkills.get(entry.getKey()).getLevel();
                    int otherPlayerLevel = entry.getValue().getLevel();
                    if (otherPlayerLevel > currentPlayerLevel) {
                        currentPlayerSkills.put(entry.getKey(), entry.getValue());
                    }

                }
            }

            cir.setReturnValue(currentPlayerSkills); // override return value

        } finally {
            inMixin.set(false);
        }
    }

    @Unique
    private List<ServerPlayerEntity> getPlayersFromUUIDs(MinecraftServer server, List<UUID> uuids) {
        return uuids.stream()
                .map(server.getPlayerManager()::getPlayer)
                .filter(Objects::nonNull)
                .filter(player -> !player.getUuid().equals(this.getPlayerEntity().getUuid()))
                .collect(Collectors.toList());
    }
}
