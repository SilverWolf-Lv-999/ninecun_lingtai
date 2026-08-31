package net.seraphina.ninecun_lingtai.common.formation;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 一座已激活阵法的运行时权威记录（存 {@link FormationSavedData}，按维度一份）。
 * 存在即"运转中"；瘫痪态（生门被拔）保留记录但效果全停，到期重校验后续/散。
 * 阵图数据只存 {@link #formationId}，/reload 后按 id 重查（施工图验收 §11）。
 */
public final class FormationInstance {
    private static final String DISC_POS_TAG = "DiscPos";
    private static final String FORMATION_ID_TAG = "FormationId";
    private static final String OWNER_TAG = "Owner";
    private static final String OWNER_NAME_TAG = "OwnerName";
    private static final String EFFICIENCY_TAG = "Efficiency";
    private static final String FLAG_POSITIONS_TAG = "FlagPositions";
    private static final String FLAG_ELEMENTS_TAG = "FlagElements";
    private static final String LIFE_GATE_TAG = "LifeGateIndex";
    private static final String ENLIGHTENED_TAG = "Enlightened";
    private static final String WHITELIST_TAG = "Whitelist";
    private static final String PARALYZED_UNTIL_TAG = "ParalyzedUntil";
    private static final String DISTURB_COUNT_TAG = "DisturbCount";
    private static final String FLOW_HIDDEN_UNTIL_TAG = "FlowHiddenUntil";
    private static final String SURGE_COOLDOWN_TAG = "SurgeCooldownUntil";
    private static final String LINK_RULES_TAG = "LinkRules";

    private final BlockPos discPos;
    private final ResourceLocation formationId;
    private final UUID ownerUuid;
    private final String ownerName;
    private double efficiency;
    private final List<BlockPos> flagPositions = new ArrayList<>();
    private final List<ElementType> flagElements = new ArrayList<>();
    /** 布阵时随机指定的生门旗下标（阵主自己也不知道——不在任何提示里出现）。 */
    private int lifeGateIndex;
    private boolean enlightened;
    private final Set<UUID> whitelist = new HashSet<>();
    /** 生门被拔后的瘫痪到期（维度 gameTime）；0 = 未瘫痪。 */
    private long paralyzedUntilGameTime;
    private int disturbCount;
    /** 惊扰满 3 后灵力流隐藏到期（修真历刻）。 */
    private long flowHiddenUntilCalendarTick;
    private long surgeCooldownUntilGameTime;
    /** 阵联规则（M5 预留字段，本期只随盘存续不解析）。 */
    private final ListTag linkRules = new ListTag();

    public FormationInstance(
            BlockPos discPos,
            ResourceLocation formationId,
            UUID ownerUuid,
            String ownerName
    ) {
        this.discPos = discPos.immutable();
        this.formationId = formationId;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
    }

    public BlockPos discPos() {
        return this.discPos;
    }

    public ResourceLocation formationId() {
        return this.formationId;
    }

    public UUID ownerUuid() {
        return this.ownerUuid;
    }

    public String ownerName() {
        return this.ownerName;
    }

    public double efficiency() {
        return this.efficiency;
    }

    public void setEfficiency(double efficiency) {
        this.efficiency = efficiency;
    }

    public List<BlockPos> flagPositions() {
        return this.flagPositions;
    }

    public List<ElementType> flagElements() {
        return this.flagElements;
    }

    public int lifeGateIndex() {
        return this.lifeGateIndex;
    }

    public void setLifeGateIndex(int lifeGateIndex) {
        this.lifeGateIndex = lifeGateIndex;
    }

    public boolean isEnlightened() {
        return this.enlightened;
    }

    public void setEnlightened(boolean enlightened) {
        this.enlightened = enlightened;
    }

    public Set<UUID> whitelist() {
        return this.whitelist;
    }

    public boolean isParalyzed(long gameTime) {
        return this.paralyzedUntilGameTime > gameTime;
    }

    public long paralyzedUntilGameTime() {
        return this.paralyzedUntilGameTime;
    }

    public void setParalyzedUntilGameTime(long gameTime) {
        this.paralyzedUntilGameTime = gameTime;
    }

    public int disturbCount() {
        return this.disturbCount;
    }

    public void setDisturbCount(int disturbCount) {
        this.disturbCount = disturbCount;
    }

    public boolean isFlowHidden(long calendarTick) {
        return this.flowHiddenUntilCalendarTick > calendarTick;
    }

    public void setFlowHiddenUntilCalendarTick(long calendarTick) {
        this.flowHiddenUntilCalendarTick = calendarTick;
    }

    public boolean isSurgeOnCooldown(long gameTime) {
        return this.surgeCooldownUntilGameTime > gameTime;
    }

    public void setSurgeCooldownUntilGameTime(long gameTime) {
        this.surgeCooldownUntilGameTime = gameTime;
    }

    /** 点睛白名单判定：阵主恒通行。 */
    public boolean isWhitelisted(UUID playerUuid) {
        return this.ownerUuid.equals(playerUuid) || this.whitelist.contains(playerUuid);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put(DISC_POS_TAG, NbtUtils.writeBlockPos(this.discPos));
        tag.putString(FORMATION_ID_TAG, this.formationId.toString());
        tag.putUUID(OWNER_TAG, this.ownerUuid);
        tag.putString(OWNER_NAME_TAG, this.ownerName);
        tag.putDouble(EFFICIENCY_TAG, this.efficiency);
        ListTag flagTags = new ListTag();
        for (BlockPos flagPos : this.flagPositions) {
            flagTags.add(NbtUtils.writeBlockPos(flagPos));
        }
        tag.put(FLAG_POSITIONS_TAG, flagTags);
        byte[] elements = new byte[this.flagElements.size()];
        for (int index = 0; index < this.flagElements.size(); index++) {
            elements[index] = (byte) ElementType.FIVE_ELEMENTS.indexOf(this.flagElements.get(index));
        }
        tag.putByteArray(FLAG_ELEMENTS_TAG, elements);
        tag.putInt(LIFE_GATE_TAG, this.lifeGateIndex);
        tag.putBoolean(ENLIGHTENED_TAG, this.enlightened);
        ListTag whitelistTags = new ListTag();
        for (UUID entry : this.whitelist) {
            whitelistTags.add(NbtUtils.createUUID(entry));
        }
        tag.put(WHITELIST_TAG, whitelistTags);
        tag.putLong(PARALYZED_UNTIL_TAG, this.paralyzedUntilGameTime);
        tag.putInt(DISTURB_COUNT_TAG, this.disturbCount);
        tag.putLong(FLOW_HIDDEN_UNTIL_TAG, this.flowHiddenUntilCalendarTick);
        tag.putLong(SURGE_COOLDOWN_TAG, this.surgeCooldownUntilGameTime);
        tag.put(LINK_RULES_TAG, this.linkRules.copy());
        return tag;
    }

    @Nullable
    public static FormationInstance fromNbt(CompoundTag tag) {
        if (!tag.hasUUID(OWNER_TAG)) {
            return null;
        }
        ResourceLocation formationId = ResourceLocation.tryParse(tag.getString(FORMATION_ID_TAG));
        if (formationId == null) {
            return null;
        }
        FormationInstance instance = new FormationInstance(
                NbtUtils.readBlockPos(tag.getCompound(DISC_POS_TAG)),
                formationId,
                tag.getUUID(OWNER_TAG),
                tag.getString(OWNER_NAME_TAG)
        );
        instance.efficiency = tag.getDouble(EFFICIENCY_TAG);
        ListTag flagTags = tag.getList(FLAG_POSITIONS_TAG, Tag.TAG_COMPOUND);
        for (Tag flagTag : flagTags) {
            instance.flagPositions.add(NbtUtils.readBlockPos((CompoundTag) flagTag));
        }
        for (byte elementIndex : tag.getByteArray(FLAG_ELEMENTS_TAG)) {
            if (elementIndex >= 0 && elementIndex < ElementType.FIVE_ELEMENTS.size()) {
                instance.flagElements.add(ElementType.FIVE_ELEMENTS.get(elementIndex));
            } else {
                instance.flagElements.add(ElementType.METAL);
            }
        }
        instance.lifeGateIndex = tag.getInt(LIFE_GATE_TAG);
        instance.enlightened = tag.getBoolean(ENLIGHTENED_TAG);
        ListTag whitelistTags = tag.getList(WHITELIST_TAG, Tag.TAG_INT_ARRAY);
        for (Tag whitelistTag : whitelistTags) {
            instance.whitelist.add(NbtUtils.loadUUID(whitelistTag));
        }
        instance.paralyzedUntilGameTime = tag.getLong(PARALYZED_UNTIL_TAG);
        instance.disturbCount = tag.getInt(DISTURB_COUNT_TAG);
        instance.flowHiddenUntilCalendarTick = tag.getLong(FLOW_HIDDEN_UNTIL_TAG);
        instance.surgeCooldownUntilGameTime = tag.getLong(SURGE_COOLDOWN_TAG);
        if (tag.contains(LINK_RULES_TAG, Tag.TAG_LIST)) {
            instance.linkRules.addAll(tag.getList(LINK_RULES_TAG, Tag.TAG_COMPOUND));
        }
        return instance;
    }
}
