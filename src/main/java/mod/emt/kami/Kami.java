package mod.emt.kami;

import mod.emt.kami.config.ConfigHandlerKami;
import mod.emt.kami.registry.CreativeTabsKAMI;
import mod.emt.kami.registry.ModBlocksKAMI;
import mod.emt.kami.registry.ModDataComponentsKAMI;
import mod.emt.kami.registry.ModEntitiesKAMI;
import mod.emt.kami.registry.ModItemsKAMI;
import mod.emt.kami.registry.ModMenusKAMI;
import mod.emt.kami.registry.ModRecipeTypesKAMI;
import mod.emt.kami.registry.ModSoundsKAMI;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Kami.MOD_ID)
public class Kami {
    public static final String MOD_ID = Tags.MOD_ID;
    public static final String MOD_NAME = Tags.MOD_NAME;
    public static final String MOD_VERSION = Tags.VERSION;
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public Kami(IEventBus modEventBus, Dist dist, ModContainer modContainer) {
        ModSoundsKAMI.SOUNDS.register(modEventBus);
        ModBlocksKAMI.BLOCKS.register(modEventBus);
        ModItemsKAMI.ITEMS.register(modEventBus);
        CreativeTabsKAMI.TABS.register(modEventBus);
        ModEntitiesKAMI.ENTITY_TYPES.register(modEventBus);
        ModRecipeTypesKAMI.RECIPE_SERIALIZERS.register(modEventBus);
        ModDataComponentsKAMI.DATA_COMPONENTS.register(modEventBus);
        ModMenusKAMI.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigHandlerKami.SPEC);

        LOGGER.info("{} {} initialized", MOD_NAME, MOD_VERSION);
    }
}
