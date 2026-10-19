package mod.emt.kami.registry;

import mod.emt.kami.Kami;
import mod.emt.kami.inventory.containers.ContainerIchorweavePouch;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
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

    /**
     * The factory reads the pouch's inventory index from the menu's extra data (see
     * {@link ContainerIchorweavePouch}); {@code null} data (menu opened without a writer) falls
     * back to the first pouch found.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerIchorweavePouch>> ICHORWEAVE_POUCH =
            MENUS.register("ichorweave_pouch", () -> IMenuTypeExtension.create(
                    (containerId, inventory, data) -> new ContainerIchorweavePouch(
                            containerId, inventory, data == null ? -1 : data.readVarInt())));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    private ModMenusKAMI() {}
}
