package net.partyaddon.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.partyaddon.PartyAddonMain;
import net.partyaddon.compat.ClumpsCompat;
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

public class PartyAddonFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        PartyAddonMain.init();
        if (PartyAddonMain.isClumpsLoaded) {
            ClumpsCompat.init();
        }

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PartyManager.onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PartyManager.onDisconnect(handler.getPlayer()));
        ServerTickEvents.END_SERVER_TICK.register(PartyManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PartyManager.clear());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CommandInit.register(dispatcher));

        registerPayloads();
    }

    private static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(PartyScreenPacket.PACKET_ID, PartyScreenPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncStarsPacket.PACKET_ID, SyncStarsPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncGroupPacket.PACKET_ID, SyncGroupPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InvitePlayerPacket.PACKET_ID, InvitePlayerPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DeclineInvitePacket.PACKET_ID, DeclineInvitePacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncSettingsPacket.PACKET_ID, SyncSettingsPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MemberHealthPacket.PACKET_ID, MemberHealthPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncPendingInvitationsPacket.PACKET_ID, SyncPendingInvitationsPacket.PACKET_CODEC);

        PayloadTypeRegistry.serverboundPlay().register(PartyScreenPacket.PACKET_ID, PartyScreenPacket.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChangeStarPacket.PACKET_ID, ChangeStarPacket.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(InvitePlayerPacket.PACKET_ID, InvitePlayerPacket.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DeclineInvitePacket.PACKET_ID, DeclineInvitePacket.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AcceptInvitePacket.PACKET_ID, AcceptInvitePacket.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LeaveGroupPacket.PACKET_ID, LeaveGroupPacket.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(KickPlayerPacket.PACKET_ID, KickPlayerPacket.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PromotePlayerPacket.PACKET_ID, PromotePlayerPacket.PACKET_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PartyScreenPacket.PACKET_ID, (payload, context) -> context.server().execute(() -> PartyAddonServerPacket.handleOpenPartyScreen(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(ChangeStarPacket.PACKET_ID, (payload, context) -> context.server().execute(() -> PartyAddonServerPacket.handleChangeStar(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(InvitePlayerPacket.PACKET_ID, (payload, context) -> context.server().execute(() -> PartyAddonServerPacket.handleInvitePlayer(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(DeclineInvitePacket.PACKET_ID, (payload, context) -> context.server().execute(() -> PartyAddonServerPacket.handleDeclineInvite(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(AcceptInvitePacket.PACKET_ID, (payload, context) -> context.server().execute(() -> PartyAddonServerPacket.handleAcceptInvite(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(LeaveGroupPacket.PACKET_ID, (payload, context) -> context.server().execute(() -> PartyAddonServerPacket.handleLeaveGroup(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(KickPlayerPacket.PACKET_ID, (payload, context) -> context.server().execute(() -> PartyAddonServerPacket.handleKickPlayer(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(PromotePlayerPacket.PACKET_ID, (payload, context) -> context.server().execute(() -> PartyAddonServerPacket.handlePromotePlayer(context.player(), payload)));
    }

}
