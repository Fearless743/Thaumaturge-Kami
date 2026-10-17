package mod.emt.kami.client;

import mod.emt.kami.Kami;
import mod.emt.kami.items.armor.ItemAwakenedArmor;
import mod.emt.kami.items.baubles.ItemIchorFocusPouch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Tooltip lines that can only be produced on the client: the Awakened set's "pieces equipped"
 * counter (needs the viewing player) and the focus pouch's keybind hint (needs the client
 * keybindings). Both are added here rather than in the items' {@code appendHoverText}, which runs on
 * the dedicated server too and must not reference client-only types.
 */
@EventBusSubscriber(modid = Kami.MOD_ID, value = Dist.CLIENT)
public final class ClientItemTooltips {
    private ClientItemTooltips() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof ItemIchorFocusPouch) {
            event.getToolTip().add(Component.translatable("tooltip.kami.focus_pouch.keybind",
                    KeyBindingsKami.openFocusPouch.getTranslatedKeyMessage()));
            return;
        }
        if (stack.getItem() instanceof ItemAwakenedArmor) {
            Player player = event.getEntity();
            if (player == null) {
                return;
            }
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.kami.awakened_set.info",
                    ItemAwakenedArmor.getEquippedPieces(player)));
        }
    }
}
