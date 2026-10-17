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
 */
public class ItemIchorFocusPouch extends ItemBase {
    public ItemIchorFocusPouch(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new ContainerIchorweavePouch(containerId, inventory),
                    Component.translatable("container.kami.ichorweave_pouch")));
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
