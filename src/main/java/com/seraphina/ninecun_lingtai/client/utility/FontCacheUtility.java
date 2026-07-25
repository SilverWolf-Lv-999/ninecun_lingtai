package com.seraphina.ninecun_lingtai.client.utility;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FontCacheUtility {
    private static final String HASH_ALGORITHM = "SHA-256";

    private FontCacheUtility() {
    }

    public static Path copyShanBeiFontToCache(ResourceManager resourceManager) throws IOException {
        byte[] resourceBytes;
        try (InputStream inputStream = resourceManager.open(CustomFontConstants.SHAN_BEI_FONT_RESOURCE)) {
            resourceBytes = inputStream.readAllBytes();
        }

        Path cacheDirectory = FMLPaths.GAMEDIR.get()
                .resolve("cache")
                .resolve(ModMain.MOD_ID)
                .resolve("fonts");
        Path cachedFontPath = cacheDirectory.resolve(CustomFontConstants.SHAN_BEI_CACHE_FILE_NAME);
        Files.createDirectories(cacheDirectory);

        if (isCurrentCache(cachedFontPath, resourceBytes)) {
            return cachedFontPath;
        }

        Path temporaryFontPath = Files.createTempFile(cacheDirectory, "shan_bei_font_", ".tmp");
        try {
            Files.write(temporaryFontPath, resourceBytes);
            moveReplacing(temporaryFontPath, cachedFontPath);
        } finally {
            Files.deleteIfExists(temporaryFontPath);
        }
        return cachedFontPath;
    }

    private static boolean isCurrentCache(Path cachedFontPath, byte[] resourceBytes) throws IOException {
        if (!Files.isRegularFile(cachedFontPath) || Files.size(cachedFontPath) != resourceBytes.length) {
            return false;
        }
        return MessageDigest.isEqual(digest(resourceBytes), digest(cachedFontPath));
    }

    private static byte[] digest(byte[] bytes) {
        return createDigest().digest(bytes);
    }

    private static byte[] digest(Path path) throws IOException {
        MessageDigest messageDigest = createDigest();
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int readLength;
            while ((readLength = inputStream.read(buffer)) != -1) {
                if (readLength > 0) {
                    messageDigest.update(buffer, 0, readLength);
                }
            }
        }
        return messageDigest.digest();
    }

    private static MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(HASH_ALGORITHM + " is unavailable", exception);
        }
    }

    private static void moveReplacing(Path sourcePath, Path targetPath) throws IOException {
        try {
            Files.move(
                    sourcePath,
                    targetPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
