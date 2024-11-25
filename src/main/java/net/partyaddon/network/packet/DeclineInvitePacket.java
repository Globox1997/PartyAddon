package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

import java.util.UUID;

public record DeclineInvitePacket(UUID uuid) implements CustomPayload {

    public static final CustomPayload.Id<DeclineInvitePacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("decline_invite_packet"));

    public static final PacketCodec<RegistryByteBuf, DeclineInvitePacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.uuid);
    }, buf -> new DeclineInvitePacket(buf.readUuid()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

