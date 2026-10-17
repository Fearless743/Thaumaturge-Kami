package mod.emt.kami.utils.helpers;

import com.google.common.collect.ImmutableList;
import mod.emt.kami.client.ClientBlockBreaking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class HarvestHelper {

    public static ImmutableList<BlockPos> getHarvestArea(Level level, Player player, @NotNull BlockHitResult trace, int diameter, int depth, boolean includeOrigin, boolean strictHarvestCheck) {
        Direction side = trace.getDirection();
        BlockPos startPos = trace.getBlockPos();
        BlockState state = level.getBlockState(startPos);

        Vec3 hitVec = trace.getLocation();
        if (diameter % 2 == 0) {
            float hx = (float) hitVec.x - startPos.getX();
            float hy = (float) hitVec.y - startPos.getY();
            float hz = (float) hitVec.z - startPos.getZ();
            if (side.getAxis() == Direction.Axis.Y && (double) hx < 0.5D || side.getAxis() == Direction.Axis.Z && (double) hx < 0.5D) {
                startPos = startPos.offset(-diameter / 2, 0, 0);
            }

            if (side.getAxis() != Direction.Axis.Y && (double) hy < 0.5D) {
                startPos = startPos.offset(0, -diameter / 2, 0);
            }

            if (side.getAxis() == Direction.Axis.Y && (double) hz < 0.5D || side.getAxis() == Direction.Axis.Z && (double) hz < 0.5D) {
                startPos = startPos.offset(0, 0, -diameter / 2);
            }
        } else {
            startPos = startPos.offset(
                    -(side.getAxis() == Direction.Axis.X ? (side == Direction.EAST ? depth - 1 : 0) : diameter / 2),
                    -(side.getAxis() == Direction.Axis.Y ? (side == Direction.UP ? depth - 1 : 0) : (diameter <= 1 ? 0 : 1)),
                    -(side.getAxis() == Direction.Axis.Z ? (side == Direction.SOUTH ? depth - 1 : 0) : diameter / 2));
        }

        ImmutableList.Builder<BlockPos> posBuilder = ImmutableList.builder();
        for (int dd = 0; dd < depth; dd++) {
            for (int dw = 0; dw < diameter; dw++) {
                for (int dh = 0; dh < diameter; dh++) {
                    BlockPos pos = startPos.offset(
                            side.getAxis() == Direction.Axis.X ? dd : dw,
                            side.getAxis() == Direction.Axis.Y ? dd : dh,
                            side.getAxis() == Direction.Axis.Z ? dd : (side.getAxis() == Direction.Axis.Y ? dh : dw)
                    );
                    if (!pos.equals(trace.getBlockPos()) || includeOrigin) {
                        state = level.getBlockState(pos);
                        if (canHarvestBlock(level, pos, state, player, strictHarvestCheck)) {
                            posBuilder.add(pos);
                        }
                    }
                }
            }
        }

        return posBuilder.build();
    }

    public static boolean canHarvestBlock(Level level, BlockPos pos, BlockState state, Player player, boolean strict) {
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        // The strict/non-strict distinction collapsed onto the vanilla tool check: a block may be
        // broken by this tool when the player has the correct tool for its drops.
        return player.hasCorrectToolForDrops(state);
    }

    public static void harvestExtraBlocks(Player player, ItemStack stack, ImmutableList<BlockPos> harvestPositions) {
        int damage = 0;
        for (BlockPos pos : harvestPositions) {
            if (attemptHarvestBlock(player.level(), player, pos)) {
                damage++;
            }
        }
        if (damage > 0 && !player.isCreative()) {
            stack.hurtAndBreak(damage, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
    }

    public static boolean attemptHarvestBlock(Level level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F || !player.hasCorrectToolForDrops(state)) {
            return false;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            // Fires BlockEvent.BreakEvent, handles drops, XP and the block-change packet.
            return serverPlayer.gameMode.destroyBlock(pos);
        } else if (level.isClientSide()) {
            return ClientBlockBreaking.destroyBlock(pos);
        }
        return false;
    }
}
