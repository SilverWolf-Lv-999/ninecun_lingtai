package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * owner 皮肤解析（R5）：优先取同维度在线玩家的皮肤；否则经 {@code SkinManager}
 * 异步获取（首帧可能是 Steve/Alex 占位，加载完成后自动过渡）。
 * 绝不在服务端查皮肤——本类仅客户端渲染器使用。
 */
final class OwnerSkinCache {
    private static final Map<UUID, ResourceLocation> RESOLVED_SKINS = new HashMap<>();
    private static final Set<UUID> PENDING = new java.util.HashSet<>();

    private OwnerSkinCache() {
    }

    static ResourceLocation resolve(UUID ownerUuid, String ownerName) {
        if (ownerUuid == null) {
            return DefaultPlayerSkin.getDefaultSkin();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null
                && minecraft.level.getPlayerByUUID(ownerUuid) instanceof AbstractClientPlayer onlineOwner) {
            return onlineOwner.getSkinTextureLocation();
        }
        ResourceLocation resolved = RESOLVED_SKINS.get(ownerUuid);
        if (resolved != null) {
            return resolved;
        }
        if (PENDING.add(ownerUuid)) {
            GameProfile profile = new GameProfile(
                    ownerUuid,
                    ownerName == null || ownerName.isBlank() ? "MindMirror" : ownerName
            );
            minecraft.getSkinManager().registerSkins(
                    profile,
                    (type, location, texture) -> {
                        if (type == MinecraftProfileTexture.Type.SKIN) {
                            RESOLVED_SKINS.put(ownerUuid, location);
                        }
                    },
                    true
            );
        }
        return DefaultPlayerSkin.getDefaultSkin(ownerUuid);
    }
}
