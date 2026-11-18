package net.partyaddon.network.packet;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

import java.util.List;
import java.util.UUID;

public record MemberHealthPacket(List<MemberHealth> members) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MemberHealthPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("member_health_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MemberHealthPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        MemberHealth.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, value.members);
    }, buf -> new MemberHealthPacket(MemberHealth.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

    public record MemberHealth(UUID uuid, float health, float maxHealth) {
        public static final StreamCodec<ByteBuf, MemberHealth> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, MemberHealth::uuid, ByteBufCodecs.FLOAT, MemberHealth::health,
                ByteBufCodecs.FLOAT, MemberHealth::maxHealth, MemberHealth::new);
    }

}
