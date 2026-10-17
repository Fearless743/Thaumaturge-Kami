package mod.emt.kami.inventory.slots;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A slot that can neither be picked up nor accept an item. Used to lock the player's currently
 * selected hotbar slot so the item being held (typically the pouch) cannot be moved while the
 * pouch menu is open.
 */
public class SlotLocked extends Slot {
    public SlotLocked(Container container, int index, int xPosition, int yPosition) {
        super(container, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return false;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }
}
