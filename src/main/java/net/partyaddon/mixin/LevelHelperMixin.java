package net.partyaddon.mixin;

import net.levelz.level.LevelManager;
import net.levelz.level.Skill;
import net.levelz.util.LevelHelper;
import net.levelz.util.PacketHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.partyaddon.init.ConfigInit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = LevelHelper.class)
public class LevelHelperMixin {
    @Unique
    private static final ThreadLocal<Boolean> inMixin = ThreadLocal.withInitial(() -> false);

    @Inject(method = "updateSkill", at = @At("TAIL"), remap = false)
    private static void updateSkill(ServerPlayerEntity serverPlayerEntity, Skill skill, CallbackInfo ci) {
        if (!ConfigInit.CONFIG.sharedSkillLevels) return;
        if (inMixin.get()) return; //prevent recursive inject

        try {
            inMixin.set(true);
            MinecraftServer server = serverPlayerEntity.getServer();
            if (server == null) return;


            List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList().stream().filter(p -> p.getUuid() != serverPlayerEntity.getUuid()).toList();
            for (ServerPlayerEntity serverPlayerE : players) {
                for (Skill s : LevelManager.SKILLS.values()) {
                    LevelHelper.updateSkill(serverPlayerE, s);
                }
                PacketHelper.syncEnchantments(serverPlayerE);
                PacketHelper.updateSkills(serverPlayerE);
                PacketHelper.updatePlayerSkills(serverPlayerE, null);
                PacketHelper.updateRestrictions(serverPlayerE);
            }
        } finally {
            inMixin.set(false);
        }
    }

}
