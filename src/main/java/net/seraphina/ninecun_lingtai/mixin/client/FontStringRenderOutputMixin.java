package net.seraphina.ninecun_lingtai.mixin.client;

import net.minecraft.network.chat.Style;
import net.seraphina.ninecun_lingtai.utility.DaoTitleGradientUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.client.gui.Font$StringRenderOutput")
public abstract class FontStringRenderOutputMixin {
    @ModifyVariable(
            method = "accept(ILnet/minecraft/network/chat/Style;I)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Style ninecunLingtai$applyDaoTitleGradient(Style style) {
        int color = DaoTitleGradientUtility.getColor(style);
        return color < 0 ? style : style.withColor(color);
    }
}
