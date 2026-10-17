package mod.emt.kami.network;

import mod.emt.kami.Kami;
import mod.emt.kami.network.packets.PacketOpenPouchGui;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Central networking registration for KAMI.
 * <p>
 * The menu type itself is registered through {@link mod.emt.kami.registry.ModMenusKAMI#MENUS}
 * (the integrator wires {@code ModMenusKAMI.register(modEventBus)} in the mod constructor).
 */
@EventBusSubscriber(modid = Kami.MOD_ID)
public final class PacketHandler {
    /** NeoForge network version for this mod's payloads. */
    public static final String VERSION = "1";

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(PacketOpenPouchGui.TYPE, PacketOpenPouchGui.STREAM_CODEC, PacketOpenPouchGui::handle);
    }
}
