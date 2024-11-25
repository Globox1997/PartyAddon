package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

import java.util.UUID;

public record AcceptInvitePacket(UUID uuid) implements CustomPayload {

    public static final CustomPayload.Id<AcceptInvitePacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("accept_invite_packet"));

    public static final PacketCodec<RegistryByteBuf, AcceptInvitePacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.uuid);
    }, buf -> new AcceptInvitePacket(buf.readUuid()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

