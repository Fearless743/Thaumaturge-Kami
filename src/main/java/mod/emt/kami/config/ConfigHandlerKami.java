package mod.emt.kami.config;

import mod.emt.kami.Kami;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Kami.MOD_ID)
public final class ConfigHandlerKami {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ArmorCategory armor = new ArmorCategory(BUILDER);
    public static final ToolCategory tools = new ToolCategory(BUILDER);
    public static final EnchantmentsCategory enchantments = new EnchantmentsCategory(BUILDER);
    public static final MiscCategory misc = new MiscCategory(BUILDER);
    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void bake() {
        armor.bake();
        tools.bake();
        enchantments.bake();
        misc.bake();
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            bake();
        }
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            bake();
        }
    }

    public static class ArmorCategory {
        public double bootsMovementBonus = 0.08;
        public double bootsSwimBonus = 0.04;
        public double bootsJumpBoost = 0.3;
        public double bootsJumpFactor = 0.03;

        private final ModConfigSpec.DoubleValue movementSpec;
        private final ModConfigSpec.DoubleValue swimSpec;
        private final ModConfigSpec.DoubleValue jumpBoostSpec;
        private final ModConfigSpec.DoubleValue jumpFactorSpec;

        ArmorCategory(ModConfigSpec.Builder builder) {
            builder.comment("Armor Settings").push("armor");
            movementSpec = builder.comment("The movement speed bonus granted when wearing the Ichorweave Boots of the Biosphere.")
                    .defineInRange("bootsMovementBonus", 0.08, 0.0, 1.0);
            swimSpec = builder.comment("The swim speed bonus granted when wearing the Ichorweave Boots of the Biosphere.")
                    .defineInRange("bootsSwimBonus", 0.04, 0.0, 1.0);
            jumpBoostSpec = builder.comment("The jump height boost granted when wearing the Ichorweave Boots of the Biosphere.")
                    .defineInRange("bootsJumpBoost", 0.3, 0.0, 10.0);
            jumpFactorSpec = builder.comment("The forward momentum speed bonus granted when jumping while wearing the Ichorweave Boots of the Biosphere.")
                    .defineInRange("bootsJumpFactor", 0.03, 0.0, 1.0);
            builder.pop();
        }

        void bake() {
            bootsMovementBonus = movementSpec.get();
            bootsSwimBonus = swimSpec.get();
            bootsJumpBoost = jumpBoostSpec.get();
            bootsJumpFactor = jumpFactorSpec.get();
        }
    }

    public static class ToolCategory {
        public double titanSlayerDamage = 0.2;

        private final ModConfigSpec.DoubleValue titanSlayerSpec;

        ToolCategory(ModConfigSpec.Builder builder) {
            builder.comment("Tool Settings").push("tools");
            titanSlayerSpec = builder.comment(
                            "The amount of damage, based on the percentage of max health of the target, a fully charged Titanslayer",
                            "strike will deal. A value of 0.2 equates to 20% of the target's maximum health.")
                    .defineInRange("titanSlayerDamage", 0.2, 0.0, 1.0);
            builder.pop();
        }

        void bake() {
            titanSlayerDamage = titanSlayerSpec.get();
        }
    }

    public static class EnchantmentsCategory {
        public boolean eternal = true;

        private final ModConfigSpec.BooleanValue eternalSpec;

        EnchantmentsCategory(ModConfigSpec.Builder builder) {
            builder.comment("Enchantment Settings").push("enchantments");
            eternalSpec = builder.comment("Enables the eternal infusion enchant, which makes items unbreakable.")
                    .define("eternal", true);
            builder.pop();
        }

        void bake() {
            eternal = eternalSpec.get();
        }
    }

    public static class MiscCategory {
        public boolean alternateResearchBackground = false;

        private final ModConfigSpec.BooleanValue alternateResearchBackgroundSpec;

        MiscCategory(ModConfigSpec.Builder builder) {
            builder.comment("Miscellaneous Settings").push("misc");
            alternateResearchBackgroundSpec = builder.comment("Enables an alternate much less colorful research background, highly recommended if the default background is too bright and hard to look at.")
                    .define("alternateResearchBackground", false);
            builder.pop();
        }

        void bake() {
            alternateResearchBackground = alternateResearchBackgroundSpec.get();
        }
    }

    private ConfigHandlerKami() {}
}
