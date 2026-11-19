package net.partyaddon.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.partyaddon.PartyAddonMain;
import net.partyaddon.group.PartyManager;
import net.partyaddon.init.CommandInit;
import net.partyaddon.network.PartyAddonServerPacket;
import net.partyaddon.network.packet.AcceptInvitePacket;
import net.partyaddon.network.packet.ChangeStarPacket;
import net.partyaddon.network.packet.DeclineInvitePacket;
import net.partyaddon.network.packet.InvitePlayerPacket;
import net.partyaddon.network.packet.KickPlayerPacket;
import net.partyaddon.network.packet.LeaveGroupPacket;
import net.partyaddon.network.packet.MemberHealthPacket;
import net.partyaddon.network.packet.PartyScreenPacket;
import net.partyaddon.network.packet.PromotePlayerPacket;
import net.partyaddon.network.packet.SyncGroupPacket;
import net.partyaddon.network.packet.SyncPendingInvitationsPacket;
import net.partyaddon.network.packet.SyncSettingsPacket;
import net.partyaddon.network.packet.SyncStarsPacket;
import net.partyaddon.neoforge.compat.ClumpsNeoForgeCompat;

@Mod(PartyAddonMain.MOD_ID)
public class PartyAddonNeoForge {

    public PartyAddonNeoForge(IEventBus modBus) {
        PartyAddonMain.init();
        if (PartyAddonMain.isClumpsLoaded) {
            ClumpsNeoForgeCompat.init();
        }

        modBus.addListener(RegisterPayloadHandlersEvent.class, PartyAddonNeoForge::registerPayloads);

        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                PartyManager.onJoin(serverPlayer);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                PartyManager.onDisconnect(serverPlayer);
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> PartyManager.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> PartyManager.clear());

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> CommandInit.register(event.getDispatcher()));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(PartyScreenPacket.PACKET_ID, PartyScreenPacket.PACKET_CODEC, (payload, context) -> PartyAddonServerPacket.handleOpenPartyScreen((ServerPlayer) context.player()));
        registrar.playBidirectional(InvitePlayerPacket.PACKET_ID, InvitePlayerPacket.PACKET_CODEC, (payload, context) -> PartyAddonServerPacket.handleInvitePlayer((ServerPlayer) context.player(), payload));
        registrar.playBidirectional(DeclineInvitePacket.PACKET_ID, DeclineInvitePacket.PACKET_CODEC, (payload, context) -> PartyAddonServerPacket.handleDeclineInvite((ServerPlayer) context.player(), payload));

        registrar.playToClient(SyncStarsPacket.PACKET_ID, SyncStarsPacket.PACKET_CODEC);
        registrar.playToClient(SyncGroupPacket.PACKET_ID, SyncGroupPacket.PACKET_CODEC);
        registrar.playToClient(SyncSettingsPacket.PACKET_ID, SyncSettingsPacket.PACKET_CODEC);
        registrar.playToClient(MemberHealthPacket.PACKET_ID, MemberHealthPacket.PACKET_CODEC);
        registrar.playToClient(SyncPendingInvitationsPacket.PACKET_ID, SyncPendingInvitationsPacket.PACKET_CODEC);

        registrar.playToServer(ChangeStarPacket.PACKET_ID, ChangeStarPacket.PACKET_CODEC, (payload, context) -> PartyAddonServerPacket.handleChangeStar((ServerPlayer) context.player(), payload));
        registrar.playToServer(AcceptInvitePacket.PACKET_ID, AcceptInvitePacket.PACKET_CODEC, (payload, context) -> PartyAddonServerPacket.handleAcceptInvite((ServerPlayer) context.player(), payload));
        registrar.playToServer(LeaveGroupPacket.PACKET_ID, LeaveGroupPacket.PACKET_CODEC, (payload, context) -> PartyAddonServerPacket.handleLeaveGroup((ServerPlayer) context.player()));
        registrar.playToServer(KickPlayerPacket.PACKET_ID, KickPlayerPacket.PACKET_CODEC, (payload, context) -> PartyAddonServerPacket.handleKickPlayer((ServerPlayer) context.player(), payload));
        registrar.playToServer(PromotePlayerPacket.PACKET_ID, PromotePlayerPacket.PACKET_CODEC, (payload, context) -> PartyAddonServerPacket.handlePromotePlayer((ServerPlayer) context.player(), payload));
    }

}
