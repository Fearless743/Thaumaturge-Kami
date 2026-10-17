package mod.emt.kami.utils.helpers;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.UsernameCache;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerHelper {

    public static double getReachDistance(Player player) {
        return player.blockInteractionRange();
    }

    @Nullable
    public static HitResult rayTrace(LivingEntity entity, float partialTicks) {
        double reach = entity instanceof Player player ? getReachDistance(player) : 5.0D;
        return rayTrace(entity, reach, partialTicks);
    }

    @Nullable
    public static HitResult rayTrace(LivingEntity entity, double blockReachDistance, float partialTicks) {
        return rayTrace(entity, blockReachDistance, partialTicks, false);
    }

    @Nullable
    public static HitResult rayTrace(LivingEntity entity, double blockReachDistance, float partialTicks, boolean stopOnLiquid) {
        return entity.pick(blockReachDistance, partialTicks, stopOnLiquid);
    }

    @Nullable
    public static ServerPlayer getPlayerFromUUID(UUID uuid) {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getPlayerList().getPlayer(uuid);
    }

    public static UUID getUUIDFromPlayer(Player player) {
        return player.getUUID();
    }

    public static String getUsernameFromUUID(UUID uuid) {
        return UsernameCache.getLastKnownUsername(uuid);
    }

    public static Direction getPlayerFacing(Player player) {
        Vec3 lookVec = player.getLookAngle();
        double absX = Math.abs(lookVec.x);
        double absY = Math.abs(lookVec.y);
        double absZ = Math.abs(lookVec.z);
        if (absY > absX && absY > absZ) {
            return lookVec.y < 0 ? Direction.DOWN : Direction.UP;
        } else if (absX > absZ) {
            return lookVec.x < 0 ? Direction.WEST : Direction.EAST;
        } else {
            return lookVec.z < 0 ? Direction.NORTH : Direction.SOUTH;
        }
    }
}
