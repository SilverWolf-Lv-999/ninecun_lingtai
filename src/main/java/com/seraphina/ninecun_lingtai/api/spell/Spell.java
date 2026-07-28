package com.seraphina.ninecun_lingtai.api.spell;

import net.minecraft.server.level.ServerPlayer;

/**
 * 功能类法术的"神通实现"。法术本身是一门普通的数据驱动功法（data/&lt;namespace&gt;/techniques，
 * 带 {@code spell} 块），需要烙印、参悟并通过使用积累熟练度；此类只承载施展时的实际行为，
 * 像物品一样通过 DeferredRegister / RegistryObject 注册（见 ModSpells），由功法 JSON 的
 * {@code spell.id} 引用。
 */
public abstract class Spell {
    /**
     * 若法术当前正以同一种形态生效，则主动解除它。
     *
     * <p>此检查发生在灵力扣除之前；返回 {@code true} 时本次操作只负责收法，
     * 不消耗灵力，也不增加熟练度。非持续型法术保持默认实现即可。</p>
     */
    public boolean cancelIfActive(ServerPlayer player) {
        return false;
    }

    /**
     * 施展法术。灵力检查与扣除由 SpellCastManager 完成；返回 false 表示施法未成，灵力将退还。
     */
    public abstract boolean cast(ServerPlayer player, SpellCastContext context);
}
