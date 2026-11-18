package net.partyaddon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

import java.util.UUID;

public record KickPlayerPacket(UUID kickPlayerUuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KickPlayerPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("kick_player_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KickPlayerPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeUUID(value.kickPlayerUuid);
    }, buf -> new KickPlayerPacket(buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}
