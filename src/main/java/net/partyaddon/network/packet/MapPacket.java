package net.partyaddon.network.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.partyaddon.PartyAddonMain;

import java.util.List;
import java.util.UUID;

public record MapPacket(List<UUID> groupPlayerUUIDs, List<BlockPos> groupPlayerBlockPoses, List<Float> groupPlayerYaws) implements CustomPayload {

    public static final CustomPayload.Id<MapPacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("map_packet"));

    public static final PacketCodec<RegistryByteBuf, MapPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeCollection(value.groupPlayerUUIDs, (bufx, uuid) -> bufx.writeUuid(uuid));
        buf.writeCollection(value.groupPlayerBlockPoses, (bufx, pos) -> bufx.writeBlockPos(pos));
        buf.writeCollection(value.groupPlayerYaws, PacketByteBuf::writeFloat);
    }, buf -> new MapPacket(buf.readList((bufx) -> bufx.readUuid()), buf.readList((bufx) -> bufx.readBlockPos()), buf.readList(PacketByteBuf::readFloat)));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

