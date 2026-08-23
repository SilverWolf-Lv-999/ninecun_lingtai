package net.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.seraphina.ninecun_lingtai.common.entity.ability.SwordQiEntity;
import net.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;

public final class FunctionDebugCommand {
    private static final int FUNCTION_PERMISSION_LEVEL = 4;
    private static final double DEFAULT_ANGLE_DEGREES = 60.0D;

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("function")
                .requires(source -> source.hasPermission(FUNCTION_PERMISSION_LEVEL))
                .then(Commands.literal("sword_qi")
                        .executes(context -> releaseSwordQi(
                                context.getSource(),
                                DEFAULT_ANGLE_DEGREES
                        ))
                        .then(Commands.argument("angle", DoubleArgumentType.doubleArg())
                                .executes(context -> releaseSwordQi(
                                        context.getSource(),
                                        DoubleArgumentType.getDouble(context, "angle")
                                ))));
    }

    private static int releaseSwordQi(CommandSourceStack commandSource, double angleDegrees)
            throws CommandSyntaxException {
        ServerPlayer player = commandSource.getPlayerOrException();
        Vec3 direction = player.getLookAngle().normalize();
        SwordQiEntity swordQiEntity = ModEntityTypes.SWORD_QI_ENTITY.get().create(player.serverLevel());
        if (swordQiEntity == null) {
            throw new IllegalStateException("Unable to create sword qi entity");
        }
        swordQiEntity.setPos(player.getEyePosition().add(direction.scale(0.55D)));
        swordQiEntity.initialize(direction, (float) angleDegrees);
        player.serverLevel().addFreshEntity(swordQiEntity);
        commandSource.sendSuccess(
                () -> Component.literal("已释放剑气实体，弧形角度：" + angleDegrees + "°"),
                false
        );
        return Command.SINGLE_SUCCESS;
    }
}
