package mod.emt.kami.items;

import mod.emt.kami.Kami;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * Tool and armor materials for the KAMI gear. Ichorium is a netherite-tier material that is
 * effectively unbreakable in the original mod (represented here by a very high durability).
 */
public final class ModMaterials {
    public static final ToolMaterial ICHORIUM = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3000, 11.0F, 5.0F, 22, ItemTags.NETHERITE_TOOL_MATERIALS);
    public static final ToolMaterial AWAKENED_ICHORIUM = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 6000, 15.0F, 7.0F, 25, ItemTags.NETHERITE_TOOL_MATERIALS);

    public static final ArmorMaterial ICHORWEAVE = new ArmorMaterial(
            37,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3),
            22, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F, ItemTags.NETHERITE_TOOL_MATERIALS, asset("ichorweave"));
    public static final ArmorMaterial AWAKENED_ICHORWEAVE = new ArmorMaterial(
            50,
            Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 9, ArmorType.LEGGINGS, 7, ArmorType.BOOTS, 4),
            25, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.2F, ItemTags.NETHERITE_TOOL_MATERIALS, asset("awakened_ichorweave"));

    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Kami.MOD_ID, name));
    }

    private ModMaterials() {}
}
