package net.partyaddon.neoforge.client;

import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.partyaddon.PartyAddonMain;
import net.partyaddon.config.PartyAddonConfig;
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

@Mod(value = PartyAddonMain.MOD_ID, dist = Dist.CLIENT)
public class PartyAddonNeoForgeClient {

    public PartyAddonNeoForgeClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(RenderInit::registerTabs));
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAboveAll(RenderInit.PARTY_HUD, (context, deltaTracker) -> RenderInit.renderPartyHud(context)));
        modBus.addListener(RegisterClientPayloadHandlersEvent.class, PartyAddonNeoForgeClient::registerPayloadHandlers);
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            event.registerCategory(KeyInit.CATEGORY);
            event.register(KeyInit.screenKey);
        });

        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> KeyInit.onClientTick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> ClientParty.reset());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> ClientParty.reset());

        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> AutoConfigClient.getConfigScreen(PartyAddonConfig.class, parent).get());
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(PartyScreenPacket.PACKET_ID, (payload, context) -> PartyAddonClientPacket.handleOpenPartyScreen());
        event.register(SyncStarsPacket.PACKET_ID, (payload, context) -> PartyAddonClientPacket.handleSyncStars(payload));
        event.register(SyncGroupPacket.PACKET_ID, (payload, context) -> PartyAddonClientPacket.handleSyncGroup(payload));
        event.register(InvitePlayerPacket.PACKET_ID, (payload, context) -> PartyAddonClientPacket.handleInvitation(payload));
        event.register(DeclineInvitePacket.PACKET_ID, (payload, context) -> PartyAddonClientPacket.handleDeclineInvitation());
        event.register(SyncSettingsPacket.PACKET_ID, (payload, context) -> PartyAddonClientPacket.handleSyncSettings(payload));
        event.register(SyncPendingInvitationsPacket.PACKET_ID, (payload, context) -> PartyAddonClientPacket.handleSyncPendingInvitations(payload));
        event.register(MemberHealthPacket.PACKET_ID, (payload, context) -> PartyAddonClientPacket.handleMemberHealth(payload));
    }

}
