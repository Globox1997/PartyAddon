package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

import java.util.UUID;

public record PromotePlayerPacket(UUID uuid) implements CustomPayload {

    public static final CustomPayload.Id<PromotePlayerPacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("promote_player_packet"));

    public static final PacketCodec<RegistryByteBuf, PromotePlayerPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.uuid);
    }, buf -> new PromotePlayerPacket(buf.readUuid()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
