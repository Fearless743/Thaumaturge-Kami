package mod.emt.kami.registry;

import mod.emt.kami.Kami;
import mod.emt.kami.blocks.BlockMaterial;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocksKAMI {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Kami.MOD_ID);

    //registerBlock (rather than register) injects the block's registry id into the properties,
    //which BlockBehaviour dereferences when it builds its drop table.
    public static final DeferredBlock<BlockMaterial> ICHOR_BLOCK = BLOCKS.registerBlock("ichor_block",
            BlockMaterial::new, properties -> properties.mapColor(MapColor.GOLD).strength(5.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<BlockMaterial> ICHORIUM_BLOCK = BLOCKS.registerBlock("ichorium_block",
            BlockMaterial::new, properties -> properties.mapColor(MapColor.GOLD).strength(5.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    private ModBlocksKAMI() {}
}
