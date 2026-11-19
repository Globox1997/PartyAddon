package net.partyaddon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

import java.util.Optional;
import java.util.UUID;

public record DeclineInvitePacket(Optional<UUID> uuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DeclineInvitePacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("decline_invite_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeclineInvitePacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeOptional(value.uuid, (bufx, uuid) -> bufx.writeUUID(uuid));
    }, buf -> new DeclineInvitePacket(buf.readOptional(bufx -> bufx.readUUID())));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

