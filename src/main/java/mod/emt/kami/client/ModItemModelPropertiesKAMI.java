package mod.emt.kami.client;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.emt.kami.Kami;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import org.jspecify.annotations.Nullable;

/**
 * Client item model property for every {@code *_mode} data component: the awakened tools use
 * {@code minecraft:range_dispatch} definitions (the modern replacement for the legacy
 * {@code overrides}/{@code predicate} item model syntax, which this Minecraft version's parsers
 * ignore) to switch their texture based on the selected mode index.
 */
@EventBusSubscriber(modid = Kami.MOD_ID, value = Dist.CLIENT)
public final class ModItemModelPropertiesKAMI {
    private ModItemModelPropertiesKAMI() {}

    @SubscribeEvent
    public static void onRegisterRangeSelectProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        // A single property id suffices: the JSON's "component" field selects which *_mode
        // data component is read (mirroring how the vanilla compass property takes a "target").
        // The underlying id mapper is a bimap, so re-registering the same codec under a second
        // id crashes with "value already present".
        event.register(Identifier.fromNamespaceAndPath(Kami.MOD_ID, "mode_component"), ComponentMode.MAP_CODEC);
    }

    /**
     * A numeric range-select property that returns the integer {@link #componentId} data
     * component of the stack (e.g. {@code kami:slayer_mode}), usable by
     * {@code minecraft:range_dispatch} item model definitions.
     */
    public record ComponentMode(Identifier componentId) implements RangeSelectItemModelProperty {
        public static final MapCodec<ComponentMode> MAP_CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                                Identifier.CODEC.fieldOf("component").forGetter(ComponentMode::componentId))
                        .apply(instance, ComponentMode::new));

        @Override
        public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
            DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(this.componentId);
            if (type != null) {
                Object value = stack.get(type);
                if (value instanceof Integer integer) {
                    return (float) integer;
                }
            }
            return 0.0F;
        }

        @Override
        public MapCodec<? extends RangeSelectItemModelProperty> type() {
            return MAP_CODEC;
        }
    }
}
