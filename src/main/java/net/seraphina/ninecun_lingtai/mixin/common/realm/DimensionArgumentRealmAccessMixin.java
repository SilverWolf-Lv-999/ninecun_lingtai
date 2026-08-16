package net.seraphina.ninecun_lingtai.mixin.common.realm;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(DimensionArgument.class)
public abstract class DimensionArgumentRealmAccessMixin {
    @Redirect(
            method = "getDimension",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;"
                            + "getLevel(Lnet/minecraft/resources/ResourceKey;)"
                            + "Lnet/minecraft/server/level/ServerLevel;"
            )
    )
    private static ServerLevel rejectThreeThousandRealmDimensionCommand(
            MinecraftServer server,
            ResourceKey<Level> levelKey
    ) {
        if (ThreeThousandRealmManager.get(server).isThreeThousandRealm(levelKey)) {
            return null;
        }
        return server.getLevel(levelKey);
    }

    @Redirect(
            method = "listSuggestions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/commands/SharedSuggestionProvider;"
                            + "levels()Ljava/util/Set;"
            )
    )
    private <S> Set<ResourceKey<Level>> hideThreeThousandRealmDimensionSuggestions(
            SharedSuggestionProvider suggestionProvider,
            CommandContext<S> context,
            SuggestionsBuilder builder
    ) {
        Set<ResourceKey<Level>> levels = suggestionProvider.levels();
        if (!(suggestionProvider instanceof CommandSourceStack commandSourceStack)) {
            return levels;
        }

        ThreeThousandRealmManager realmManager =
                ThreeThousandRealmManager.get(commandSourceStack.getServer());
        Set<ResourceKey<Level>> filteredLevels = new LinkedHashSet<>();
        for (ResourceKey<Level> levelKey : levels) {
            if (!realmManager.isThreeThousandRealm(levelKey)) {
                filteredLevels.add(levelKey);
            }
        }
        return filteredLevels;
    }
}
