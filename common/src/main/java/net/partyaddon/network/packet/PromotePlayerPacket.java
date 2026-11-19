package net.partyaddon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

import java.util.UUID;

public record PromotePlayerPacket(UUID uuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PromotePlayerPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("promote_player_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PromotePlayerPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeUUID(value.uuid);
    }, buf -> new PromotePlayerPacket(buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}
