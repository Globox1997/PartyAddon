package net.partyaddon.network;

import net.minecraft.server.level.ServerPlayer;
import net.partyaddon.group.PartyManager;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.network.packet.*;
import net.partyaddon.network.packet.MemberHealthPacket.MemberHealth;
import net.partyaddon.platform.Services;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PartyAddonServerPacket {

    public static void handleOpenPartyScreen(ServerPlayer player) {
        PartyManager.syncPlayer(player);
        writeS2COpenPartyScreenPacket(player);
    }

    public static void handleChangeStar(ServerPlayer player, ChangeStarPacket payload) {
        PartyManager.toggleStar(player, payload.uuid());
    }

    public static void handleInvitePlayer(ServerPlayer player, InvitePlayerPacket payload) {
        ServerPlayer invited = player.level().getServer().getPlayerList().getPlayer(payload.uuid());
        if (invited != null) {
            PartyManager.invite(player, invited);
        } else {
            PartyManager.syncOutgoingInvitations(player);
        }
    }

    public static void handleDeclineInvite(ServerPlayer player, DeclineInvitePacket payload) {
        PartyManager.declineInvitation(player, payload.uuid().orElse(null));
    }

    public static void handleAcceptInvite(ServerPlayer player, AcceptInvitePacket payload) {
        PartyManager.acceptInvitation(player, payload.uuid());
    }

    public static void handleLeaveGroup(ServerPlayer player) {
        PartyManager.leave(player);
    }

    public static void handleKickPlayer(ServerPlayer player, KickPlayerPacket payload) {
        PartyManager.kick(player, payload.kickPlayerUuid());
    }

    public static void handlePromotePlayer(ServerPlayer player, PromotePlayerPacket payload) {
        PartyManager.promote(player, payload.uuid());
    }

    public static void writeS2CSyncGroupPacket(ServerPlayer serverPlayerEntity, List<UUID> availablePlayerIds, List<UUID> starPlayerIds, List<UUID> groupPlayerIds, @Nullable UUID groupLeaderId) {
        Services.network().sendToPlayer(serverPlayerEntity,
                new SyncGroupPacket(List.copyOf(availablePlayerIds), List.copyOf(starPlayerIds), List.copyOf(groupPlayerIds), Optional.ofNullable(groupLeaderId)));
    }

    public static void writeS2CSyncStarPlayerListPacket(ServerPlayer serverPlayerEntity, List<UUID> starPlayerIds) {
        Services.network().sendToPlayer(serverPlayerEntity, new SyncStarsPacket(List.copyOf(starPlayerIds)));
    }

    public static void writeS2CSyncInvitationPacket(ServerPlayer serverPlayerEntity, UUID invitationPlayerId) {
        Services.network().sendToPlayer(serverPlayerEntity, new InvitePlayerPacket(invitationPlayerId));
    }

    public static void writeS2CSyncDeclinePacket(ServerPlayer serverPlayerEntity, @Nullable UUID playerId) {
        Services.network().sendToPlayer(serverPlayerEntity, new DeclineInvitePacket(Optional.ofNullable(playerId)));
    }

    public static void writeS2COpenPartyScreenPacket(ServerPlayer serverPlayerEntity) {
        Services.network().sendToPlayer(serverPlayerEntity, new PartyScreenPacket());
    }

    public static void writeS2CSyncSettingsPacket(ServerPlayer serverPlayerEntity) {
        Services.network().sendToPlayer(serverPlayerEntity, new SyncSettingsPacket(ConfigInit.CONFIG.groupSize, ConfigInit.CONFIG.groupXpBonus, ConfigInit.CONFIG.fullGroupBonus));
    }

    public static void writeS2CSyncPendingInvitationsPacket(ServerPlayer serverPlayerEntity, List<UUID> invitedPlayerIds) {
        Services.network().sendToPlayer(serverPlayerEntity, new SyncPendingInvitationsPacket(List.copyOf(invitedPlayerIds)));
    }

    public static void writeS2CMemberHealthPacket(ServerPlayer serverPlayerEntity, List<MemberHealth> memberHealths) {
        Services.network().sendToPlayer(serverPlayerEntity, new MemberHealthPacket(List.copyOf(memberHealths)));
    }

}
