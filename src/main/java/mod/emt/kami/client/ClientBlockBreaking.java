package mod.emt.kami.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;

/**
 * The client half of {@link mod.emt.kami.utils.helpers.HarvestHelper#attemptHarvestBlock}: performing
 * the extra block breaks through the local game mode so the client predicts them and notifies the
 * server.
 *
 * <p>This lives in its own client-only class because {@link MultiPlayerGameMode} does not exist on
 * the dedicated server, and {@code HarvestHelper} is loaded on both sides.</p>
 */
public final class ClientBlockBreaking {
    private ClientBlockBreaking() {}

    public static boolean destroyBlock(BlockPos pos) {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        return gameMode != null && gameMode.destroyBlock(pos);
    }
}
