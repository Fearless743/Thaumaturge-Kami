package mod.emt.kami.registry;

import mod.emt.kami.Kami;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreativeTabsKAMI {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Kami.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KAMI_TAB = TABS.register("kami", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.kami"))
            .icon(() -> new ItemStack(ModItemsKAMI.ICHOR.get()))
            .displayItems((parameters, output) -> ModItemsKAMI.ITEMS.getEntries().forEach(holder -> output.accept(holder.get())))
            .build());

    private CreativeTabsKAMI() {}
}
