package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

import java.util.UUID;

public record ChangeStarPacket(UUID uuid) implements CustomPayload {

    public static final CustomPayload.Id<ChangeStarPacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("change_star_packet"));

    public static final PacketCodec<RegistryByteBuf, ChangeStarPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.uuid);
    }, buf -> new ChangeStarPacket(buf.readUuid()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

