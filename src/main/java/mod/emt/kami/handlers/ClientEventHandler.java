package mod.emt.kami.handlers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.emt.kami.Kami;
import mod.emt.kami.api.item.IAreaBreakTool;
import mod.emt.kami.client.KeyBindingsKami;
import mod.emt.kami.network.packets.PacketOpenPouchGui;
import mod.emt.kami.registry.ModItemsKAMI;
import mod.emt.kami.utils.helpers.PlayerHelper;
import mod.emt.kami.utils.helpers.TreeHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = Kami.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {
    public static final Identifier WIDGETS =
            Identifier.fromNamespaceAndPath(Kami.MOD_ID, "textures/gui/widgets.png");

    /**
     * Draws the extra (area-of-effect) block outlines while the player holds an area-break tool.
     *
     * // TODO(port): the legacy implementation also drew the block-destruction overlay texture on
     * // the extra blocks. 26.1 tracks destruction progress server-side only, so that part of the
     * // overlay was dropped; the selection outlines below are preserved.
     */
    @SubscribeEvent
    public static void renderExtraBlockBreak(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.gameRenderer == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IAreaBreakTool aoeTool)) {
            return;
        }

        HitResult trace = PlayerHelper.rayTrace(player, 1.0F);
        if (!(trace instanceof BlockHitResult blockHit)) {
            return;
        }

        List<BlockPos> extraBlocks = aoeTool.getBreakAreaPositions(player, stack, blockHit.getBlockPos(), false);
        if (extraBlocks.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        if (poseStack == null) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(RenderTypes.lines());
        for (BlockPos pos : extraBlocks) {
            BlockState state = player.level().getBlockState(pos);
            VoxelShape shape = state.getShape(player.level(), pos);
            ShapeRenderer.renderShape(poseStack, buffer, shape,
                    pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z,
                    0xFFFFFFFF, 2.0F);
        }
        buffers.endBatch(RenderTypes.lines());
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && KeyBindingsKami.openFocusPouch.consumeClick()) {
            if (KeyBindingsKami.isPouchEquipped(player)) {
                ClientPacketDistributor.sendToServer(PacketOpenPouchGui.INSTANCE);
            }
        }
    }

    @SubscribeEvent
    public static void onDrawScreenPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack mhStack = player.getMainHandItem();
        if (mhStack.is(ModItemsKAMI.AWAKENED_ICHORIUM_AXE.get())) {
            HitResult trace = PlayerHelper.rayTrace(player, 1.0F);
            if (trace instanceof BlockHitResult blockHit
                    && TreeHelper.isTreeStructure(player.level(), blockHit.getBlockPos())) {
                renderTimberOverlay(event.getGuiGraphics(), mc);
            }
        }
    }

    private static void renderTimberOverlay(GuiGraphicsExtractor graphics, Minecraft mc) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int texX = mc.player != null && mc.player.isShiftKeyDown() ? 16 : 0;
        int xc = screenWidth / 2 + 8;
        int yc = screenHeight / 2 - 8;
        graphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, xc, yc, (float) texX, 0.0F, 16, 16, 32, 16);
    }
}
