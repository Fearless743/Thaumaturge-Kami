package mod.emt.kami.inventory.containers;

import mod.emt.kami.inventory.handlers.PouchStackHandler;
import mod.emt.kami.inventory.slots.SlotLocked;
import mod.emt.kami.registry.ModItemsKAMI;
import mod.emt.kami.registry.ModMenusKAMI;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * The Ichorweave Focus Pouch menu.
 * <p>
 * Ported from the 1.12.2 {@code Container}. Legacy baubles/Curios slot APIs no longer exist in this
 * stack, so the pouch is located inside the player's main inventory instead of an accessory slot
 * (see {@link mod.emt.kami.client.KeyBindingsKami} for the matching use check).
 */
public class ContainerIchorweavePouch extends AbstractContainerMenu {
    /** The pouch inventory occupies slots [0, POUCH_SLOTS), the player inventory follows. */
    public static final int POUCH_SLOTS = PouchStackHandler.SLOTS;

    private final ItemStack pouchStack;
    private final PouchStackHandler pouchHandler;

    /**
     * Factory constructor used by {@link ModMenusKAMI#ICHORWEAVE_POUCH} (client and server).
     *
     * @param pouchIndex index of the pouch inside the player's non-equipment items, written as
     *                   extra menu data by whoever opened the menu ({@code -1} when unspecified)
     */
    public ContainerIchorweavePouch(int containerId, Inventory playerInventory, int pouchIndex) {
        super(ModMenusKAMI.ICHORWEAVE_POUCH.get(), containerId);
        this.pouchStack = findPouch(playerInventory, pouchIndex);
        this.pouchHandler = new PouchStackHandler(this.pouchStack);
        this.bindPouchInventory();
        this.bindPlayerInventory(playerInventory);
    }

    /**
     * Resolves the stack this menu edits: {@code pouchIndex} pins the exact pouch the player
     * opened (the held one when right-clicking), so a second pouch carried elsewhere can no longer
     * hijack the menu. Falls back to the first pouch found when the index is unset or stale.
     */
    private static ItemStack findPouch(Inventory playerInventory, int pouchIndex) {
        NonNullList<ItemStack> items = playerInventory.getNonEquipmentItems();
        if (pouchIndex >= 0 && pouchIndex < items.size()) {
            ItemStack indexed = items.get(pouchIndex);
            if (!indexed.isEmpty() && indexed.is(ModItemsKAMI.FOCUS_POUCH.get())) {
                return indexed;
            }
        }
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.is(ModItemsKAMI.FOCUS_POUCH.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getPouchStack() {
        return this.pouchStack;
    }

    private void bindPouchInventory() {
        for (int slot = 0; slot < this.pouchHandler.getSlots(); slot++) {
            int xIndex = slot % 13;
            int yIndex = slot / 13;
            this.addSlot(new SlotItemHandler(this.pouchHandler, slot, xIndex * 18 + 12, yIndex * 18 + 8));
        }
    }

    private void bindPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 48 + col * 18, 177 + row * 18));
            }
        }

        int selected = playerInventory.getSelectedSlot();
        for (int col = 0; col < 9; col++) {
            if (col == selected) {
                this.addSlot(new SlotLocked(playerInventory, col, 48 + col * 18, 235));
            } else {
                this.addSlot(new Slot(playerInventory, col, 48 + col * 18, 235));
            }
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < POUCH_SLOTS) {
                if (!this.moveItemStackTo(slotStack, POUCH_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, POUCH_SLOTS, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        // The pouch is the player's own item; the menu stays valid as long as the player is alive.
        return !this.pouchStack.isEmpty() && player.isAlive();
    }
}
