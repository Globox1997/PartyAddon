package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

import java.util.List;
import java.util.UUID;

public record SyncStarsPacket(List<UUID> uuids) implements CustomPayload {

    public static final CustomPayload.Id<SyncStarsPacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("sync_stars_packet"));

    public static final PacketCodec<RegistryByteBuf, SyncStarsPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeCollection(value.uuids, (bufx, uuid) -> bufx.writeUuid(uuid));
    }, buf -> new SyncStarsPacket(buf.readList((bufx) -> bufx.readUuid())));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

