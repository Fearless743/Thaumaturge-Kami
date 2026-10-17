package mod.emt.kami.client;

import com.mojang.blaze3d.platform.InputConstants;
import mod.emt.kami.Kami;
import mod.emt.kami.registry.ModItemsKAMI;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side key bindings. The focus pouch key is only active while the player is carrying an
 * Ichorweave Focus Pouch.
 * <p>
 * The 1.12.2 pouch lived in a Baubles slot; neither Baubles nor Curios exist on this platform, so
 * the pouch is instead carried in the player's main inventory (hotbar included) and this check
 * scans that inventory rather than an accessory slot.
 */
@EventBusSubscriber(modid = Kami.MOD_ID, value = Dist.CLIENT)
public class KeyBindingsKami {
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(Kami.MOD_ID, "main"));

    /** Only active while the player actually carries a Focus Pouch. */
    private static final IKeyConflictContext POUCH_CONTEXT = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            Player player = Minecraft.getInstance().player;
            return player != null && isPouchEquipped(player);
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return this == other;
        }
    };

    public static final KeyMapping openFocusPouch = new KeyMapping(
            "key.kami.open_focus_pouch",
            POUCH_CONTEXT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY);

    /** Returns true when the player carries an Ichorweave Focus Pouch anywhere in their inventory. */
    public static boolean isPouchEquipped(Player player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && stack.is(ModItemsKAMI.FOCUS_POUCH.get())) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(openFocusPouch);
    }
}
