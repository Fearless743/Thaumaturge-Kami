package mod.emt.kami.items.armor;

import com.leclowndu93150.thaumaturge.api.items.IVisDiscountGear;
import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import mod.emt.kami.items.IDyeableGear;
import mod.emt.kami.items.ModMaterials;
import mod.emt.kami.items.base.ItemBaseArmor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * The dyeable Ichorweave robe set. The dyed colour is stored in the vanilla {@link DyedItemColor}
 * component so it behaves like leather armour (including the "dyed" tooltip). A filled cauldron
 * washes the dye back out, matching the original item's right-click behaviour.
 */
public class ItemIchorweaveArmor extends ItemBaseArmor implements IDyeableGear, IWarpingGear, IVisDiscountGear {
    public static final int DEFAULT_DYED_COLOR = 12607488;

    private final EquipmentSlot slot;

    public ItemIchorweaveArmor(EquipmentSlot slot, Item.Properties properties) {
        this(slot, ModMaterials.ICHORWEAVE, properties);
    }

    protected ItemIchorweaveArmor(EquipmentSlot slot, ArmorMaterial material, Item.Properties properties) {
        super(applyProperties(properties, slot, material));
        this.slot = slot;
    }

    private static Item.Properties applyProperties(Item.Properties properties, EquipmentSlot slot, ArmorMaterial material) {
        Item.Properties result = properties.fireResistant().humanoidArmor(material, armorType(slot));
        if (slot == EquipmentSlot.HEAD) {
            //Replaces the legacy IGoggles marker: Thaumaturge treats any stack carrying this
            //component as revealing goggles (see GogglesAccess.isRevealing).
            result = result.component(TTDataComponents.GOGGLES_UPGRADE.get(), Unit.INSTANCE);
        }
        return result;
    }

    protected static ArmorType armorType(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> ArmorType.HELMET;
            case CHEST -> ArmorType.CHESTPLATE;
            case LEGS -> ArmorType.LEGGINGS;
            case FEET -> ArmorType.BOOTS;
            default -> ArmorType.BODY;
        };
    }

    public EquipmentSlot getEquipmentSlot() {
        return this.slot;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        //Right-clicking a filled cauldron washes the dye out of the armour.
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        if (this.slot == EquipmentSlot.HEAD) {
            consumer.accept(Component.translatable("tooltip.kami.revealing"));
        }
        super.appendHoverText(stack, context, display, consumer, flag);
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
    public int getVisDiscount(ItemStack stack) {
        return 7;
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 1;
    }
}
