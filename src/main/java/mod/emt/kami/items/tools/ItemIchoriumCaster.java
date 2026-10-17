package mod.emt.kami.items.tools;

import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import mod.emt.kami.items.IDyeableGear;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The dyeable Ichorium Caster.
 *
 * <p>The 1.12 item extended ThaumicAPI's {@code AbstractItemCaster}. Thaumaturge has no equivalent
 * "caster" item class: spell casting is provided by the assembled wand ({@code ItemWand} +
 * rod/cap data), and {@code ICaster} is a capability-style behaviour rather than a base class.
 * Without the rod/cap assembly data a subclassed wand would be non-functional, so this is ported
 * as a dyeable trophy item that keeps its warp value and dyeable colour. Its reduced vis
 * consumption can be revisited once a Thaumaturge wand-part integration exists.</p>
 */
public class ItemIchoriumCaster extends Item implements IDyeableGear, IWarpingGear {
    public static final int DEFAULT_DYED_COLOR = 16748295;

    public ItemIchoriumCaster(Item.Properties properties) {
        super(properties.rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        //Right-clicking a filled cauldron washes the dye out.
        if (state.getBlock() instanceof LayeredCauldronBlock
                && state.getValue(LayeredCauldronBlock.LEVEL) > 0
                && getDyedColor(stack) != DEFAULT_DYED_COLOR) {
            setDyedColor(stack, DEFAULT_DYED_COLOR);
            if (!level.isClientSide()) {
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getDyedColor(ItemStack stack) {
        return stack.getOrDefault(DataComponents.DYED_COLOR, new DyedItemColor(DEFAULT_DYED_COLOR)).rgb();
    }

    @Override
    public void setDyedColor(ItemStack stack, int color) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 3;
    }
}
