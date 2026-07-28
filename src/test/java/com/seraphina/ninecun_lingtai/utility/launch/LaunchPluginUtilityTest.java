package com.seraphina.ninecun_lingtai.utility.launch;

import cpw.mods.modlauncher.ModuleLayerHandler;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.NamedPath;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LaunchPluginUtilityTest {
    @Test
    void readsModLauncherRecordBackedLayersThroughPublicApi() throws ReflectiveOperationException {
        var constructor = ModuleLayerHandler.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        IModuleLayerManager moduleLayerManager = constructor.newInstance();

        List<ModuleLayer> completedLayers = assertDoesNotThrow(
                () -> LaunchPluginUtility.getCompletedModuleLayers(moduleLayerManager)
        );

        assertNotNull(completedLayers);
    }

    @Test
    void removesUnionCodeSourceFromTransformerExclusions() {
        Path codeSourcePath = LaunchPluginUtility.normalizeCodeSourcePath(
                "union:/D:/minecraft/mods/ninecun%20lingtai.jar%231!/"
        );
        NamedPath ownJar = new NamedPath(
                "cpw.mods.modlauncher.api.ITransformationService",
                Path.of("D:/minecraft/mods/ninecun lingtai.jar")
        );
        NamedPath otherJar = new NamedPath(
                "cpw.mods.modlauncher.api.ITransformationService",
                Path.of("D:/minecraft/mods/other.jar")
        );
        List<NamedPath> found = new ArrayList<>(List.of(ownJar, otherJar));

        LaunchPluginUtility.removeFromFoundPaths(found, codeSourcePath);

        assertEquals(
                List.of(otherJar),
                found
        );
    }
}
