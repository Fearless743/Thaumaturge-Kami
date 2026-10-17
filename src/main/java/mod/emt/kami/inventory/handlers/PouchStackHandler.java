package mod.emt.kami.inventory.handlers;

import com.leclowndu93150.thaumaturge.content.casters.FocusPouchItem;
import mod.emt.kami.registry.ModItemsKAMI;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Item handler backing the Ichorweave Focus Pouch.
 * <p>
 * The 1.12.2 version persisted its 117 slots into the item's {@code NBTTagCompound} through an
 * {@code ICapabilitySerializable}. Modern Minecraft has no item NBT, so the inventory is stored in
 * the stack's {@link DataComponents#CONTAINER} ({@link ItemContainerContents}) component instead:
 * it is loaded once in the constructor and written back whenever the contents change. This is the
 * same storage vanilla uses for shulker boxes, so the pouch is tooltip- and command-compatible.
 */
public class PouchStackHandler extends ItemStackHandler {
    /** 13 columns x 9 rows, matching the legacy layout. */
    public static final int SLOTS = 117;

    private final ItemStack pouchStack;

    public PouchStackHandler(ItemStack pouchStack) {
        super(SLOTS);
        this.pouchStack = pouchStack;
        ItemContainerContents contents = pouchStack.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(this.stacks);
        }
    }

    public NonNullList<ItemStack> getInventory() {
        return this.stacks;
    }

    public void setInventory(NonNullList<ItemStack> stackList) {
        int slots = Math.min(this.getSlots(), stackList.size());
        for (int i = 0; i < slots; i++) {
            this.stacks.set(i, stackList.get(i));
        }
        this.onContentsChanged(0);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        // Pouches may not be nested inside each other.
        return !(stack.getItem() instanceof FocusPouchItem) && !stack.is(ModItemsKAMI.FOCUS_POUCH.get());
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (this.pouchStack.isEmpty()) {
            return;
        }
        this.pouchStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.stacks));
    }
}
