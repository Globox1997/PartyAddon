package net.partyaddon.network;

import net.minecraft.client.Minecraft;
import net.partyaddon.group.ClientParty;
import net.partyaddon.network.packet.*;
import net.partyaddon.platform.Services;
import net.partyaddon.screen.PartyScreen;

import java.util.Optional;
import java.util.UUID;

public class PartyAddonClientPacket {

    public static void handleOpenPartyScreen() {
        Minecraft.getInstance().gui.setScreen(new PartyScreen());
    }

    public static void handleSyncStars(SyncStarsPacket payload) {
        ClientParty.updateStars(payload.uuids());
    }

    public static void handleSyncGroup(SyncGroupPacket payload) {
        ClientParty.updateGroup(payload.availablePlayerIdList(), payload.starPlayerIdList(), payload.groupPlayerIdList(), payload.groupLeaderId().orElse(null));
    }

    public static void handleInvitation(InvitePlayerPacket payload) {
        ClientParty.setInvitationPlayerId(payload.uuid());
    }

    public static void handleDeclineInvitation() {
        ClientParty.setInvitationPlayerId(null);
    }

    public static void handleSyncSettings(SyncSettingsPacket payload) {
        ClientParty.updateSettings(payload.groupSize(), payload.groupXpBonus(), payload.fullGroupBonus());
    }

    public static void handleSyncPendingInvitations(SyncPendingInvitationsPacket payload) {
        ClientParty.updatePendingInvitations(payload.uuids());
    }

    public static void handleMemberHealth(MemberHealthPacket payload) {
        ClientParty.updateMemberHealth(payload.members());
    }

    public static void writeC2SOpenPartyScreenPacket(Minecraft client) {
        Services.network().sendToServer(new PartyScreenPacket());
    }

    public static void writeC2SChangeStarListPacket(UUID entityUuid) {
        Services.network().sendToServer(new ChangeStarPacket(entityUuid));
    }

    public static void writeC2SInvitePlayerToGroupPacket(UUID invitedPlayerId) {
        Services.network().sendToServer(new InvitePlayerPacket(invitedPlayerId));
    }

    public static void writeC2SAcceptInvitationPacket(UUID invitationPlayerId) {
        Services.network().sendToServer(new AcceptInvitePacket(invitationPlayerId));
    }

    public static void writeC2SDeclineInvitationPacket(UUID invitationPlayerId) {
        Services.network().sendToServer(new DeclineInvitePacket(Optional.ofNullable(invitationPlayerId)));
    }

    public static void writeC2SLeaveGroupPacket() {
        Services.network().sendToServer(new LeaveGroupPacket());
    }

    public static void writeC2SKickPlayerPacket(UUID kickPlayerUuid) {
        Services.network().sendToServer(new KickPlayerPacket(kickPlayerUuid));
    }

    public static void writeC2SPromotePlayerPacket(UUID promotePlayerUuid) {
        Services.network().sendToServer(new PromotePlayerPacket(promotePlayerUuid));
    }

}
