package net.partyaddon;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.partyaddon.group.ClientParty;
import net.partyaddon.init.KeyInit;
import net.partyaddon.init.RenderInit;
import net.partyaddon.network.PartyAddonClientPacket;
import net.partyaddon.network.packet.DeclineInvitePacket;
import net.partyaddon.network.packet.InvitePlayerPacket;
import net.partyaddon.network.packet.MemberHealthPacket;
import net.partyaddon.network.packet.PartyScreenPacket;
import net.partyaddon.network.packet.SyncGroupPacket;
import net.partyaddon.network.packet.SyncPendingInvitationsPacket;
import net.partyaddon.network.packet.SyncSettingsPacket;
import net.partyaddon.network.packet.SyncStarsPacket;

@Environment(EnvType.CLIENT)
public class PartyAddonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        RenderInit.registerTabs();
        HudElementRegistry.addLast(RenderInit.PARTY_HUD, (context, deltaTracker) -> RenderInit.renderPartyHud(context));

        ClientPlayConnectionEvents.INIT.register((handler, client) -> client.execute(ClientParty::reset));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ClientParty::reset));

        ClientPlayNetworking.registerGlobalReceiver(PartyScreenPacket.PACKET_ID, (payload, context) -> context.client().execute(PartyAddonClientPacket::handleOpenPartyScreen));
        ClientPlayNetworking.registerGlobalReceiver(SyncStarsPacket.PACKET_ID, (payload, context) -> context.client().execute(() -> PartyAddonClientPacket.handleSyncStars(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SyncGroupPacket.PACKET_ID, (payload, context) -> context.client().execute(() -> PartyAddonClientPacket.handleSyncGroup(payload)));
        ClientPlayNetworking.registerGlobalReceiver(InvitePlayerPacket.PACKET_ID, (payload, context) -> context.client().execute(() -> PartyAddonClientPacket.handleInvitation(payload)));
        ClientPlayNetworking.registerGlobalReceiver(DeclineInvitePacket.PACKET_ID, (payload, context) -> context.client().execute(PartyAddonClientPacket::handleDeclineInvitation));
        ClientPlayNetworking.registerGlobalReceiver(SyncSettingsPacket.PACKET_ID, (payload, context) -> context.client().execute(() -> PartyAddonClientPacket.handleSyncSettings(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SyncPendingInvitationsPacket.PACKET_ID, (payload, context) -> context.client().execute(() -> PartyAddonClientPacket.handleSyncPendingInvitations(payload)));
        ClientPlayNetworking.registerGlobalReceiver(MemberHealthPacket.PACKET_ID, (payload, context) -> context.client().execute(() -> PartyAddonClientPacket.handleMemberHealth(payload)));

        KeyMapping.Category.register(KeyInit.CATEGORY.id());
        KeyMappingHelper.registerKeyMapping(KeyInit.screenKey);
        ClientTickEvents.END_CLIENT_TICK.register(KeyInit::onClientTick);
    }

}
