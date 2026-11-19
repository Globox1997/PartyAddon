package net.partyaddon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

import java.util.UUID;

public record InvitePlayerPacket(UUID uuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InvitePlayerPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("invite_player_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InvitePlayerPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeUUID(value.uuid);
    }, buf -> new InvitePlayerPacket(buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

