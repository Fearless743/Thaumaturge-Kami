package mod.emt.kami.client.gui;

import mod.emt.kami.Kami;
import mod.emt.kami.inventory.containers.ContainerIchorweavePouch;
import mod.emt.kami.registry.ModMenusKAMI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Kami.MOD_ID, value = Dist.CLIENT)
public class GuiIchorweavePouch extends AbstractContainerScreen<ContainerIchorweavePouch> {
    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Kami.MOD_ID, "textures/gui/ichorweave_pouch.png");

    public GuiIchorweavePouch(ContainerIchorweavePouch menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 256);
    }

    public ContainerIchorweavePouch getContainer() {
        return this.menu;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xStart = (this.width - this.imageWidth) / 2;
        int yStart = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xStart, yStart, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenusKAMI.ICHORWEAVE_POUCH.get(), GuiIchorweavePouch::new);
    }
}
