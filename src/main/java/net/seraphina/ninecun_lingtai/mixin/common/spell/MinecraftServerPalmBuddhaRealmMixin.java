package net.seraphina.ninecun_lingtai.mixin.common.spell;

import net.minecraft.server.MinecraftServer;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaRealmManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在服务器创建世界前注册已持久化的掌中佛国维度，避免新增 Forge 生命周期事件。
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerPalmBuddhaRealmMixin {
    @Inject(method = "createLevels", at = @At("HEAD"))
    private void ninecunLingtai$registerPalmBuddhaRealmDefinitions(CallbackInfo callbackInfo) {
        PalmBuddhaRealmManager.registerPersistedRealmDefinitions((MinecraftServer) (Object) this);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void ninecunLingtai$clearPalmBuddhaRealmManager(CallbackInfo callbackInfo) {
        PalmBuddhaRealmManager.clear((MinecraftServer) (Object) this);
    }
}
