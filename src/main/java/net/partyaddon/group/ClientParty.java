package net.partyaddon.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.network.packet.MemberHealthPacket.MemberHealth;

@Environment(EnvType.CLIENT)
public final class ClientParty {

    private static List<UUID> availablePlayerIds = List.of();
    private static List<UUID> starPlayerIds = List.of();
    private static List<UUID> groupPlayerIds = List.of();
    private static List<UUID> pendingInvitationIds = List.of();
    @Nullable
    private static UUID groupLeaderId = null;
    @Nullable
    private static UUID invitationPlayerId = null;
    private static final Map<UUID, MemberHealth> MEMBER_HEALTH = new HashMap<UUID, MemberHealth>();

    private static int groupSize = ConfigInit.CONFIG.groupSize;
    private static float groupXpBonus = ConfigInit.CONFIG.groupXpBonus;
    private static float fullGroupBonus = ConfigInit.CONFIG.fullGroupBonus;

    private static int version = 0;
    private static int pendingInvitationsVersion = 0;

    private ClientParty() {
    }

    public static void reset() {
        availablePlayerIds = List.of();
        starPlayerIds = List.of();
        groupPlayerIds = List.of();
        pendingInvitationIds = List.of();
        groupLeaderId = null;
        invitationPlayerId = null;
        MEMBER_HEALTH.clear();
        groupSize = ConfigInit.CONFIG.groupSize;
        groupXpBonus = ConfigInit.CONFIG.groupXpBonus;
        fullGroupBonus = ConfigInit.CONFIG.fullGroupBonus;
        version++;
    }

    public static void updateGroup(List<UUID> availablePlayerIds, List<UUID> starPlayerIds, List<UUID> groupPlayerIds, @Nullable UUID groupLeaderId) {
        ClientParty.availablePlayerIds = List.copyOf(availablePlayerIds);
        ClientParty.starPlayerIds = List.copyOf(starPlayerIds);
        ClientParty.groupPlayerIds = List.copyOf(groupPlayerIds);
        ClientParty.groupLeaderId = groupLeaderId;
        MEMBER_HEALTH.keySet().retainAll(ClientParty.groupPlayerIds);
        version++;
    }

    public static void updateStars(List<UUID> starPlayerIds) {
        ClientParty.starPlayerIds = List.copyOf(starPlayerIds);
        version++;
    }

    public static void updatePendingInvitations(List<UUID> pendingInvitationIds) {
        ClientParty.pendingInvitationIds = List.copyOf(pendingInvitationIds);
        pendingInvitationsVersion++;
        version++;
    }

    public static int getPendingInvitationsVersion() {
        return pendingInvitationsVersion;
    }

    public static void updateMemberHealth(List<MemberHealth> memberHealths) {
        MEMBER_HEALTH.clear();
        for (MemberHealth memberHealth : memberHealths) {
            MEMBER_HEALTH.put(memberHealth.uuid(), memberHealth);
        }
    }

    public static void updateSettings(int groupSize, float groupXpBonus, float fullGroupBonus) {
        ClientParty.groupSize = groupSize;
        ClientParty.groupXpBonus = groupXpBonus;
        ClientParty.fullGroupBonus = fullGroupBonus;
        version++;
    }

    public static void setInvitationPlayerId(@Nullable UUID invitationPlayerId) {
        ClientParty.invitationPlayerId = invitationPlayerId;
        version++;
    }

    public static int getVersion() {
        return version;
    }

    public static List<UUID> getPendingInvitationIds() {
        return pendingInvitationIds;
    }

    public static List<UUID> getAvailablePlayerIds() {
        return availablePlayerIds;
    }

    public static List<UUID> getStarPlayerIds() {
        return starPlayerIds;
    }

    public static List<UUID> getGroupPlayerIds() {
        return groupPlayerIds;
    }

    @Nullable
    public static UUID getGroupLeaderId() {
        return groupLeaderId;
    }

    @Nullable
    public static UUID getInvitationPlayerId() {
        return invitationPlayerId;
    }

    @Nullable
    public static MemberHealth getMemberHealth(UUID playerId) {
        return MEMBER_HEALTH.get(playerId);
    }

    public static boolean isGroupLeader(MinecraftClient client) {
        return groupLeaderId != null && client.player != null && groupLeaderId.equals(client.player.getUuid());
    }

    public static boolean isGroupLeader(UUID playerId) {
        return groupLeaderId != null && groupLeaderId.equals(playerId);
    }

    public static int getGroupSize() {
        return groupSize;
    }

    public static float getGroupXpBonus() {
        return groupXpBonus;
    }

    public static float getFullGroupBonus() {
        return fullGroupBonus;
    }
}
