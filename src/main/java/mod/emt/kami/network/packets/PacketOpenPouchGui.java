package mod.emt.kami.network.packets;

import mod.emt.kami.Kami;
import mod.emt.kami.inventory.containers.ContainerIchorweavePouch;
import mod.emt.kami.registry.ModItemsKAMI;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Serverbound packet sent when the player presses the focus pouch key or right-clicks the
 * Ichorweave Focus Pouch. Opening the container is now handled entirely on the server via
 * {@link net.minecraft.world.entity.player.Player#openMenu} (the old IGuiHandler is obsolete).
 */
public record PacketOpenPouchGui() implements CustomPacketPayload {
    public static final PacketOpenPouchGui INSTANCE = new PacketOpenPouchGui();

    public static final CustomPacketPayload.Type<PacketOpenPouchGui> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Kami.MOD_ID, "open_pouch_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketOpenPouchGui> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<PacketOpenPouchGui> type() {
        return TYPE;
    }

    public static void handle(PacketOpenPouchGui payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            int pouchIndex = findPouch(context.player().getInventory());
            if (pouchIndex < 0) {
                return;
            }
            //The index is sent as extra menu data so the client builds its menu copy around the
            //same stack the server picked.
            context.player().openMenu(new SimpleMenuProvider(
                    (containerId, inventory, player) -> new ContainerIchorweavePouch(containerId, inventory, pouchIndex),
                    Component.translatable("container.kami.ichorweave_pouch")),
                    buf -> buf.writeVarInt(pouchIndex));
        });
    }

    private static int findPouch(Inventory inventory) {
        NonNullList<ItemStack> items = inventory.getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && stack.is(ModItemsKAMI.FOCUS_POUCH.get())) {
                return i;
            }
        }
        return -1;
    }
}
