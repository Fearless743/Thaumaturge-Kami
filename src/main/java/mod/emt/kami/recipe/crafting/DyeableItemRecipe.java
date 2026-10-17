package mod.emt.kami.recipe.crafting;

import com.mojang.serialization.MapCodec;
import mod.emt.kami.items.IDyeableGear;
import mod.emt.kami.registry.ModRecipeTypesKAMI;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Special crafting recipe that combines a single piece of dyeable KAMI gear with one or more dyes,
 * blending them into a single colour. Uses the vanilla {@link DyedItemColor} component so the
 * resulting colour matches the behaviour of leather armour dyeing.
 */
public class DyeableItemRecipe extends CustomRecipe {
    public static final DyeableItemRecipe INSTANCE = new DyeableItemRecipe();
    public static final MapCodec<DyeableItemRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasTarget = false;
        boolean hasDye = false;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof IDyeableGear) {
                if (hasTarget) {
                    return false;
                }
                hasTarget = true;
            } else if (stack.has(DataComponents.DYE)) {
                hasDye = true;
            } else {
                return false;
            }
        }

        return hasTarget && hasDye;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack target = ItemStack.EMPTY;
        List<DyeColor> dyes = new ArrayList<>();

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof IDyeableGear) {
                if (!target.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                target = stack;
            } else if (stack.has(DataComponents.DYE)) {
                dyes.add(stack.getOrDefault(DataComponents.DYE, DyeColor.WHITE));
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (target.isEmpty() || dyes.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = DyedItemColor.applyDyes(target, dyes);
        if (result.getItem() instanceof IDyeableGear gear) {
            gear.setDyedColor(result, result.getOrDefault(DataComponents.DYED_COLOR, new DyedItemColor(0)).rgb());
        }
        return result;
    }

    @Override
    public RecipeSerializer<DyeableItemRecipe> getSerializer() {
        return ModRecipeTypesKAMI.DYEABLE_ITEM_SERIALIZER.get();
    }
}
