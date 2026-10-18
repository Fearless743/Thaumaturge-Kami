package mod.emt.kami.items.tools;

import com.google.common.collect.ImmutableList;
import mod.emt.kami.api.item.IAreaBreakTool;
import mod.emt.kami.registry.ModDataComponentsKAMI;
import mod.emt.kami.registry.ModSoundsKAMI;
import mod.emt.kami.utils.helpers.HarvestHelper;
import mod.emt.kami.utils.helpers.PlayerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * The Awakened Ichorium Sickle. Charge it to sweep an area around the player; the selected
 * {@link EnumHarvestMode} decides what the sweep does (reap plants, strip leaves, sow, or behead).
 */
public class ItemAwakenedSickle extends ItemIchoriumHoe implements IAreaBreakTool {
    public static final int MAX_AREA = 15;
    public static final int USE_TICKS = 72000;

    public ItemAwakenedSickle(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (entity instanceof Player player && player.getUseItem() == stack && player.getUseItemRemainingTicks() > 0) {
            int duration = player.getTicksUsingItem();
            int stages = Math.min(MAX_AREA, duration / 10);
            if (stages > 0 && duration % 10 == 0) {
                EnumHarvestMode mode = EnumHarvestMode.getMode(stack);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.harvest_area", stages)
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
                EnumHarvestMode mode = EnumHarvestMode.getMode(heldStack).nextMode();
                EnumHarvestMode.setMode(heldStack, mode);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.harvest_mode." + mode)
                                .withStyle(style -> style.withColor(mode.getTextColor())));
            }
            return InteractionResult.SUCCESS;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            // Entering the use state must be explicit on 26.1: returning CONSUME alone never
            // starts it, so the release hook (the harvest sweep) would never run.
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        int duration = this.getUseDuration(stack, entity) - remainingTime;
        if (!(entity instanceof Player player) || duration <= 10) {
            return false;
        }
        int radius = Math.min(MAX_AREA, duration / 10);
        AABB area = player.getBoundingBox().inflate(radius, 3.0D, radius);
        EnumHarvestMode mode = EnumHarvestMode.getMode(stack);
        switch (mode) {
            case GRASSLANDS -> this.attemptPlantReap(level, player, area);
            case CANOPY -> {
                this.attemptDestroyCanopy(level, player, radius);
                // Original 1.12 code intentionally fell through from CANOPY into SOW.
                this.attemptTillAndPlant(level, player, area, player.getOffhandItem());
            }
            case SOW -> this.attemptTillAndPlant(level, player, area, player.getOffhandItem());
            case REAP -> this.attemptHarvest(level, player, area);
            case BEHEAD -> this.attemptBeheading(level, player, area);
        }
        return true;
    }

    private void attemptPlantReap(Level level, Player player, AABB area) {
        if (!level.isClientSide()) {
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
                    BlockPos.containing(area.maxX - 1.0D, area.maxY, area.maxZ - 1.0D))) {
                BlockState state = level.getBlockState(pos);
                if (isReapablePlant(state) && state.getDestroySpeed(level, pos) == 0.0F) {
                    HarvestHelper.attemptHarvestBlock(level, player, pos);
                }
            }
        }
    }

    private void attemptTillAndPlant(Level level, Player player, AABB area, ItemStack offhandStack) {
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
                BlockPos.containing(area.maxX - 1.0D, area.maxY, area.maxZ - 1.0D))) {
            boolean planted = this.attemptPlant(level, pos, player, offhandStack);
            if (!planted) {
                this.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
                this.attemptPlant(level, pos, player, offhandStack);
            }
        }
    }

    private boolean attemptPlant(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        UseOnContext context = new UseOnContext(player, InteractionHand.OFF_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        return stack.useOn(context).consumesAction();
    }

    private void attemptHarvest(Level level, Player player, AABB area) {
        // TODO(port): do basic crop harvesting here. The original 1.12 implementation was also a
        // no-op stub; the MI crop handling that was meant to expand it never landed.
    }

    private void attemptDestroyCanopy(Level level, Player player, int radius) {
        BlockPos start = player.blockPosition().offset(-radius, -2, -radius);
        BlockPos end = player.blockPosition().offset(radius, radius, radius);
        for (BlockPos pos : BlockPos.betweenClosed(start, end)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LEAVES)) {
                HarvestHelper.attemptHarvestBlock(level, player, pos);
            }
        }
    }

    private void attemptBeheading(Level level, Player player, AABB area) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!this.isBeheadingTarget(player, target)) {
                continue;
            }
            player.attack(target);
            // TODO(port): guarantee a head drop on a killing blow. Requires modded loot handling
            // that has no Thaumaturge/26.1 equivalent; the base attack still applies.
        }
    }

    private boolean isBeheadingTarget(Player player, LivingEntity entity) {
        return entity.isAlive()
                && !entity.isInvulnerable()
                && !(entity instanceof Player)
                && !(entity instanceof OwnableEntity ownable && ownable.getOwner() == player);
    }

    /** Whether a block should be swept by the GRASSLANDS reap mode. */
    private static boolean isReapablePlant(BlockState state) {
        return state.getBlock() instanceof BushBlock || state.is(BlockTags.CROPS) || state.is(BlockTags.FLOWERS);
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
        EnumHarvestMode mode = EnumHarvestMode.getMode(stack);
        consumer.accept(Component.translatable("tooltip.kami.tool.aoe_mode", 3));
        consumer.accept(Component.translatable("tooltip.kami.tool.harvest_mode." + mode)
                .withStyle(style -> style.withColor(mode.getTextColor())));
        super.appendHoverText(stack, context, display, consumer, flag);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && owner instanceof Player player && !player.isShiftKeyDown()) {
            HitResult trace = PlayerHelper.rayTrace(player, 0);
            if (trace instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(pos)) {
                for (BlockPos harvestPos : this.getBreakAreaPositions(player, stack, pos, false)) {
                    BlockState harvestState = level.getBlockState(harvestPos);
                    if (isReapablePlant(harvestState) && harvestState.getDestroySpeed(level, harvestPos) == 0.0F) {
                        HarvestHelper.attemptHarvestBlock(level, player, harvestPos);
                    }
                }
            }
        }
        return super.mineBlock(stack, level, state, pos, owner);
    }

    @Override
    public ImmutableList<BlockPos> getBreakAreaPositions(Player player, ItemStack stack, BlockPos origin, boolean includeOrigin) {
        if (!player.isShiftKeyDown() && !player.level().getBlockState(origin).isAir()) {
            HitResult trace = PlayerHelper.rayTrace(player, 0);
            if (trace instanceof BlockHitResult blockHit) {
                return HarvestHelper.getHarvestArea(player.level(), player, blockHit, 3, 1, includeOrigin, false);
            }
        }
        return ImmutableList.of();
    }

    @Override
    public boolean spawnDropsAtPlayer(Player player, ItemStack stack) {
        return true;
    }

    public enum EnumHarvestMode {
        GRASSLANDS(ChatFormatting.GRAY),
        CANOPY(ChatFormatting.DARK_PURPLE),
        SOW(ChatFormatting.DARK_GREEN),
        REAP(ChatFormatting.BLUE),
        BEHEAD(ChatFormatting.DARK_RED);

        private final ChatFormatting textColor;

        EnumHarvestMode(ChatFormatting textColor) {
            this.textColor = textColor;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting getTextColor() {
            return this.textColor;
        }

        public EnumHarvestMode nextMode() {
            EnumHarvestMode[] values = EnumHarvestMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static EnumHarvestMode getMode(ItemStack stack) {
            EnumHarvestMode[] values = EnumHarvestMode.values();
            int ordinal = stack.getOrDefault(ModDataComponentsKAMI.HARVEST_MODE.get(), 0);
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }

        public static void setMode(ItemStack stack, EnumHarvestMode mode) {
            stack.set(ModDataComponentsKAMI.HARVEST_MODE.get(), mode.ordinal());
        }
    }
}
