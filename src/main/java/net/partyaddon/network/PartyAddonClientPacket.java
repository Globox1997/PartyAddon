package net.partyaddon.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.group.GroupManager;
import net.partyaddon.init.CompatInit;
import net.partyaddon.network.packet.*;
import net.partyaddon.screen.PartyScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class PartyAddonClientPacket {

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(PartyScreenPacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                context.client().setScreen(new PartyScreen());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncStarsPacket.PACKET_ID, (payload, context) -> {
            List<UUID> starPlayerIdList = payload.uuids();
            context.client().execute(() -> {
                GroupManager groupManager = ((GroupManagerAccess) context.player()).getGroupManager();
                groupManager.updateStarPlayerIdList(starPlayerIdList);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncGroupPacket.PACKET_ID, (payload, context) -> {
            List<UUID> availablePlayerIdList = payload.availablePlayerIdList();
            List<UUID> starPlayerIdList = payload.starPlayerIdList();
            List<UUID> groupPlayerIdList = payload.groupPlayerIdList();
            Optional<UUID> groupLeaderId = payload.groupLeaderId();
            context.client().execute(() -> {
                GroupManager groupManager = ((GroupManagerAccess) context.player()).getGroupManager();
                groupManager.setAvailablePlayerIdList(availablePlayerIdList);
                groupManager.updateStarPlayerIdList(starPlayerIdList);
                groupManager.updatePlayerGroupIdList(groupPlayerIdList, groupLeaderId.orElse(null));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(InvitePlayerPacket.PACKET_ID, (payload, context) -> {
            UUID playerId = payload.uuid();
            context.client().execute(() -> {
                ((GroupManagerAccess) context.player()).getGroupManager().invitePlayerToGroup(playerId);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(DeclineInvitePacket.PACKET_ID, (payload, context) -> {
            // int playerId = buf.readInt();
            // maybe use playerId to send feedback?
            context.client().execute(() -> {
                ((GroupManagerAccess) context.player()).getGroupManager().declineInvitation();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(MapPacket.PACKET_ID, (payload, context) -> {
            List<UUID> groupPlayerUUIDs = payload.groupPlayerUUIDs();
            List<BlockPos> groupPlayerBlockPoses = payload.groupPlayerBlockPoses();
            List<Float> groupPlayerYaws = payload.groupPlayerYaws();
            context.client().execute(() -> {
                CompatInit.syncGroupToMap(context.client(), groupPlayerUUIDs, groupPlayerBlockPoses, groupPlayerYaws);
            });
        });
    }

    // screen
    public static void writeC2SOpenPartyScreenPacket(MinecraftClient client) {
        ClientPlayNetworking.send(new PartyScreenPacket());
    }

    public static void writeC2SChangeStarListPacket(UUID entityUuid) {
        ClientPlayNetworking.send(new ChangeStarPacket(entityUuid));
    }

    public static void writeC2SInvitePlayerToGroupPacket(UUID invitedPlayerId) {
        ClientPlayNetworking.send(new InvitePlayerPacket(invitedPlayerId));
    }

    public static void writeC2SAcceptInvitationPacket(UUID invitationPlayerId) {
        ClientPlayNetworking.send(new AcceptInvitePacket(invitationPlayerId));
    }

    public static void writeC2SDeclineInvitationPacket(UUID entityId) {
        ClientPlayNetworking.send(new DeclineInvitePacket(entityId));
    }

    public static void writeC2SLeaveGroupPacket() {
        ClientPlayNetworking.send(new LeaveGroupPacket());
    }

    public static void writeC2SKickPlayerPacket(UUID groupLeaderUuid, UUID kickPlayerUuid) {
        ClientPlayNetworking.send(new KickPlayerPacket(groupLeaderUuid, kickPlayerUuid));
    }

    public static void writeC2SSyncGroupMemberPacket(MinecraftClient client) {
        ClientPlayNetworking.send(new SyncGroupPacket(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Optional.empty()));
    }

    public static void writeC2SMapPacket() {
        ClientPlayNetworking.send(new MapPacket(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
    }

}
