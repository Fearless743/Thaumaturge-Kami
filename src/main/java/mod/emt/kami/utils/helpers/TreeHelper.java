package mod.emt.kami.utils.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class TreeHelper {
    public static boolean fellTree(ItemStack itemstack, BlockPos start, Player player) {
        if (!player.level().isClientSide()) {
            NeoForge.EVENT_BUS.register(new TreeChopTask(itemstack, start, player, 1));
        }
        return true;
    }

    public static boolean isTreeStructure(Level level, BlockPos origin) {
        for (int y = 0; y < 32; y++) {
            BlockPos checkPos = origin.above(y);
            if (isLog(level, checkPos)) {
                for (BlockPos adjacent : getAdjacentPositions(checkPos)) {
                    if (isLeaves(level, adjacent)) {
                        return true;
                    }
                }
            } else {
                return false;
            }
        }
        return false;
    }

    private static boolean isLog(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.LOGS);
    }

    private static boolean isLeaves(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.LEAVES);
    }

    public static List<BlockPos> getAdjacentPositions(BlockPos pos) {
        List<BlockPos> adjacent = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    adjacent.add(pos.offset(x, y, z));
                }
            }
        }
        return adjacent;
    }

    public static class TreeChopTask {
        public final Level level;
        public final Player player;
        public final ItemStack tool;
        public final int blocksPerTick;
        public Queue<BlockPos> blocks = new LinkedList<>();
        public Set<BlockPos> visited = new HashSet<>();

        public TreeChopTask(ItemStack tool, BlockPos start, Player player, int blocksPerTick) {
            this.level = player.level();
            this.player = player;
            this.tool = tool;
            this.blocksPerTick = blocksPerTick;
            this.blocks.add(start);
        }

        @SubscribeEvent
        public void fellTree(LevelTickEvent.Post event) {
            if (event.getLevel().isClientSide()) {
                this.finish();
            } else if (event.getLevel().dimension() == this.level.dimension()) {
                int left = this.blocksPerTick;

                while (left > 0) {
                    if (this.blocks.isEmpty()) {
                        this.finish();
                        return;
                    }

                    BlockPos pos = this.blocks.remove();
                    if (this.visited.add(pos) && isLog(this.level, pos)
                            && HarvestHelper.canHarvestBlock(this.level, pos, this.level.getBlockState(pos), this.player, true)) {
                        for (BlockPos adjacent : getAdjacentPositions(pos)) {
                            if (!this.visited.contains(adjacent)) {
                                this.blocks.add(adjacent);
                            }
                        }

                        //Adding up to a 3x3 surrounding the harvested block
                        for (int x = 0; x < 3; ++x) {
                            for (int z = 0; z < 3; ++z) {
                                BlockPos pos2 = pos.offset(-1 + x, 1, -1 + z);
                                if (!this.visited.contains(pos2)) {
                                    this.blocks.add(pos2);
                                }
                            }
                        }

                        HarvestHelper.attemptHarvestBlock(this.level, this.player, pos);
                        left--;
                    }
                }
            }
        }

        private void finish() {
            NeoForge.EVENT_BUS.unregister(this);
        }
    }
}
