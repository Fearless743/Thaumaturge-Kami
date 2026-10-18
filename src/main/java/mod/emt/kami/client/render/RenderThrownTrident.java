package mod.emt.kami.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mod.emt.kami.Kami;
import mod.emt.kami.entities.EntityThrownTrident;
import mod.emt.kami.items.tools.ItemIchoriumTrident;
import mod.emt.kami.registry.ModDataComponentsKAMI;
import mod.emt.kami.registry.ModEntitiesKAMI;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * Renders the thrown Ichorium Trident with the vanilla trident entity mesh, oriented along its
 * flight direction (boomerang style) rather than bill-boarded toward the camera. The texture
 * follows the thrown stack: ichorium gold for the base trident, mode colors for the awakened one.
 */
@EventBusSubscriber(modid = Kami.MOD_ID, value = Dist.CLIENT)
public class RenderThrownTrident extends EntityRenderer<EntityThrownTrident, RenderThrownTrident.TridentRenderState> {
    private static final Identifier TEXTURE_GOLD =
            Identifier.fromNamespaceAndPath(Kami.MOD_ID, "textures/entity/trident/ichorium_trident.png");
    private static final Identifier TEXTURE_INACTIVE =
            Identifier.fromNamespaceAndPath(Kami.MOD_ID, "textures/entity/trident/awakened_ichorium_trident.png");
    private static final Identifier TEXTURE_YELLOW =
            Identifier.fromNamespaceAndPath(Kami.MOD_ID, "textures/entity/trident/awakened_ichorium_trident_yellow.png");
    private static final Identifier TEXTURE_AQUA =
            Identifier.fromNamespaceAndPath(Kami.MOD_ID, "textures/entity/trident/awakened_ichorium_trident_aqua.png");

    private final TridentModel model;

    public RenderThrownTrident(EntityRendererProvider.Context context) {
        super(context);
        this.model = new TridentModel(context.bakeLayer(ModelLayers.TRIDENT));
    }

    @Override
    public TridentRenderState createRenderState() {
        return new TridentRenderState();
    }

    @Override
    public void extractRenderState(EntityThrownTrident entity, TridentRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ItemStack trident = entity.getTridentItem();
        state.texture = textureFor(trident);
        state.isFoil = trident.hasFoil();
        state.yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
    }

    @Override
    public void submit(TridentRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.yRot + (state.yRot < 0.0F ? -90.0F : 90.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
        submitNodeCollector.order(0).submitModel(this.model, Unit.INSTANCE, poseStack, state.texture,
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        if (state.isFoil) {
            submitNodeCollector.order(1).submitModel(this.model, Unit.INSTANCE, poseStack,
                    ItemFeatureRenderer.getFoilRenderType(this.model.renderType(state.texture), false),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        }
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static Identifier textureFor(ItemStack trident) {
        if (trident.getItem() instanceof ItemIchoriumTrident) {
            return TEXTURE_GOLD;
        }
        int mode = trident.getOrDefault(ModDataComponentsKAMI.STORM_MODE.get(), 0);
        return switch (mode) {
            case 1 -> TEXTURE_YELLOW;
            case 2 -> TEXTURE_AQUA;
            default -> TEXTURE_INACTIVE;
        };
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntitiesKAMI.THROWN_ICHORIUM_TRIDENT.get(), RenderThrownTrident::new);
    }

    public static class TridentRenderState extends EntityRenderState {
        public Identifier texture = TEXTURE_INACTIVE;
        public boolean isFoil;
        public float yRot;
        public float xRot;
    }
}
