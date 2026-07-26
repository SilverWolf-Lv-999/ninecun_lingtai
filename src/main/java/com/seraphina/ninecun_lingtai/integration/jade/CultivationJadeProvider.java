package com.seraphina.ninecun_lingtai.integration.jade;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import com.seraphina.ninecun_lingtai.utility.ChineseNumberUtility;
import com.seraphina.ninecun_lingtai.utility.CreatureCultivationUtility;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Optional;

/**
 * Shows a creature's cultivation stage and spiritual root, but only to players whose own major
 * realm is at least the target's — a weaker cultivator cannot fathom a stronger one.
 */
public final class CultivationJadeProvider implements
        IEntityComponentProvider,
        IServerDataProvider<EntityAccessor> {
    public static final CultivationJadeProvider INSTANCE = new CultivationJadeProvider();

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "cultivation"
    );
    private static final String REALM_KEY = "ninecunLingtaiCultivationRealm";
    private static final String LAYER_KEY = "ninecunLingtaiCultivationLayer";
    private static final String ROOT_ELEMENT_MASK_KEY = "ninecunLingtaiSpiritualRootElementMask";
    private static final String ROOT_LENGTH_INCHES_KEY = "ninecunLingtaiSpiritualRootLengthInches";

    private CultivationJadeProvider() {
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        Optional<CultivationStage> stage = CreatureCultivationData.getStage(entity);
        if (stage.isEmpty() || !CreatureCultivationUtility.canInspect(accessor.getPlayer(), stage.get())) {
            return;
        }
        data.putString(REALM_KEY, stage.get().realm().getSerializedName());
        data.putInt(LAYER_KEY, stage.get().layer());
        CreatureCultivationData.getSpiritualRoot(entity).ifPresent(spiritualRoot -> {
            data.putInt(ROOT_ELEMENT_MASK_KEY, spiritualRoot.getElementMask());
            data.putFloat(ROOT_LENGTH_INCHES_KEY, spiritualRoot.lengthInches());
        });
    }

    @Override
    public void appendTooltip(
            ITooltip tooltip,
            EntityAccessor accessor,
            IPluginConfig config
    ) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(REALM_KEY, Tag.TAG_STRING)) {
            return;
        }
        Optional<CultivationRealm> realm = CultivationRealm.fromSerializedName(data.getString(REALM_KEY));
        if (realm.isEmpty()) {
            return;
        }
        CultivationStage stage;
        try {
            stage = new CultivationStage(realm.get(), data.getInt(LAYER_KEY));
        } catch (IllegalArgumentException exception) {
            return;
        }
        tooltip.add(
                Component.translatable(
                                "jade.ninecun_lingtai.cultivation",
                                CultivationUtility.getDisplayName(stage)
                        )
                        .withStyle(ChatFormatting.GRAY),
                UID
        );
        if (!data.contains(ROOT_ELEMENT_MASK_KEY, Tag.TAG_ANY_NUMERIC)) {
            return;
        }
        SpiritualRoot.fromElementMask(
                data.getInt(ROOT_ELEMENT_MASK_KEY),
                data.getFloat(ROOT_LENGTH_INCHES_KEY)
        ).ifPresent(spiritualRoot -> tooltip.add(
                Component.translatable(
                                "jade.ninecun_lingtai.spiritual_root",
                                SpiritualRootUtility.getSpiritualRootName(spiritualRoot.elements()),
                                ChineseNumberUtility.formatTenths(spiritualRoot.lengthInches(), "寸", "分")
                        )
                        .withStyle(ChatFormatting.GRAY),
                UID
        ));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
