package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

import java.util.List;
import java.util.UUID;

public record SyncPendingInvitationsPacket(List<UUID> uuids) implements CustomPayload {

    public static final CustomPayload.Id<SyncPendingInvitationsPacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("sync_pending_invitations_packet"));

    public static final PacketCodec<RegistryByteBuf, SyncPendingInvitationsPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeCollection(value.uuids, (bufx, uuid) -> bufx.writeUuid(uuid));
    }, buf -> new SyncPendingInvitationsPacket(buf.readList((bufx) -> bufx.readUuid())));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
