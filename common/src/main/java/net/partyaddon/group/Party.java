package net.partyaddon.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.partyaddon.network.packet.MemberHealthPacket.MemberHealth;

public class Party {

    private final List<UUID> memberIds = new ArrayList<UUID>();
    private UUID leaderId;

    private double vanillaExperiencePool = 0.0D;
    private double levelZExperiencePool = 0.0D;

    private List<MemberHealth> lastSentMemberHealths = List.of();

    public Party(UUID leaderId) {
        this.leaderId = leaderId;
        this.memberIds.add(leaderId);
    }

    public UUID getLeaderId() {
        return this.leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        if (this.memberIds.contains(leaderId)) {
            this.leaderId = leaderId;
        }
    }

    public boolean isLeader(UUID playerId) {
        return this.leaderId.equals(playerId);
    }

    public List<UUID> getMemberIds() {
        return Collections.unmodifiableList(this.memberIds);
    }

    public List<UUID> getMemberIdsFor(UUID viewerId) {
        List<UUID> memberIds = new ArrayList<UUID>(this.memberIds.size());
        memberIds.add(viewerId);
        for (UUID memberId : this.memberIds) {
            if (!memberId.equals(viewerId)) {
                memberIds.add(memberId);
            }
        }
        return memberIds;
    }

    public List<ServerPlayer> getOnlineMembers(MinecraftServer server) {
        List<ServerPlayer> members = new ArrayList<ServerPlayer>(this.memberIds.size());
        for (UUID memberId : this.memberIds) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    public boolean contains(UUID playerId) {
        return this.memberIds.contains(playerId);
    }

    public int size() {
        return this.memberIds.size();
    }

    public void addMember(UUID playerId) {
        if (!this.memberIds.contains(playerId)) {
            this.memberIds.add(playerId);
        }
    }

    public void removeMember(UUID playerId) {
        this.memberIds.remove(playerId);
    }

    public int takeVanillaExperienceShare(double experience, int recipients) {
        this.vanillaExperiencePool += experience;
        int share = (int) (this.vanillaExperiencePool / recipients);
        this.vanillaExperiencePool -= (double) share * recipients;
        return share;
    }

    public int takeLevelZExperienceShare(double experience, int recipients) {
        this.levelZExperiencePool += experience;
        int share = (int) (this.levelZExperiencePool / recipients);
        this.levelZExperiencePool -= (double) share * recipients;
        return share;
    }

    public boolean updateSentMemberHealths(List<MemberHealth> memberHealths) {
        if (this.lastSentMemberHealths.equals(memberHealths)) {
            return false;
        }
        this.lastSentMemberHealths = List.copyOf(memberHealths);
        return true;
    }

    public void resetSentMemberHealths() {
        this.lastSentMemberHealths = List.of();
    }

    public static double getExperienceMultiplier(int partySize, int groupSize, float groupXpBonus, float fullGroupBonus) {
        double multiplier = 1.0D + Math.max(0.0f, groupXpBonus) * partySize;
        if (partySize >= groupSize) {
            multiplier += Math.max(0.0f, fullGroupBonus);
        }
        return multiplier;
    }
}
