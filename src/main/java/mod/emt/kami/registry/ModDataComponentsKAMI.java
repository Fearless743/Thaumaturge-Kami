package mod.emt.kami.registry;

import com.mojang.serialization.Codec;
import mod.emt.kami.Kami;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom {@link DataComponentType}s used by the KAMI gear. Each awakened tool stores its
 * currently selected operating mode as a small integer component (the modern replacement for
 * the old {@code NBTTagCompound} int keys).
 *
 * <p>The {@link #DATA_COMPONENTS} register is intentionally <em>not</em> wired to the mod event
 * bus here; the integration layer registers it. All item code resolves components lazily via
 * {@link DeferredHolder#get()} at use time, so referencing this class during item construction is
 * safe as long as the register is eventually attached.</p>
 */
public final class ModDataComponentsKAMI {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Kami.MOD_ID);

    /** Selected slayer target of the Awakened Ichorium Sword. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SLAYER_MODE =
            DATA_COMPONENTS.registerComponentType("slayer_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Selected impact mode of the thrown Awakened Ichorium Axe. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> IMPACT_MODE =
            DATA_COMPONENTS.registerComponentType("impact_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Selected area-of-effect size of the Awakened Ichorium Pickaxe. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AOE_MODE =
            DATA_COMPONENTS.registerComponentType("aoe_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Selected bury target of the Awakened Ichorium Shovel. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BURY_MODE =
            DATA_COMPONENTS.registerComponentType("bury_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Selected harvest mode of the Awakened Ichorium Sickle. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HARVEST_MODE =
            DATA_COMPONENTS.registerComponentType("harvest_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /**Selected ward mode of the Awakened Ichorium Shield.*/
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WARD_MODE =
            DATA_COMPONENTS.registerComponentType("ward_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /**Selected storm mode of the Awakened Ichorium Trident.*/
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORM_MODE =
            DATA_COMPONENTS.registerComponentType("storm_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /**Selected lance mode of the Awakened Ichorium Spear.*/
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LANCE_MODE =
            DATA_COMPONENTS.registerComponentType("lance_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /**Selected quake mode of the Awakened Ichorium Maul.*/
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> QUAKE_MODE =
            DATA_COMPONENTS.registerComponentType("quake_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponentsKAMI() {}
}
