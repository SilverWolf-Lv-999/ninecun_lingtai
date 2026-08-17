package net.seraphina.ninecun_lingtai.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.seraphina.ninecun_lingtai.client.SeraFont;
import net.seraphina.ninecun_lingtai.client.render.BookRenderer;
import net.seraphina.ninecun_lingtai.common.data.BookManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.utility.BookItemUtility;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class BookItem extends Item {
    public BookItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack itemStack) {
        Optional<String> title = BookItemUtility.getTitle(itemStack);
        if (title.isPresent()) {
            return Component.literal(title.get());
        }
        return super.getName(itemStack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(itemStack);
        }

        Optional<ResourceLocation> bookId = BookItemUtility.getBookId(itemStack);
        if (bookId.isEmpty() || BookManager.INSTANCE.getBook(bookId.get()).isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.missing_book"),
                    true
            );
            return InteractionResultHolder.fail(itemStack);
        }

        ModNetwork.sendBookPage((ServerPlayer) player, bookId.get(), 0);
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Level level,
            List<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
        if (tooltipFlag.isAdvanced()) {
            BookItemUtility.getBookId(itemStack).ifPresent(bookId -> tooltip.add(
                    Component.literal(bookId.toString()).withStyle(ChatFormatting.DARK_GRAY)
            ));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return BookRenderer.INSTANCE;
            }

            @Override
            public Font getFont(ItemStack itemStack, FontContext context) {
                return SeraFont.get(ItemFontStyle.BOOK);
            }
        });
    }
}
