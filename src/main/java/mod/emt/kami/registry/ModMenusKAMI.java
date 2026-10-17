package mod.emt.kami.registry;

import mod.emt.kami.Kami;
import mod.emt.kami.inventory.containers.ContainerIchorweavePouch;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Menu (container) type registrations for KAMI.
 * <p>
 * The integrator must call {@link #register(IEventBus)} from the mod constructor, e.g.
 * {@code ModMenusKAMI.register(modEventBus);}.
 */
public final class ModMenusKAMI {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Kami.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ContainerIchorweavePouch>> ICHORWEAVE_POUCH =
            MENUS.register("ichorweave_pouch",
                    () -> new MenuType<>(ContainerIchorweavePouch::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    private ModMenusKAMI() {}
}
