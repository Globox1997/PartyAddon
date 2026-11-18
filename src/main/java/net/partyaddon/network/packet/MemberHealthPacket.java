package net.partyaddon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

import java.util.List;
import java.util.UUID;

public record MemberHealthPacket(List<MemberHealth> members) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MemberHealthPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("member_health_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MemberHealthPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeCollection(value.members, (bufx, member) -> {
            bufx.writeUUID(member.uuid());
            bufx.writeFloat(member.health());
            bufx.writeFloat(member.maxHealth());
        });
    }, buf -> new MemberHealthPacket(buf.readList((bufx) -> new MemberHealth(bufx.readUUID(), bufx.readFloat(), bufx.readFloat()))));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

    public record MemberHealth(UUID uuid, float health, float maxHealth) {
    }

}
