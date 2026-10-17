package mod.emt.kami.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mod.emt.kami.Kami;
import mod.emt.kami.entities.EntityThrownAxe;
import mod.emt.kami.registry.ModEntitiesKAMI;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Renders the thrown Awakened Ichorium Axe as its item model, oriented along its flight
 * direction (boomerang style) rather than bill-boarded toward the camera.
 */
@EventBusSubscriber(modid = Kami.MOD_ID, value = Dist.CLIENT)
public class RenderThrownAxe extends EntityRenderer<EntityThrownAxe, RenderThrownAxe.AxeRenderState> {
    private final ItemModelResolver itemModelResolver;

    public RenderThrownAxe(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public AxeRenderState createRenderState() {
        return new AxeRenderState();
    }

    @Override
    public void extractRenderState(EntityThrownAxe entity, AxeRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.itemModelResolver.updateForNonLiving(state.item, entity.getAxeItem(),
                ItemDisplayContext.GROUND, entity);
        state.yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
    }

    @Override
    public void submit(AxeRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.yRot + (state.yRot < 0.0F ? -90.0F : 90.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY,
                state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntitiesKAMI.THROWN_AWAKENED_ICHORIUM_AXE.get(), RenderThrownAxe::new);
    }

    public static class AxeRenderState extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float yRot;
        public float xRot;
    }
}
