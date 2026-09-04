package net.seraphina.ninecun_lingtai.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaCandidateSet;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaCandidateSnapshot;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberKind;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberPageSnapshot;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberPacketLimits;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 掌中佛国成员摘要的有界网络编解码。 */
public final class PalmBuddhaMemberPacketUtility {
    private static final int MAXIMUM_PAGE_INDEX = PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_COUNT - 1;
    private static final int MAXIMUM_DISPLAY_NAME_LENGTH = PalmBuddhaMemberSnapshot.MAXIMUM_DISPLAY_NAME_LENGTH;

    private PalmBuddhaMemberPacketUtility() {
    }

    public static void writePage(FriendlyByteBuf buffer, PalmBuddhaMemberPageSnapshot snapshot) {
        buffer.writeVarInt(snapshot.pageIndex());
        buffer.writeVarInt(snapshot.pageCount());
        buffer.writeVarInt(snapshot.totalMemberCount());
        List<PalmBuddhaMemberSnapshot> members = snapshot.members();
        if (members.size() > PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("Too many palm-buddha members in packet");
        }
        buffer.writeVarInt(members.size());
        for (PalmBuddhaMemberSnapshot member : members) {
            buffer.writeUUID(member.memberId());
            buffer.writeVarInt(member.kind().wireId());
            buffer.writeUtf(member.displayName(), MAXIMUM_DISPLAY_NAME_LENGTH);
            buffer.writeBoolean(member.online());
            buffer.writeBoolean(member.currentDimension().isPresent());
            member.currentDimension().ifPresent(buffer::writeResourceLocation);
            buffer.writeBoolean(member.inExpectedRealm());
            buffer.writeBoolean(member.currentPosition().isPresent());
            member.currentPosition().ifPresent(position -> {
                buffer.writeVarInt(position.getX());
                buffer.writeVarInt(position.getY());
                buffer.writeVarInt(position.getZ());
            });
            buffer.writeBoolean(member.cultivationStage().isPresent());
            member.cultivationStage().ifPresent(stage -> {
                buffer.writeVarInt(stage.realm().ordinal());
                buffer.writeVarInt(stage.layer());
            });
        }
    }

    public static void writeCandidates(FriendlyByteBuf buffer, PalmBuddhaCandidateSet candidateSet) {
        List<PalmBuddhaCandidateSnapshot> candidates = candidateSet.candidates();
        if (candidates.size() > PalmBuddhaMemberPacketLimits.MAXIMUM_CANDIDATE_COUNT) {
            throw new IllegalArgumentException("Too many palm-buddha candidates in packet");
        }
        buffer.writeVarInt(candidates.size());
        for (PalmBuddhaCandidateSnapshot candidate : candidates) {
            buffer.writeUUID(candidate.memberId());
            buffer.writeVarInt(candidate.kind().wireId());
            buffer.writeUtf(candidate.displayName(), MAXIMUM_DISPLAY_NAME_LENGTH);
            buffer.writeBoolean(candidate.cultivationStage().isPresent());
            candidate.cultivationStage().ifPresent(stage -> {
                buffer.writeVarInt(stage.realm().ordinal());
                buffer.writeVarInt(stage.layer());
            });
            buffer.writeFloat(candidate.distance());
        }
    }

    public static PalmBuddhaCandidateSet readCandidates(FriendlyByteBuf buffer) {
        int candidateCount = readBoundedVarInt(
                buffer,
                "candidate count",
                0,
                PalmBuddhaMemberPacketLimits.MAXIMUM_CANDIDATE_COUNT
        );
        List<PalmBuddhaCandidateSnapshot> candidates = new ArrayList<>(candidateCount);
        for (int index = 0; index < candidateCount; index++) {
            java.util.UUID candidateId = buffer.readUUID();
            PalmBuddhaMemberKind kind = PalmBuddhaMemberKind.fromWireId(buffer.readVarInt());
            String displayName = buffer.readUtf(MAXIMUM_DISPLAY_NAME_LENGTH);
            Optional<CultivationStage> cultivationStage = Optional.empty();
            if (buffer.readBoolean()) {
                int realmOrdinal = buffer.readVarInt();
                CultivationRealm[] realms = CultivationRealm.values();
                if (realmOrdinal < 0 || realmOrdinal >= realms.length) {
                    throw new IllegalArgumentException("Invalid palm-buddha candidate cultivation realm");
                }
                cultivationStage = Optional.of(new CultivationStage(realms[realmOrdinal], buffer.readVarInt()));
            }
            candidates.add(new PalmBuddhaCandidateSnapshot(
                    candidateId,
                    displayName,
                    kind,
                    cultivationStage,
                    buffer.readFloat()
            ));
        }
        return new PalmBuddhaCandidateSet(candidates);
    }
    public static PalmBuddhaMemberPageSnapshot readPage(FriendlyByteBuf buffer) {
        int pageIndex = readBoundedVarInt(buffer, "page index", 0, MAXIMUM_PAGE_INDEX);
        int pageCount = readBoundedVarInt(
                buffer,
                "page count",
                0,
                PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_COUNT
        );
        int totalMemberCount = readBoundedVarInt(
                buffer,
                "total member count",
                0,
                PalmBuddhaMemberPacketLimits.MAXIMUM_TOTAL_MEMBER_COUNT
        );
        int memberCount = readBoundedVarInt(
                buffer,
                "page member count",
                0,
                PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_SIZE
        );
        if (pageCount == 0 && (pageIndex != 0 || totalMemberCount != 0 || memberCount != 0)) {
            throw new IllegalArgumentException("Empty palm-buddha page has inconsistent metadata");
        }
        if (pageCount > 0 && pageIndex >= pageCount) {
            throw new IllegalArgumentException("Palm-buddha page index exceeds page count");
        }

        List<PalmBuddhaMemberSnapshot> members = new ArrayList<>(memberCount);
        for (int index = 0; index < memberCount; index++) {
            members.add(readMember(buffer));
        }
        return new PalmBuddhaMemberPageSnapshot(pageIndex, pageCount, totalMemberCount, members);
    }

    private static PalmBuddhaMemberSnapshot readMember(FriendlyByteBuf buffer) {
        java.util.UUID memberId = buffer.readUUID();
        PalmBuddhaMemberKind kind = PalmBuddhaMemberKind.fromWireId(buffer.readVarInt());
        String displayName = buffer.readUtf(MAXIMUM_DISPLAY_NAME_LENGTH);
        boolean online = buffer.readBoolean();
        Optional<ResourceLocation> currentDimension = buffer.readBoolean()
                ? Optional.of(buffer.readResourceLocation())
                : Optional.empty();
        boolean inExpectedRealm = buffer.readBoolean();
        Optional<BlockPos> currentPosition = buffer.readBoolean()
                ? Optional.of(new BlockPos(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()))
                : Optional.empty();
        Optional<CultivationStage> cultivationStage = Optional.empty();
        if (buffer.readBoolean()) {
            int realmOrdinal = buffer.readVarInt();
            CultivationRealm[] realms = CultivationRealm.values();
            if (realmOrdinal < 0 || realmOrdinal >= realms.length) {
                throw new IllegalArgumentException("Invalid palm-buddha cultivation realm");
            }
            int layer = buffer.readVarInt();
            cultivationStage = Optional.of(new CultivationStage(realms[realmOrdinal], layer));
        }
        return new PalmBuddhaMemberSnapshot(
                memberId,
                displayName,
                online,
                currentDimension,
                inExpectedRealm,
                currentPosition,
                cultivationStage,
                kind
        );
    }

    private static int readBoundedVarInt(FriendlyByteBuf buffer, String name, int minimum, int maximum) {
        int value = buffer.readVarInt();
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Invalid palm-buddha " + name + ": " + value);
        }
        return value;
    }
}

