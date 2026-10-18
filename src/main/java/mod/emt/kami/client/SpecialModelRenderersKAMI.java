package mod.emt.kami.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.emt.kami.Kami;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Custom special item model renderers for KAMI gear.
 * <p>
 * Vanilla's {@code minecraft:shield} and {@code minecraft:trident} special model types hardcode
 * their textures ({@link net.minecraft.client.renderer.Sheets#SHIELD_BASE} and
 * {@link TridentModel#TEXTURE}), so kami variants register their own types that render the vanilla
 * entity meshes ({@link ModelLayers#SHIELD} / {@link ModelLayers#TRIDENT}) with per-item textures:
 * <ul>
 *   <li>{@code kami:ichorium_shield} — {@code texture} is a sprite on the built-in shield pattern
 *       sheet ({@code textures/atlas/shield_patterns.png}, which picks up {@code entity/shield/}
 *       from every namespace), e.g. {@code kami:entity/shield/ichorium_shield}.</li>
 *   <li>{@code kami:ichorium_trident} — {@code texture} is a raw entity texture id without the
 *       {@code textures/} prefix and {@code .png} suffix, e.g.
 *       {@code kami:entity/trident/ichorium_trident}.</li>
 * </ul>
 * The wrappers' {@code base} models supply the vanilla display transforms (see
 * {@code Kami:item/ichorium_shield_held} / {@code _blocking} and
 * {@code Kami:item/ichorium_trident_in_hand} / {@code _throwing}).
 */
@EventBusSubscriber(modid = Kami.MOD_ID, value = Dist.CLIENT)
public final class SpecialModelRenderersKAMI {
    private SpecialModelRenderersKAMI() {}

    @SubscribeEvent
    public static void onRegisterSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(Kami.MOD_ID, "ichorium_shield"), UnbakedKamiShield.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(Kami.MOD_ID, "ichorium_trident"), UnbakedKamiTrident.MAP_CODEC);
    }

    /** {@code {"type": "kami:ichorium_shield", "texture": "kami:entity/shield/<name>"}}. */
    public record UnbakedKamiShield(Identifier texture) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<UnbakedKamiShield> MAP_CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                                Identifier.CODEC.fieldOf("texture").forGetter(UnbakedKamiShield::texture))
                        .apply(instance, UnbakedKamiShield::new));

        @Override
        public NoDataSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            ShieldModel model = new ShieldModel(context.entityModelSet().bakeLayer(ModelLayers.SHIELD));
            SpriteId sprite = new SpriteId(net.minecraft.client.renderer.Sheets.SHIELD_SHEET, this.texture);
            return new RenderedKamiShield(model, context.sprites(), sprite);
        }

        @Override
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }

    private record RenderedKamiShield(ShieldModel model, SpriteGetter sprites, SpriteId sprite)
            implements NoDataSpecialModelRenderer {
        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                           int overlayCoords, boolean hasFoil, int outlineColor) {
            submitNodeCollector.submitModel(this.model, Unit.INSTANCE, poseStack, lightCoords, overlayCoords,
                    -1, this.sprite, this.sprites, outlineColor, null);
            if (hasFoil) {
                submitNodeCollector.submitModel(this.model, Unit.INSTANCE, poseStack, RenderTypes.entityGlint(),
                        lightCoords, overlayCoords, -1, this.sprites.get(this.sprite), 0, null);
            }
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            this.model.root().getExtentsForGui(new PoseStack(), output);
        }
    }

    /** {@code {"type": "kami:ichorium_trident", "texture": "kami:entity/trident/<name>"}}. */
    public record UnbakedKamiTrident(Identifier texture) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<UnbakedKamiTrident> MAP_CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                                Identifier.CODEC.fieldOf("texture").forGetter(UnbakedKamiTrident::texture))
                        .apply(instance, UnbakedKamiTrident::new));

        @Override
        public NoDataSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            TridentModel model = new TridentModel(context.entityModelSet().bakeLayer(ModelLayers.TRIDENT));
            return new RenderedKamiTrident(model, this.texture);
        }

        @Override
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }

    private record RenderedKamiTrident(TridentModel model, Identifier texture)
            implements NoDataSpecialModelRenderer {
        private Identifier fullTexture() {
            return this.texture.withPrefix("textures/").withSuffix(".png");
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                           int overlayCoords, boolean hasFoil, int outlineColor) {
            submitNodeCollector.submitModelPart(this.model.root(), poseStack,
                    this.model.renderType(this.fullTexture()), lightCoords, overlayCoords, null, false, hasFoil,
                    -1, null, outlineColor);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            this.model.root().getExtentsForGui(new PoseStack(), output);
        }
    }
}
