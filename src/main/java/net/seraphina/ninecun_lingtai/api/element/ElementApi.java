package net.seraphina.ninecun_lingtai.api.element;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class ElementApi {
    private static final ElementalPower ROOTLESS_PLAYER_ELEMENT = new ElementalPower(ElementType.METAL, 1.0D);
    private static final List<ElementalEntityProvider> ENTITY_PROVIDERS = new CopyOnWriteArrayList<>();

    public static void registerEntityProvider(ElementalEntityProvider provider) {
        ENTITY_PROVIDERS.add(Objects.requireNonNull(provider, "provider"));
    }

    public static Optional<ElementalPower> getAttackElement(LivingEntity entity) {
        return resolveElement(entity, provider -> provider.getAttackElement(entity));
    }

    public static Optional<ElementalPower> getDefenseElement(LivingEntity entity) {
        return resolveElement(entity, provider -> provider.getDefenseElement(entity));
    }

    private static Optional<ElementalPower> resolveElement(
            LivingEntity entity,
            Function<ElementalEntityProvider, Optional<ElementalPower>> resolver
    ) {
        for (ElementalEntityProvider provider : ENTITY_PROVIDERS) {
            Optional<ElementalPower> elementalPower = resolver.apply(provider);
            if (elementalPower.isPresent()) {
                return elementalPower;
            }
        }
        if (entity instanceof Player) {
            return Optional.of(ROOTLESS_PLAYER_ELEMENT);
        }
        return Optional.empty();
    }
}
