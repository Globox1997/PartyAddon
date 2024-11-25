package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record SyncGroupPacket(List<UUID> availablePlayerIdList, List<UUID> starPlayerIdList, List<UUID> groupPlayerIdList, Optional<UUID> groupLeaderId) implements CustomPayload {

    public static final CustomPayload.Id<SyncGroupPacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("sync_group_packet"));

    public static final PacketCodec<RegistryByteBuf, SyncGroupPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeCollection(value.availablePlayerIdList, (bufx, uuid) -> bufx.writeUuid(uuid));
        buf.writeCollection(value.starPlayerIdList, (bufx, uuid) -> bufx.writeUuid(uuid));
        buf.writeCollection(value.groupPlayerIdList, (bufx, uuid) -> bufx.writeUuid(uuid));
        buf.writeOptional(value.groupLeaderId, (bufx, uuid) -> bufx.writeUuid(uuid));
    }, buf -> new SyncGroupPacket(buf.readList((bufx) -> bufx.readUuid()), buf.readList((bufx) -> bufx.readUuid()), buf.readList((bufx) -> bufx.readUuid()), buf.readOptional(bufx -> bufx.readUuid())));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

