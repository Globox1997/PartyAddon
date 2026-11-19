package net.partyaddon.network.packet;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record SyncGroupPacket(List<UUID> availablePlayerIdList, List<UUID> starPlayerIdList, List<UUID> groupPlayerIdList, Optional<UUID> groupLeaderId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncGroupPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("sync_group_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncGroupPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, value.availablePlayerIdList);
        UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, value.starPlayerIdList);
        UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, value.groupPlayerIdList);
        buf.writeOptional(value.groupLeaderId, (bufx, uuid) -> bufx.writeUUID(uuid));
    }, buf -> new SyncGroupPacket(UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf), UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf), buf.readOptional(bufx -> bufx.readUUID())));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

