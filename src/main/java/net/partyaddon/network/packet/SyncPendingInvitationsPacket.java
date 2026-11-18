package net.partyaddon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

import java.util.List;
import java.util.UUID;

public record SyncPendingInvitationsPacket(List<UUID> uuids) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncPendingInvitationsPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("sync_pending_invitations_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPendingInvitationsPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeCollection(value.uuids, (bufx, uuid) -> bufx.writeUUID(uuid));
    }, buf -> new SyncPendingInvitationsPacket(buf.readList((bufx) -> bufx.readUUID())));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}
