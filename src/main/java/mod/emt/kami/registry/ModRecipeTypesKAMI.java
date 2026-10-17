package mod.emt.kami.registry;

import mod.emt.kami.Kami;
import mod.emt.kami.recipe.crafting.DyeableItemRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeTypesKAMI {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Kami.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DyeableItemRecipe>> DYEABLE_ITEM_SERIALIZER =
            RECIPE_SERIALIZERS.register("dyeable_item",
                    () -> new RecipeSerializer<>(DyeableItemRecipe.MAP_CODEC, StreamCodec.unit(DyeableItemRecipe.INSTANCE)));

    private ModRecipeTypesKAMI() {}
}
