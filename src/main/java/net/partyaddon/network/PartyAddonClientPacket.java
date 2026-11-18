package net.partyaddon.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.partyaddon.group.ClientParty;
import net.partyaddon.network.packet.*;
import net.partyaddon.screen.PartyScreen;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class PartyAddonClientPacket {

    public static void init() {
        ClientPlayConnectionEvents.INIT.register((handler, client) -> client.execute(ClientParty::reset));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ClientParty::reset));

        ClientPlayNetworking.registerGlobalReceiver(PartyScreenPacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                context.client().gui.setScreen(new PartyScreen());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncStarsPacket.PACKET_ID, (payload, context) -> {
            List<UUID> starPlayerIdList = payload.uuids();
            context.client().execute(() -> {
                ClientParty.updateStars(starPlayerIdList);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncGroupPacket.PACKET_ID, (payload, context) -> {
            List<UUID> availablePlayerIdList = payload.availablePlayerIdList();
            List<UUID> starPlayerIdList = payload.starPlayerIdList();
            List<UUID> groupPlayerIdList = payload.groupPlayerIdList();
            Optional<UUID> groupLeaderId = payload.groupLeaderId();
            context.client().execute(() -> {
                ClientParty.updateGroup(availablePlayerIdList, starPlayerIdList, groupPlayerIdList, groupLeaderId.orElse(null));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(InvitePlayerPacket.PACKET_ID, (payload, context) -> {
            UUID playerId = payload.uuid();
            context.client().execute(() -> {
                ClientParty.setInvitationPlayerId(playerId);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(DeclineInvitePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientParty.setInvitationPlayerId(null);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncSettingsPacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientParty.updateSettings(payload.groupSize(), payload.groupXpBonus(), payload.fullGroupBonus());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncPendingInvitationsPacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientParty.updatePendingInvitations(payload.uuids());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(MemberHealthPacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientParty.updateMemberHealth(payload.members());
            });
        });
    }

    // screen
    public static void writeC2SOpenPartyScreenPacket(Minecraft client) {
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

    public static void writeC2SDeclineInvitationPacket(UUID invitationPlayerId) {
        ClientPlayNetworking.send(new DeclineInvitePacket(Optional.ofNullable(invitationPlayerId)));
    }

    public static void writeC2SLeaveGroupPacket() {
        ClientPlayNetworking.send(new LeaveGroupPacket());
    }

    public static void writeC2SKickPlayerPacket(UUID kickPlayerUuid) {
        ClientPlayNetworking.send(new KickPlayerPacket(kickPlayerUuid));
    }

    public static void writeC2SPromotePlayerPacket(UUID promotePlayerUuid) {
        ClientPlayNetworking.send(new PromotePlayerPacket(promotePlayerUuid));
    }

}
