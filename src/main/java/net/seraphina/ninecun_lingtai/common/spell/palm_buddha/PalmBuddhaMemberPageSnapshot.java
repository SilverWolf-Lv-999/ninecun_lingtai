package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import java.util.List;
import java.util.Objects;

/** 一页佛国成员摘要，供后续水墨界面按页读取。 */
public record PalmBuddhaMemberPageSnapshot(
        int pageIndex,
        int pageCount,
        int totalMemberCount,
        List<PalmBuddhaMemberSnapshot> members
) {
    public PalmBuddhaMemberPageSnapshot {
        if (pageIndex < 0) {
            throw new IllegalArgumentException("Palm-buddha page index cannot be negative");
        }
        if (pageCount < 0 || (pageCount == 0 && pageIndex != 0) || (pageCount > 0 && pageIndex >= pageCount)) {
            throw new IllegalArgumentException("Invalid palm-buddha page bounds");
        }
        if (totalMemberCount < 0) {
            throw new IllegalArgumentException("Palm-buddha member count cannot be negative");
        }
        members = List.copyOf(Objects.requireNonNull(members, "members"));
        if (members.size() > PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("Too many members in a palm-buddha page");
        }
        if (members.size() > totalMemberCount) {
            throw new IllegalArgumentException("A page cannot contain more members than the total count");
        }
    }

    public static PalmBuddhaMemberPageSnapshot empty() {
        return new PalmBuddhaMemberPageSnapshot(0, 0, 0, List.of());
    }
}
