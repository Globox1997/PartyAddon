package net.partyaddon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

import java.util.List;
import java.util.UUID;

public record SyncStarsPacket(List<UUID> uuids) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncStarsPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("sync_stars_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncStarsPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeCollection(value.uuids, (bufx, uuid) -> bufx.writeUUID(uuid));
    }, buf -> new SyncStarsPacket(buf.readList((bufx) -> bufx.readUUID())));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

