package mod.emt.kami.items.tools;

import com.google.common.collect.ImmutableList;
import mod.emt.kami.api.item.IAreaBreakTool;
import mod.emt.kami.registry.ModDataComponentsKAMI;
import mod.emt.kami.registry.ModSoundsKAMI;
import mod.emt.kami.utils.helpers.HarvestHelper;
import mod.emt.kami.utils.helpers.PlayerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The Awakened Ichorium Shovel. Charges to bury nearby creatures (driving them into the ground)
 * and sweeps a 3x3 area of diggable blocks.
 *
 * <p>The Thaumcraft {@code FXDispatcher} is gone, so the charge feedback in {@link #inventoryTick}
 * uses the vanilla action-bar message only and the block-crack particles are emitted directly.</p>
 */
public class ItemAwakenedShovel extends ItemIchoriumShovel implements IAreaBreakTool {
    public static final int MAX_DEPTH = 15;
    public static final int USE_TICKS = 72000;

    public ItemAwakenedShovel(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (entity instanceof Player player && player.getUseItem() == stack && player.getUseItemRemainingTicks() > 0) {
            int duration = player.getTicksUsingItem();
            int stages = Math.min(MAX_DEPTH, duration / 10);
            if (stages > 0 && duration % 10 == 0) {
                EnumBuryMode mode = EnumBuryMode.getMode(stack);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.bury_area", stages)
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
                EnumBuryMode mode = EnumBuryMode.getMode(heldStack).nextMode();
                EnumBuryMode.setMode(heldStack, mode);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.bury_mode." + mode)
                                .withStyle(style -> style.withColor(mode.getTextColor())));
            }
            return InteractionResult.SUCCESS;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            // Entering the use state must be explicit on 26.1: returning CONSUME alone never
            // starts it, so the release hooks (the bury sweep) would never run.
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
        int depth = Math.min(MAX_DEPTH, duration / 10);
        EnumBuryMode mode = EnumBuryMode.getMode(stack);
        // Anchored on the feet block (like the 1.12 original): the area covers the player's own
        // block plus `depth` blocks on every side, i.e. a reach of 1+depth from the player.
        AABB area = new AABB(player.blockPosition()).inflate(depth, 2.0D, depth);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, mode::isEntityValid)) {
            if (!level.isClientSide()) {
                level.playSound(null, target.blockPosition(), ModSoundsKAMI.ITEM_ICHOR_BURY.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, target.blockPosition(), SoundEvents.GRAVEL_BREAK,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                double buriedY = target.getY() - Math.ceil(target.getBbHeight() + 1.0D);
                target.teleportTo(target.getX(), buriedY, target.getZ());
            } else {
                BlockState below = level.getBlockState(target.blockPosition().below());
                if (!below.isAir()) {
                    for (int i = 0; i < 12; i++) {
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below),
                                target.getX() + (level.getRandom().nextFloat() - 0.5D) * target.getBbWidth(),
                                target.getBoundingBox().minY + 0.1D,
                                target.getZ() + (level.getRandom().nextFloat() - 0.5D) * target.getBbWidth(),
                                -target.getDeltaMovement().x * 4.0D, 1.5D, -target.getDeltaMovement().z * 4.0D);
                    }
                }
            }
        }
        return true;
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
        EnumBuryMode mode = EnumBuryMode.getMode(stack);
        consumer.accept(Component.translatable("tooltip.kami.tool.aoe_mode", 3));
        consumer.accept(Component.translatable("tooltip.kami.tool.bury_mode." + mode)
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
                return HarvestHelper.getHarvestArea(player.level(), player, blockHit, 3, 1, includeOrigin, true);
            }
        }
        return ImmutableList.of();
    }

    @Override
    public boolean spawnDropsAtPlayer(Player player, ItemStack stack) {
        return true;
    }

    public enum EnumBuryMode {
        UNDEAD(ChatFormatting.GRAY, LivingEntity::isInvertedHealAndHarm),   //White
        ANIMALS(ChatFormatting.DARK_GREEN, entity -> entity instanceof Animal),  //Green
        HOSTILES(ChatFormatting.DARK_RED, entity -> entity instanceof Enemy),    //Red
        ALL(ChatFormatting.BLUE, entity -> true);                           //Blue

        private final ChatFormatting textColor;
        private final Predicate<LivingEntity> predicate;

        EnumBuryMode(ChatFormatting textColor, Predicate<LivingEntity> predicate) {
            this.textColor = textColor;
            this.predicate = predicate;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting getTextColor() {
            return this.textColor;
        }

        public boolean isEntityValid(LivingEntity entity) {
            return entity.isAlive()
                    && !(entity instanceof Player)
                    && entity.getType() != EntityType.ENDER_DRAGON
                    && entity.getType() != EntityType.WITHER
                    && this.predicate.test(entity);
        }

        public EnumBuryMode nextMode() {
            EnumBuryMode[] values = EnumBuryMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static EnumBuryMode getMode(ItemStack stack) {
            EnumBuryMode[] values = EnumBuryMode.values();
            int ordinal = stack.getOrDefault(ModDataComponentsKAMI.BURY_MODE.get(), 0);
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }

        public static void setMode(ItemStack stack, EnumBuryMode mode) {
            stack.set(ModDataComponentsKAMI.BURY_MODE.get(), mode.ordinal());
        }
    }
}
