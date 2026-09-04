package net.seraphina.ninecun_lingtai.client.player;

import net.minecraft.client.Minecraft;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestPalmBuddhaCandidatesPacket;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaCandidateSet;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaCandidateSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ClientPalmBuddhaCandidateManager {
    private static PalmBuddhaCandidateSet thisCandidateSet = PalmBuddhaCandidateSet.empty();
    private static boolean thisRequestPending;

    private ClientPalmBuddhaCandidateManager() {
    }

    public static void request() {
        if (Minecraft.getInstance().player == null || thisRequestPending) {
            return;
        }
        thisRequestPending = true;
        ModNetwork.sendToServer(RequestPalmBuddhaCandidatesPacket.INSTANCE);
    }

    public static void accept(PalmBuddhaCandidateSet candidateSet) {
        if (candidateSet == null) {
            return;
        }
        thisCandidateSet = candidateSet;
        thisRequestPending = false;
    }

    public static PalmBuddhaCandidateSet getCandidateSet() {
        return thisCandidateSet;
    }

    public static List<PalmBuddhaCandidateSnapshot> getCandidates() {
        return thisCandidateSet.candidates();
    }

    public static Optional<PalmBuddhaCandidateSnapshot> findCandidate(UUID candidateId) {
        if (candidateId == null) {
            return Optional.empty();
        }
        return thisCandidateSet.candidates().stream()
                .filter(candidate -> candidate.memberId().equals(candidateId))
                .findFirst();
    }

    public static boolean isRequestPending() {
        return thisRequestPending;
    }

    public static void reset() {
        thisCandidateSet = PalmBuddhaCandidateSet.empty();
        thisRequestPending = false;
    }
}
