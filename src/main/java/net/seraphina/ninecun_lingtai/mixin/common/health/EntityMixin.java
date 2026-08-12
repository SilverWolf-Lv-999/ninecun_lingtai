package net.seraphina.ninecun_lingtai.mixin.common.health;

import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    public boolean ninecunLingtai$protectAlive(boolean original) {
        Entity entity = (Entity) (Object) this;
        return CultivationDefenseUtility.getProtectedAlive(entity, original);
    }
}
