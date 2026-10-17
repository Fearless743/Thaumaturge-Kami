package mod.emt.kami.api.item;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IAreaBreakTool {
    ImmutableList<BlockPos> getBreakAreaPositions(Player player, ItemStack stack, BlockPos origin, boolean includeOrigin);

    boolean spawnDropsAtPlayer(Player player, ItemStack stack);
}
