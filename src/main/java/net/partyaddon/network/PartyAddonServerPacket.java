package net.partyaddon.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.partyaddon.group.PartyManager;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.network.packet.*;
import net.partyaddon.network.packet.MemberHealthPacket.MemberHealth;
import org.jetbrains.annotations.Nullable;

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
        PayloadTypeRegistry.playS2C().register(SyncSettingsPacket.PACKET_ID, SyncSettingsPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(MemberHealthPacket.PACKET_ID, MemberHealthPacket.PACKET_CODEC);

        PayloadTypeRegistry.playC2S().register(PartyScreenPacket.PACKET_ID, PartyScreenPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(ChangeStarPacket.PACKET_ID, ChangeStarPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(InvitePlayerPacket.PACKET_ID, InvitePlayerPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(DeclineInvitePacket.PACKET_ID, DeclineInvitePacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(AcceptInvitePacket.PACKET_ID, AcceptInvitePacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(LeaveGroupPacket.PACKET_ID, LeaveGroupPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(KickPlayerPacket.PACKET_ID, KickPlayerPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(PromotePlayerPacket.PACKET_ID, PromotePlayerPacket.PACKET_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PartyScreenPacket.PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                PartyManager.syncPlayer(context.player());
                writeS2COpenPartyScreenPacket(context.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ChangeStarPacket.PACKET_ID, (payload, context) -> {
            UUID playerId = payload.uuid();
            context.server().execute(() -> {
                PartyManager.toggleStar(context.player(), playerId);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(InvitePlayerPacket.PACKET_ID, (payload, context) -> {
            UUID invitedPlayerId = payload.uuid();
            context.server().execute(() -> {
                ServerPlayerEntity invited = context.server().getPlayerManager().getPlayer(invitedPlayerId);
                if (invited != null) {
                    PartyManager.invite(context.player(), invited);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(DeclineInvitePacket.PACKET_ID, (payload, context) -> {
            Optional<UUID> invitationPlayerId = payload.uuid();
            context.server().execute(() -> {
                PartyManager.declineInvitation(context.player(), invitationPlayerId.orElse(null));
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(AcceptInvitePacket.PACKET_ID, (payload, context) -> {
            UUID invitationPlayerId = payload.uuid();
            context.server().execute(() -> {
                PartyManager.acceptInvitation(context.player(), invitationPlayerId);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(LeaveGroupPacket.PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                PartyManager.leave(context.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(KickPlayerPacket.PACKET_ID, (payload, context) -> {
            UUID kickPlayerUuid = payload.kickPlayerUuid();
            context.server().execute(() -> {
                PartyManager.kick(context.player(), kickPlayerUuid);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(PromotePlayerPacket.PACKET_ID, (payload, context) -> {
            UUID promotePlayerUuid = payload.uuid();
            context.server().execute(() -> {
                PartyManager.promote(context.player(), promotePlayerUuid);
            });
        });
    }

    public static void writeS2CSyncGroupPacket(ServerPlayerEntity serverPlayerEntity, List<UUID> availablePlayerIds, List<UUID> starPlayerIds, List<UUID> groupPlayerIds, @Nullable UUID groupLeaderId) {
        ServerPlayNetworking.send(serverPlayerEntity,
                new SyncGroupPacket(List.copyOf(availablePlayerIds), List.copyOf(starPlayerIds), List.copyOf(groupPlayerIds), Optional.ofNullable(groupLeaderId)));
    }

    public static void writeS2CSyncStarPlayerListPacket(ServerPlayerEntity serverPlayerEntity, List<UUID> starPlayerIds) {
        ServerPlayNetworking.send(serverPlayerEntity, new SyncStarsPacket(List.copyOf(starPlayerIds)));
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

    public static void writeS2CSyncSettingsPacket(ServerPlayerEntity serverPlayerEntity) {
        ServerPlayNetworking.send(serverPlayerEntity, new SyncSettingsPacket(ConfigInit.CONFIG.groupSize, ConfigInit.CONFIG.groupXpBonus, ConfigInit.CONFIG.fullGroupBonus));
    }

    public static void writeS2CMemberHealthPacket(ServerPlayerEntity serverPlayerEntity, List<MemberHealth> memberHealths) {
        ServerPlayNetworking.send(serverPlayerEntity, new MemberHealthPacket(List.copyOf(memberHealths)));
    }

}
