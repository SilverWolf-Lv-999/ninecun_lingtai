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
     * 施展法术。灵力检查与扣除由 SpellCastManager 完成；返回 false 表示施法未成，灵力将退还。
     */
    public abstract boolean cast(ServerPlayer player, SpellCastContext context);
}
