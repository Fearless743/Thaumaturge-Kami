package mod.emt.kami.items.tools;

import com.google.common.collect.ImmutableList;
import mod.emt.kami.api.item.IAreaBreakTool;
import mod.emt.kami.registry.ModDataComponentsKAMI;
import mod.emt.kami.registry.ModSoundsKAMI;
import mod.emt.kami.utils.helpers.HarvestHelper;
import mod.emt.kami.utils.helpers.PlayerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * The Awakened Ichorium Pickaxe. Charge it to mine a cube of blocks whose depth grows with the
 * charge time, and use the selected area size for the footprint. Also sweeps a small area around
 * any single block it breaks.
 *
 * <p>The legacy {@code onUpdate} charge feedback became {@link #inventoryTick} (server-only) and
 * the Thaumcraft {@code FXDispatcher} charge particles were dropped as there is no Thaumaturge
 * equivalent.</p>
 */
public class ItemAwakenedPickaxe extends ItemIchoriumPickaxe implements IAreaBreakTool {
    public static final int MAX_DEPTH = 15;
    public static final int USE_TICKS = 72000;

    public ItemAwakenedPickaxe(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (entity instanceof Player player && player.getUseItem() == stack && player.getUseItemRemainingTicks() > 0) {
            int duration = player.getTicksUsingItem();
            int stages = Math.min(MAX_DEPTH, duration / 10);
            if (stages > 0 && duration % 10 == 0) {
                EnumAoeMode mode = EnumAoeMode.getMode(stack);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.dig_depth", stages)
                                .withStyle(style -> style.withColor(mode.getTextColor())));
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        HitResult trace = PlayerHelper.rayTrace(player, 0);
        if (player.isShiftKeyDown() && (trace == null || trace.getType() == HitResult.Type.MISS)) {
            if (level.isClientSide()) {
                level.playSound(player, player.blockPosition(), ModSoundsKAMI.ITEM_ICHOR_TOGGLE.get(),
                        SoundSource.PLAYERS, 1.0F, 1.5F);
            } else {
                EnumAoeMode mode = EnumAoeMode.getMode(heldStack).nextMode();
                EnumAoeMode.setMode(heldStack, mode);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.aoe_mode", mode.getBreakAreaSize())
                                .withStyle(style -> style.withColor(mode.getTextColor())));
            }
            return InteractionResult.SUCCESS;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            // Entering the use state must be explicit on 26.1: returning CONSUME alone never
            // starts it, so the release hooks (the area break) would never run.
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        int duration = this.getUseDuration(stack, entity) - remainingTime;
        if (entity instanceof Player player && duration > 10) {
            HitResult trace = PlayerHelper.rayTrace(player, 0);
            if (trace instanceof BlockHitResult blockHit) {
                int diameter = this.getBreakAreaSize(stack);
                int depth = Math.min(MAX_DEPTH, duration / 10);
                ImmutableList<BlockPos> harvestPositions =
                        HarvestHelper.getHarvestArea(level, player, blockHit, diameter, depth, true, false);
                level.playSound(null, player.blockPosition(), ModSoundsKAMI.ITEM_ICHOR_DIG.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                HarvestHelper.harvestExtraBlocks(player, stack, harvestPositions);
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        EnumAoeMode mode = EnumAoeMode.getMode(stack);
        consumer.accept(Component.translatable("tooltip.kami.tool.aoe_mode", mode.getBreakAreaSize())
                .withStyle(style -> style.withColor(mode.getTextColor())));
        super.appendHoverText(stack, context, display, consumer, flag);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && owner instanceof Player player && !player.isShiftKeyDown()) {
            HitResult trace = PlayerHelper.rayTrace(player, 0);
            if (trace instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(pos)) {
                HarvestHelper.harvestExtraBlocks(player, stack,
                        this.getBreakAreaPositions(player, stack, pos, false));
            }
        }
        return super.mineBlock(stack, level, state, pos, owner);
    }

    @Override
    public ImmutableList<BlockPos> getBreakAreaPositions(Player player, ItemStack stack, BlockPos origin, boolean includeOrigin) {
        if (!player.isShiftKeyDown() && !player.level().getBlockState(origin).isAir()) {
            HitResult trace = PlayerHelper.rayTrace(player, 0);
            if (trace instanceof BlockHitResult blockHit) {
                int diameter = this.getBreakAreaSize(stack);
                return HarvestHelper.getHarvestArea(player.level(), player, blockHit, diameter, 1, includeOrigin, false);
            }
        }
        return ImmutableList.of();
    }

    @Override
    public boolean spawnDropsAtPlayer(Player player, ItemStack stack) {
        return true;
    }

    public int getBreakAreaSize(ItemStack stack) {
        return EnumAoeMode.getMode(stack).getBreakAreaSize();
    }

    public enum EnumAoeMode {
        ONE(1, ChatFormatting.GRAY),
        THREE(3, ChatFormatting.BLUE),
        FIVE(5, ChatFormatting.DARK_GREEN),
        SEVEN(7, ChatFormatting.DARK_RED);

        private final int breakAreaSize;
        private final ChatFormatting textColor;

        EnumAoeMode(int breakAreaSize, ChatFormatting textColor) {
            this.breakAreaSize = breakAreaSize;
            this.textColor = textColor;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public int getBreakAreaSize() {
            return this.breakAreaSize;
        }

        public ChatFormatting getTextColor() {
            return this.textColor;
        }

        public EnumAoeMode nextMode() {
            EnumAoeMode[] values = EnumAoeMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static EnumAoeMode getMode(ItemStack stack) {
            EnumAoeMode[] values = EnumAoeMode.values();
            int ordinal = stack.getOrDefault(ModDataComponentsKAMI.AOE_MODE.get(), 0);
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }

        public static void setMode(ItemStack stack, EnumAoeMode mode) {
            stack.set(ModDataComponentsKAMI.AOE_MODE.get(), mode.ordinal());
        }
    }
}
