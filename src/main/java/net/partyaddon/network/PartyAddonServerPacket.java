package net.partyaddon.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.group.GroupManager;
import net.partyaddon.network.packet.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PartyAddonServerPacket {

    public static void init() {
        PayloadTypeRegistry.playS2C().register(PartyScreenPacket.PACKET_ID, PartyScreenPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncStarsPacket.PACKET_ID, SyncStarsPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGroupPacket.PACKET_ID, SyncGroupPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(InvitePlayerPacket.PACKET_ID, InvitePlayerPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(DeclineInvitePacket.PACKET_ID, DeclineInvitePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(MapPacket.PACKET_ID, MapPacket.PACKET_CODEC);

        PayloadTypeRegistry.playC2S().register(PartyScreenPacket.PACKET_ID, PartyScreenPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(ChangeStarPacket.PACKET_ID, ChangeStarPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(InvitePlayerPacket.PACKET_ID, InvitePlayerPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(DeclineInvitePacket.PACKET_ID, DeclineInvitePacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(AcceptInvitePacket.PACKET_ID, AcceptInvitePacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(LeaveGroupPacket.PACKET_ID, LeaveGroupPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(KickPlayerPacket.PACKET_ID, KickPlayerPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(MapPacket.PACKET_ID, MapPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(SyncGroupPacket.PACKET_ID, SyncGroupPacket.PACKET_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PartyScreenPacket.PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                writeS2CSyncGroupManagerPacket(context.player(), ((GroupManagerAccess) context.player()).getGroupManager());
                writeS2COpenPartyScreenPacket(context.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ChangeStarPacket.PACKET_ID, (payload, context) -> {
            UUID playerId = payload.uuid();
            context.server().execute(() -> {
                GroupManager groupManager = ((GroupManagerAccess) context.player()).getGroupManager();
                if (groupManager.getStarPlayerIdList().contains(playerId)) {
                    groupManager.removePlayerStar(playerId);
                } else {
                    groupManager.addPlayerStar(playerId);
                }
                // Sync star player list
                writeS2CSyncStarPlayerListPacket(context.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(InvitePlayerPacket.PACKET_ID, (payload, context) -> {
            UUID invitedPlayerId = payload.uuid();
            context.server().execute(() -> {
                if (context.player().getWorld().getPlayerByUuid(invitedPlayerId) != null && context.player().getWorld().getPlayerByUuid(invitedPlayerId) instanceof ServerPlayerEntity invitedServerPlayerEntity) {
                    invitedServerPlayerEntity.sendMessage(Text.translatable("text.partyaddon.invitation", context.player().getName().getString()));
                    ((GroupManagerAccess) invitedServerPlayerEntity).getGroupManager().invitePlayerToGroup(context.player().getUuid());
                    // Sync invitation
                    writeS2CSyncInvitationPacket(invitedServerPlayerEntity, context.player().getUuid());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(DeclineInvitePacket.PACKET_ID, (payload, context) -> {
            Optional<UUID> invitationPlayerId = payload.uuid();
            context.server().execute(() -> {
                if (invitationPlayerId.isPresent()) {
                    writeS2CSyncDeclinePacket(context.player(), invitationPlayerId.get());
                    ((GroupManagerAccess) context.player()).getGroupManager().declineInvitation();

                    if (context.player().getWorld().getPlayerByUuid(invitationPlayerId.get()) instanceof ServerPlayerEntity serverPlayerEntity) {
                        serverPlayerEntity.sendMessage(Text.translatable("text.partyaddon.declined_invitation", context.player().getName().getString()));
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(AcceptInvitePacket.PACKET_ID, (payload, context) -> {
            UUID invitationPlayerId = payload.uuid();
            context.server().execute(() -> {
                GroupManager.tryJoinGroup(context.player(), invitationPlayerId);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(LeaveGroupPacket.PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                GroupManager.leaveGroup(context.player(), false);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(KickPlayerPacket.PACKET_ID, (payload, context) -> {
            UUID groupLeaderUuid = payload.groupLeaderUuid();
            UUID kickPlayerUuid = payload.kickPlayerUuid();
            context.server().execute(() -> {
                if (context.player().getWorld().getPlayerByUuid(kickPlayerUuid) != null && context.player().getWorld().getPlayerByUuid(kickPlayerUuid) instanceof ServerPlayerEntity
                        && context.player().getWorld().getPlayerByUuid(groupLeaderUuid) != null && context.player().getWorld().getPlayerByUuid(groupLeaderUuid) instanceof ServerPlayerEntity
                        && ((GroupManagerAccess) context.player().getWorld().getPlayerByUuid(groupLeaderUuid)).getGroupManager().isGroupLeader()) {
                    GroupManager.leaveGroup((ServerPlayerEntity) context.player().getWorld().getPlayerByUuid(kickPlayerUuid), true);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(MapPacket.PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                writeS2CMapCompatPacket(context.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SyncGroupPacket.PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                writeS2CSyncGroupManagerPacket(context.player(), ((GroupManagerAccess) context.player()).getGroupManager());
            });
        });
    }

    public static void writeS2CSyncGroupManagerPacket(ServerPlayerEntity serverPlayerEntity, GroupManager groupManager) {
        List<UUID> availablePlayerIdList = new ArrayList<>();
        for (int i = 0; i < serverPlayerEntity.getServer().getPlayerManager().getPlayerList().size(); i++) {
            availablePlayerIdList.add(serverPlayerEntity.getServer().getPlayerManager().getPlayerList().get(i).getUuid());
        }
        availablePlayerIdList.remove(serverPlayerEntity.getUuid());
        ServerPlayNetworking.send(serverPlayerEntity, new SyncGroupPacket(availablePlayerIdList, groupManager.getStarPlayerIdList(), groupManager.getGroupPlayerIdList(), Optional.ofNullable(groupManager.getGroupLeaderId())));
    }

    public static void writeS2CSyncStarPlayerListPacket(ServerPlayerEntity serverPlayerEntity) {
        ServerPlayNetworking.send(serverPlayerEntity, new SyncStarsPacket(((GroupManagerAccess) serverPlayerEntity).getGroupManager().getStarPlayerIdList()));
    }

    public static void writeS2CSyncInvitationPacket(ServerPlayerEntity serverPlayerEntity, UUID invitationPlayerId) {
        ServerPlayNetworking.send(serverPlayerEntity, new InvitePlayerPacket(invitationPlayerId));
    }

    public static void writeS2CSyncDeclinePacket(ServerPlayerEntity serverPlayerEntity, @Nullable UUID playerId) {
        ServerPlayNetworking.send(serverPlayerEntity, new DeclineInvitePacket(Optional.ofNullable(playerId)));
    }

    public static void writeS2COpenPartyScreenPacket(ServerPlayerEntity serverPlayerEntity) {
        ServerPlayNetworking.send(serverPlayerEntity, new PartyScreenPacket());
    }

    public static void writeS2CMapCompatPacket(ServerPlayerEntity serverPlayerEntity) {
        GroupManager groupManager = ((GroupManagerAccess) serverPlayerEntity).getGroupManager();

        List<UUID> groupPlayerUUIDs = new ArrayList<>();
        List<BlockPos> groupPlayerBlockPoses = new ArrayList<>();
        List<Float> groupPlayerYaws = new ArrayList<>();

        for (int i = 0; i < groupManager.getGroupPlayerIdList().size(); i++) {
            if (serverPlayerEntity.getWorld().getPlayerByUuid(groupManager.getGroupPlayerIdList().get(i)) == null || serverPlayerEntity.getUuid().equals(groupManager.getGroupPlayerIdList().get(i))) {
                continue;
            }
            groupPlayerUUIDs.add(groupManager.getGroupPlayerIdList().get(i));
            groupPlayerBlockPoses.add(serverPlayerEntity.getWorld().getPlayerByUuid(groupManager.getGroupPlayerIdList().get(i)).getBlockPos());
            groupPlayerYaws.add(serverPlayerEntity.getWorld().getPlayerByUuid(groupManager.getGroupPlayerIdList().get(i)).getYaw());
        }
        ServerPlayNetworking.send(serverPlayerEntity, new MapPacket(groupPlayerUUIDs, groupPlayerBlockPoses, groupPlayerYaws));
    }

}
