package net.partyaddon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record SyncGroupPacket(List<UUID> availablePlayerIdList, List<UUID> starPlayerIdList, List<UUID> groupPlayerIdList, Optional<UUID> groupLeaderId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncGroupPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("sync_group_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncGroupPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeCollection(value.availablePlayerIdList, (bufx, uuid) -> bufx.writeUUID(uuid));
        buf.writeCollection(value.starPlayerIdList, (bufx, uuid) -> bufx.writeUUID(uuid));
        buf.writeCollection(value.groupPlayerIdList, (bufx, uuid) -> bufx.writeUUID(uuid));
        buf.writeOptional(value.groupLeaderId, (bufx, uuid) -> bufx.writeUUID(uuid));
    }, buf -> new SyncGroupPacket(buf.readList((bufx) -> bufx.readUUID()), buf.readList((bufx) -> bufx.readUUID()), buf.readList((bufx) -> bufx.readUUID()), buf.readOptional(bufx -> bufx.readUUID())));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

