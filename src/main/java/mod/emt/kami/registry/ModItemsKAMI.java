package mod.emt.kami.registry;

import mod.emt.kami.Kami;
import mod.emt.kami.items.armor.ItemAwakenedArmor;
import mod.emt.kami.items.armor.ItemIchorweaveArmor;
import mod.emt.kami.items.base.ItemBase;
import mod.emt.kami.items.base.ItemBaseEnchanted;
import mod.emt.kami.items.baubles.ItemIchorFocusPouch;
import mod.emt.kami.items.tools.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItemsKAMI {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Kami.MOD_ID);

    //registerItem/registerSimpleBlockItem (rather than register) inject the item's registry id into
    //its properties, which Item dereferences to build the description id.
    public static final DeferredItem<Item> ICHOR = ITEMS.registerItem("ichor", ItemBase::new);
    public static final DeferredItem<Item> ICHORWEAVE_FABRIC = ITEMS.registerItem("ichorweave_fabric", ItemBase::new);
    public static final DeferredItem<Item> ICHORIUM_INGOT = ITEMS.registerItem("ichorium_ingot", ItemBase::new);
    public static final DeferredItem<Item> ICHORIUM_NUGGET = ITEMS.registerItem("ichorium_nugget", ItemBase::new);
    public static final DeferredItem<Item> BLESSED_SILVERWOOD_ROD = ITEMS.registerItem("blessed_silverwood_rod", ItemBaseEnchanted::new);
    public static final DeferredItem<Item> FOCUS_POUCH = ITEMS.registerItem("focus_pouch", ItemIchorFocusPouch::new);

    public static final DeferredItem<Item> ICHORWEAVE_HOOD = ITEMS.registerItem("ichorweave_hood",
            properties -> new ItemIchorweaveArmor(EquipmentSlot.HEAD, properties));
    public static final DeferredItem<Item> ICHORWEAVE_ROBE = ITEMS.registerItem("ichorweave_robe",
            properties -> new ItemIchorweaveArmor(EquipmentSlot.CHEST, properties));
    public static final DeferredItem<Item> ICHORWEAVE_LEGGINGS = ITEMS.registerItem("ichorweave_leggings",
            properties -> new ItemIchorweaveArmor(EquipmentSlot.LEGS, properties));
    public static final DeferredItem<Item> ICHORWEAVE_BOOTS = ITEMS.registerItem("ichorweave_boots",
            properties -> new ItemIchorweaveArmor(EquipmentSlot.FEET, properties));

    public static final DeferredItem<Item> AWAKENED_ICHORWEAVE_HOOD = ITEMS.registerItem("awakened_ichorweave_hood",
            properties -> new ItemAwakenedArmor(EquipmentSlot.HEAD, properties));
    public static final DeferredItem<Item> AWAKENED_ICHORWEAVE_ROBE = ITEMS.registerItem("awakened_ichorweave_robe",
            properties -> new ItemAwakenedArmor(EquipmentSlot.CHEST, properties));
    public static final DeferredItem<Item> AWAKENED_ICHORWEAVE_LEGGINGS = ITEMS.registerItem("awakened_ichorweave_leggings",
            properties -> new ItemAwakenedArmor(EquipmentSlot.LEGS, properties));
    public static final DeferredItem<Item> AWAKENED_ICHORWEAVE_BOOTS = ITEMS.registerItem("awakened_ichorweave_boots",
            properties -> new ItemAwakenedArmor(EquipmentSlot.FEET, properties));

    public static final DeferredItem<Item> ICHORIUM_SWORD = ITEMS.registerItem("ichorium_sword", ItemIchoriumSword::new);
    public static final DeferredItem<Item> ICHORIUM_SHOVEL = ITEMS.registerItem("ichorium_shovel", ItemIchoriumShovel::new);
    public static final DeferredItem<Item> ICHORIUM_PICKAXE = ITEMS.registerItem("ichorium_pickaxe", ItemIchoriumPickaxe::new);
    public static final DeferredItem<Item> ICHORIUM_AXE = ITEMS.registerItem("ichorium_axe", ItemIchoriumAxe::new);
    public static final DeferredItem<Item> ICHORIUM_HOE = ITEMS.registerItem("ichorium_hoe", ItemIchoriumHoe::new);

    public static final DeferredItem<Item> AWAKENED_ICHORIUM_SWORD = ITEMS.registerItem("awakened_ichorium_sword", ItemAwakenedSword::new);
    public static final DeferredItem<Item> AWAKENED_ICHORIUM_SHOVEL = ITEMS.registerItem("awakened_ichorium_shovel", ItemAwakenedShovel::new);
    public static final DeferredItem<Item> AWAKENED_ICHORIUM_PICKAXE = ITEMS.registerItem("awakened_ichorium_pickaxe", ItemAwakenedPickaxe::new);
    public static final DeferredItem<Item> AWAKENED_ICHORIUM_AXE = ITEMS.registerItem("awakened_ichorium_axe", ItemAwakenedAxe::new);
    public static final DeferredItem<Item> AWAKENED_ICHORIUM_SICKLE = ITEMS.registerItem("awakened_ichorium_sickle", ItemAwakenedSickle::new);

    public static final DeferredItem<Item> ICHORIUM_CASTER = ITEMS.registerItem("ichorium_caster", ItemIchoriumCaster::new);

    public static final DeferredItem<BlockItem> ICHOR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("ichor_block", ModBlocksKAMI.ICHOR_BLOCK);
    public static final DeferredItem<BlockItem> ICHORIUM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("ichorium_block", ModBlocksKAMI.ICHORIUM_BLOCK);

    private ModItemsKAMI() {}
}
