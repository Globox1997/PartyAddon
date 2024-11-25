package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

import java.util.UUID;

public record KickPlayerPacket(UUID groupLeaderUuid, UUID kickPlayerUuid) implements CustomPayload {

    public static final CustomPayload.Id<KickPlayerPacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("kick_player_packet"));

    public static final PacketCodec<RegistryByteBuf, KickPlayerPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.groupLeaderUuid);
        buf.writeUuid(value.kickPlayerUuid);
    }, buf -> new KickPlayerPacket(buf.readUuid(), buf.readUuid()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

