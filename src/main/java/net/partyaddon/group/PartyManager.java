package net.partyaddon.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.levelz.access.ServerPlayerSyncAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.network.PartyAddonServerPacket;
import net.partyaddon.network.packet.MemberHealthPacket.MemberHealth;

public final class PartyManager {

    private static final int HEALTH_SYNC_INTERVAL = 10;

    private static final Map<UUID, Party> PARTIES = new HashMap<UUID, Party>();
    private static final Map<UUID, Invitation> INVITATIONS = new HashMap<UUID, Invitation>();
    private static final Map<UUID, Set<UUID>> STARS = new HashMap<UUID, Set<UUID>>();

    private record Invitation(UUID inviterId, int expirationTick) {
    }

    private enum LeaveReason {
        LEFT, KICKED, DISCONNECTED
    }

    private PartyManager() {
    }

    @Nullable
    public static Party getParty(UUID playerId) {
        return PARTIES.get(playerId);
    }

    public static void clear() {
        PARTIES.clear();
        INVITATIONS.clear();
        STARS.clear();
    }

    // Invitation stuff
    public static void invite(ServerPlayerEntity inviter, ServerPlayerEntity invited) {
        sendInvitation(inviter, invited);
        syncOutgoingInvitations(inviter);
    }

    private static void sendInvitation(ServerPlayerEntity inviter, ServerPlayerEntity invited) {
        if (inviter.getUuid().equals(invited.getUuid())) {
            inviter.sendMessage(Text.translatable("text.partyaddon.cannot_invite_self"));
            return;
        }
        Party party = getParty(inviter.getUuid());
        if (party != null) {
            if (!party.isLeader(inviter.getUuid())) {
                inviter.sendMessage(Text.translatable("text.partyaddon.not_leader"));
                return;
            }
            if (party.contains(invited.getUuid())) {
                inviter.sendMessage(Text.translatable("text.partyaddon.already_in_your_group", invited.getName().getString()));
                return;
            }
            if (party.size() >= ConfigInit.CONFIG.groupSize) {
                inviter.sendMessage(Text.translatable("text.partyaddon.group_is_full", inviter.getName().getString()));
                return;
            }
        }
        Invitation pending = INVITATIONS.get(invited.getUuid());
        if (pending != null && pending.inviterId().equals(inviter.getUuid())) {
            inviter.sendMessage(Text.translatable("text.partyaddon.already_invited", invited.getName().getString()));
            return;
        }
        MinecraftServer server = inviter.getServer();
        INVITATIONS.put(invited.getUuid(), new Invitation(inviter.getUuid(), server.getTicks() + Math.max(1, ConfigInit.CONFIG.invitationTime)));

        if (pending != null) {
            ServerPlayerEntity previousInviter = server.getPlayerManager().getPlayer(pending.inviterId());
            if (previousInviter != null) {
                previousInviter.sendMessage(Text.translatable("text.partyaddon.invitation_replaced", invited.getName().getString()));
                syncOutgoingInvitations(previousInviter);
            }
        }

        invited.sendMessage(Text.translatable("text.partyaddon.invitation", inviter.getName().getString()));
        inviter.sendMessage(Text.translatable("text.partyaddon.invitation_sent", invited.getName().getString()));
        PartyAddonServerPacket.writeS2CSyncInvitationPacket(invited, inviter.getUuid());
    }

    public static void acceptInvitation(ServerPlayerEntity player, @Nullable UUID expectedInviterId) {
        Invitation invitation = INVITATIONS.get(player.getUuid());
        if (invitation == null || (expectedInviterId != null && !expectedInviterId.equals(invitation.inviterId()))) {
            player.sendMessage(Text.translatable("text.partyaddon.no_invitation"));
            return;
        }
        INVITATIONS.remove(player.getUuid());
        PartyAddonServerPacket.writeS2CSyncDeclinePacket(player, null);

        MinecraftServer server = player.getServer();
        ServerPlayerEntity inviter = server.getPlayerManager().getPlayer(invitation.inviterId());
        if (inviter == null) {
            player.sendMessage(Text.translatable("text.partyaddon.no_invitation"));
            return;
        }
        syncOutgoingInvitations(inviter);
        Party party = getParty(inviter.getUuid());
        if (party != null) {
            if (!party.isLeader(inviter.getUuid())) {
                player.sendMessage(Text.translatable("text.partyaddon.invitation_invalid"));
                return;
            }
            if (party.contains(player.getUuid())) {
                player.sendMessage(Text.translatable("text.partyaddon.already_member"));
                return;
            }
            if (party.size() >= ConfigInit.CONFIG.groupSize) {
                player.sendMessage(Text.translatable("text.partyaddon.group_is_full", getName(server, party.getLeaderId())));
                return;
            }
        }
        if (getParty(player.getUuid()) != null) {
            removeMember(player, LeaveReason.LEFT);
        }
        if (party == null) {
            party = new Party(inviter.getUuid());
            PARTIES.put(inviter.getUuid(), party);
        }
        for (ServerPlayerEntity member : party.getOnlineMembers(server)) {
            member.sendMessage(Text.translatable("text.partyaddon.accepted_invitation", player.getName().getString()));
        }
        party.addMember(player.getUuid());
        PARTIES.put(player.getUuid(), party);

        player.sendMessage(Text.translatable("text.partyaddon.joined_group", getName(server, party.getLeaderId())));
        syncParty(server, party);
    }

    public static void declineInvitation(ServerPlayerEntity player, @Nullable UUID expectedInviterId) {
        Invitation invitation = INVITATIONS.get(player.getUuid());
        PartyAddonServerPacket.writeS2CSyncDeclinePacket(player, null);
        if (invitation == null || (expectedInviterId != null && !expectedInviterId.equals(invitation.inviterId()))) {
            player.sendMessage(Text.translatable("text.partyaddon.no_invitation"));
            return;
        }
        INVITATIONS.remove(player.getUuid());

        MinecraftServer server = player.getServer();
        ServerPlayerEntity inviter = server.getPlayerManager().getPlayer(invitation.inviterId());
        if (inviter != null) {
            inviter.sendMessage(Text.translatable("text.partyaddon.declined_invitation", player.getName().getString()));
            syncOutgoingInvitations(inviter);
        }
        player.sendMessage(Text.translatable("text.partyaddon.invitation_declined", getName(server, invitation.inviterId())));
    }

    // Group stuff
    public static void leave(ServerPlayerEntity player) {
        if (getParty(player.getUuid()) == null) {
            player.sendMessage(Text.translatable("text.partyaddon.no_group"));
            return;
        }
        removeMember(player, LeaveReason.LEFT);
    }

    public static void kick(ServerPlayerEntity leader, UUID targetId) {
        Party party = getLeaderParty(leader);
        if (party == null) {
            return;
        }
        if (leader.getUuid().equals(targetId)) {
            leader.sendMessage(Text.translatable("text.partyaddon.cannot_kick_self"));
            return;
        }
        ServerPlayerEntity target = leader.getServer().getPlayerManager().getPlayer(targetId);
        if (target == null || !party.contains(targetId)) {
            leader.sendMessage(Text.translatable("text.partyaddon.not_in_group"));
            return;
        }
        removeMember(target, LeaveReason.KICKED);
    }

    public static void promote(ServerPlayerEntity leader, UUID targetId) {
        Party party = getLeaderParty(leader);
        if (party == null) {
            return;
        }
        if (leader.getUuid().equals(targetId)) {
            leader.sendMessage(Text.translatable("text.partyaddon.already_leader"));
            return;
        }
        if (!party.contains(targetId)) {
            leader.sendMessage(Text.translatable("text.partyaddon.not_in_group"));
            return;
        }
        MinecraftServer server = leader.getServer();
        party.setLeaderId(targetId);
        for (ServerPlayerEntity member : party.getOnlineMembers(server)) {
            member.sendMessage(Text.translatable("text.partyaddon.new_leader", getName(server, targetId)));
        }
        syncParty(server, party);
    }

    public static void list(ServerPlayerEntity player) {
        Party party = getParty(player.getUuid());
        if (party == null) {
            player.sendMessage(Text.translatable("text.partyaddon.no_group"));
            return;
        }
        MinecraftServer server = player.getServer();
        MutableText members = Text.empty();
        for (int i = 0; i < party.size(); i++) {
            UUID memberId = party.getMemberIds().get(i);
            if (i > 0) {
                members.append(", ");
            }
            if (party.isLeader(memberId)) {
                members.append(Text.translatable("text.partyaddon.leader_suffix", getName(server, memberId)));
            } else {
                members.append(getName(server, memberId));
            }
        }
        player.sendMessage(Text.translatable("text.partyaddon.group_list", party.size(), ConfigInit.CONFIG.groupSize, members));
    }

    // Star stuff
    public static void toggleStar(ServerPlayerEntity player, UUID targetId) {
        Set<UUID> stars = STARS.computeIfAbsent(player.getUuid(), id -> new LinkedHashSet<UUID>());
        if (!stars.remove(targetId) && !targetId.equals(player.getUuid()) && player.getServer().getPlayerManager().getPlayer(targetId) != null) {
            stars.add(targetId);
        }
        PartyAddonServerPacket.writeS2CSyncStarPlayerListPacket(player, getStarIds(player.getUuid()));
    }

    public static List<UUID> getStarIds(UUID playerId) {
        Set<UUID> stars = STARS.get(playerId);
        return stars == null ? List.of() : List.copyOf(stars);
    }

    // Experience stuff
    public static boolean shareVanillaExperience(ServerPlayerEntity collector, int experience) {
        Party party = getParty(collector.getUuid());
        if (party == null || experience <= 0) {
            return false;
        }
        List<ServerPlayerEntity> members = party.getOnlineMembers(collector.getServer());
        if (members.isEmpty()) {
            return false;
        }
        int share = party.takeVanillaExperienceShare(experience * getExperienceMultiplier(party), members.size());
        if (share > 0) {
            for (ServerPlayerEntity member : members) {
                member.addExperience(share);
            }
        }
        return true;
    }

    public static boolean shareLevelZExperience(ServerPlayerEntity collector, int experience) {
        Party party = getParty(collector.getUuid());
        if (party == null || experience <= 0) {
            return false;
        }
        List<ServerPlayerEntity> members = party.getOnlineMembers(collector.getServer());
        if (members.isEmpty()) {
            return false;
        }
        int share = party.takeLevelZExperienceShare(experience * getExperienceMultiplier(party), members.size());
        if (share > 0) {
            for (ServerPlayerEntity member : members) {
                ((ServerPlayerSyncAccess) member).addLevelExperience(share);
            }
        }
        return true;
    }

    private static double getExperienceMultiplier(Party party) {
        return Party.getExperienceMultiplier(party.size(), ConfigInit.CONFIG.groupSize, ConfigInit.CONFIG.groupXpBonus, ConfigInit.CONFIG.fullGroupBonus);
    }

    // Events
    public static void onJoin(ServerPlayerEntity player) {
        PartyAddonServerPacket.writeS2CSyncSettingsPacket(player);
        syncPlayer(player);
    }

    public static void onDisconnect(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        Invitation ownInvitation = INVITATIONS.remove(player.getUuid());
        if (ownInvitation != null) {
            syncOutgoingInvitations(server, ownInvitation.inviterId());
        }

        Iterator<Map.Entry<UUID, Invitation>> iterator = INVITATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Invitation> entry = iterator.next();
            if (!entry.getValue().inviterId().equals(player.getUuid())) {
                continue;
            }
            iterator.remove();
            ServerPlayerEntity invited = server.getPlayerManager().getPlayer(entry.getKey());
            if (invited != null) {
                invited.sendMessage(Text.translatable("text.partyaddon.invitation_expired", player.getName().getString()));
                PartyAddonServerPacket.writeS2CSyncDeclinePacket(invited, null);
            }
        }
        removeMember(player, LeaveReason.DISCONNECTED);
    }

    public static void tick(MinecraftServer server) {
        if (!INVITATIONS.isEmpty()) {
            int ticks = server.getTicks();
            Set<UUID> invitersToSync = null;
            Iterator<Map.Entry<UUID, Invitation>> iterator = INVITATIONS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Invitation> entry = iterator.next();
                if (entry.getValue().expirationTick() > ticks) {
                    continue;
                }
                iterator.remove();
                ServerPlayerEntity invited = server.getPlayerManager().getPlayer(entry.getKey());
                ServerPlayerEntity inviter = server.getPlayerManager().getPlayer(entry.getValue().inviterId());
                if (invited != null) {
                    invited.sendMessage(Text.translatable("text.partyaddon.invitation_expired", getName(server, entry.getValue().inviterId())));
                    PartyAddonServerPacket.writeS2CSyncDeclinePacket(invited, null);
                    if (inviter != null) {
                        inviter.sendMessage(Text.translatable("text.partyaddon.invitation_not_answered", invited.getName().getString()));
                    }
                }
                if (inviter != null) {
                    if (invitersToSync == null) {
                        invitersToSync = new HashSet<UUID>();
                    }
                    invitersToSync.add(inviter.getUuid());
                }
            }
            if (invitersToSync != null) {
                for (UUID inviterId : invitersToSync) {
                    syncOutgoingInvitations(server, inviterId);
                }
            }
        }
        if (!PARTIES.isEmpty() && server.getTicks() % HEALTH_SYNC_INTERVAL == 0) {
            Set<Party> parties = Collections.newSetFromMap(new IdentityHashMap<Party, Boolean>());
            parties.addAll(PARTIES.values());
            for (Party party : parties) {
                List<ServerPlayerEntity> members = party.getOnlineMembers(server);
                List<MemberHealth> memberHealths = new ArrayList<MemberHealth>(members.size());
                for (ServerPlayerEntity member : members) {
                    memberHealths.add(new MemberHealth(member.getUuid(), member.getHealth(), member.getMaxHealth()));
                }
                if (!party.updateSentMemberHealths(memberHealths)) {
                    continue;
                }
                for (ServerPlayerEntity member : members) {
                    PartyAddonServerPacket.writeS2CMemberHealthPacket(member, memberHealths);
                }
            }
        }
    }

    // Sync stuff
    public static void syncPlayer(ServerPlayerEntity player) {
        Party party = getParty(player.getUuid());
        List<UUID> availablePlayerIds = new ArrayList<UUID>();
        for (ServerPlayerEntity serverPlayerEntity : player.getServer().getPlayerManager().getPlayerList()) {
            if (!serverPlayerEntity.getUuid().equals(player.getUuid())) {
                availablePlayerIds.add(serverPlayerEntity.getUuid());
            }
        }
        List<UUID> groupPlayerIds = party == null ? List.of() : party.getMemberIdsFor(player.getUuid());
        PartyAddonServerPacket.writeS2CSyncGroupPacket(player, availablePlayerIds, getStarIds(player.getUuid()), groupPlayerIds, party == null ? null : party.getLeaderId());
    }

    public static void syncOutgoingInvitations(ServerPlayerEntity inviter) {
        List<UUID> invitedIds = new ArrayList<UUID>();
        for (Map.Entry<UUID, Invitation> entry : INVITATIONS.entrySet()) {
            if (entry.getValue().inviterId().equals(inviter.getUuid())) {
                invitedIds.add(entry.getKey());
            }
        }
        PartyAddonServerPacket.writeS2CSyncPendingInvitationsPacket(inviter, invitedIds);
    }

    private static void syncOutgoingInvitations(MinecraftServer server, UUID inviterId) {
        ServerPlayerEntity inviter = server.getPlayerManager().getPlayer(inviterId);
        if (inviter != null) {
            syncOutgoingInvitations(inviter);
        }
    }

    private static void syncParty(MinecraftServer server, Party party) {
        party.resetSentMemberHealths();
        for (ServerPlayerEntity member : party.getOnlineMembers(server)) {
            syncPlayer(member);
        }
    }

    private static void removeMember(ServerPlayerEntity player, LeaveReason reason) {
        Party party = PARTIES.remove(player.getUuid());
        if (party == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        boolean wasLeader = party.isLeader(player.getUuid());
        party.removeMember(player.getUuid());

        if (reason != LeaveReason.DISCONNECTED) {
            player.sendMessage(Text.translatable(reason == LeaveReason.KICKED ? "text.partyaddon.kicked" : "text.partyaddon.leave_group"));
            syncPlayer(player);
        }
        Text memberMessage = Text.translatable(reason == LeaveReason.KICKED ? "text.partyaddon.kicked_member" : "text.partyaddon.left_group", player.getName().getString());

        if (party.size() <= 1) {
            for (UUID memberId : party.getMemberIds()) {
                PARTIES.remove(memberId);
                ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                if (member != null) {
                    member.sendMessage(memberMessage);
                    member.sendMessage(Text.translatable("text.partyaddon.group_disbanded"));
                    syncPlayer(member);
                }
            }
            return;
        }
        if (wasLeader) {
            party.setLeaderId(party.getMemberIds().get(0));
        }
        for (ServerPlayerEntity member : party.getOnlineMembers(server)) {
            member.sendMessage(memberMessage);
            if (wasLeader) {
                member.sendMessage(Text.translatable("text.partyaddon.new_leader", getName(server, party.getLeaderId())));
            }
        }
        syncParty(server, party);
    }

    @Nullable
    private static Party getLeaderParty(ServerPlayerEntity player) {
        Party party = getParty(player.getUuid());
        if (party == null) {
            player.sendMessage(Text.translatable("text.partyaddon.no_group"));
            return null;
        }
        if (!party.isLeader(player.getUuid())) {
            player.sendMessage(Text.translatable("text.partyaddon.not_leader"));
            return null;
        }
        return party;
    }

    private static String getName(MinecraftServer server, UUID playerId) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        return player != null ? player.getName().getString() : "?";
    }
}
