package mod.emt.kami.items.baubles;

import mod.emt.kami.inventory.containers.ContainerIchorweavePouch;
import mod.emt.kami.inventory.handlers.PouchStackHandler;
import mod.emt.kami.items.base.ItemBase;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Ichorweave Focus Pouch stores up to {@link PouchStackHandler#SLOTS} focus items.
 *
 * <p>Thaumaturge's {@code FocusPouchItem} is final and owns its own (much smaller) menu, so this
 * pouch cannot extend it. Instead it is a plain item that opens KAMI's own
 * {@link ContainerIchorweavePouch} - mirroring {@code PacketOpenPouchGui}, which the focus-pouch
 * key uses. The 1.12 Baubles {@code onEquipped}/{@code onUnequipped} sounds have no hook now that
 * the pouch is carried in the inventory rather than an accessory slot.</p>
 *
 * <p>The pouch must never stack ({@code stacksTo(1)}): its contents live in the stack's
 * {@code minecraft:container} component, which is shared by the whole pile - a merged stack would
 * expose one inventory for every pouch in it, and splitting the pile would duplicate the contents.</p>
 */
public class ItemIchorFocusPouch extends ItemBase {
    public ItemIchorFocusPouch(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
            //Right-clicking must open the pouch in hand, not the first pouch in the inventory, so
            //the held stack's index travels with the menu as extra client-side data.
            int pouchIndex = player.getInventory().getSelectedSlot();
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new ContainerIchorweavePouch(containerId, inventory, pouchIndex),
                    Component.translatable("container.kami.ichorweave_pouch")),
                    buf -> buf.writeVarInt(pouchIndex));
        }
        return InteractionResult.SUCCESS;
    }

    /** Reads the pouch contents, exposed for parity with the legacy {@code ItemFocusPouch} API. */
    public static NonNullList<ItemStack> getInventory(ItemStack stack) {
        return new PouchStackHandler(stack).getInventory();
    }

    /** Writes the pouch contents, exposed for parity with the legacy {@code ItemFocusPouch} API. */
    public static void setInventory(ItemStack stack, NonNullList<ItemStack> contents) {
        new PouchStackHandler(stack).setInventory(contents);
    }
}
