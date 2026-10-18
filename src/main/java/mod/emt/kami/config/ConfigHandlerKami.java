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
        public double wardReflectance = 0.25;
        public double wardBarrierBoost = 1.5;
        public double avalancheDamage = 0.4;
        public double spellbreakerDamage = 8.0;
        public double lungeDistance = 7.0;
        public double lungeDamageMultiplier = 1.5;

        private final ModConfigSpec.DoubleValue titanSlayerSpec;
        private final ModConfigSpec.DoubleValue wardReflectanceSpec;
        private final ModConfigSpec.DoubleValue wardBarrierBoostSpec;
        private final ModConfigSpec.DoubleValue avalancheDamageSpec;
        private final ModConfigSpec.DoubleValue spellbreakerDamageSpec;
        private final ModConfigSpec.DoubleValue lungeDistanceSpec;
        private final ModConfigSpec.DoubleValue lungeDamageMultiplierSpec;

        ToolCategory(ModConfigSpec.Builder builder) {
            builder.comment("Tool Settings").push("tools");
            titanSlayerSpec = builder.comment(
                            "The extra damage, based on a percentage of the target's max health, that a fully charged",
                            "Titanslayer strike deals against attuned targets. It is added on top of the strike's own",
                            "damage (attack attribute, enchantments, effects). A value of 0.2 equates to 20% of the",
                            "target's maximum health.")
                    .defineInRange("titanSlayerDamage", 0.2, 0.0, 1.0);
            wardReflectanceSpec = builder.comment(
                            "The fraction of blocked damage the Ichorium Shield of the Warden reflects back at the attacker",
                            "while in Reflect mode. A value of 0.25 equates to 25% of the blocked damage.")
                    .defineInRange("wardReflectance", 0.25, 0.0, 1.0);
            wardBarrierBoostSpec = builder.comment(
                            "The multiplier applied to blocked damage while the Ichorium Shield of the Warden is in Barrier mode.",
                            "A value of 1.5 blocks an additional 50% of the incoming damage.")
                    .defineInRange("wardBarrierBoost", 1.5, 1.0, 10.0);
            avalancheDamageSpec = builder.comment(
                            "The extra damage, based on a percentage of the target's max health, that an Avalanche",
                            "mallet strike deals against targets at full health. It is added on top of the strike's",
                            "own damage (smash bonus, enchantments, effects). A value of 0.4 equates to 40% of the",
                            "target's maximum health.")
                    .defineInRange("avalancheDamage", 0.4, 0.0, 1.0);
            spellbreakerDamageSpec = builder.comment(
                            "The flat bonus damage dealt by a Spellbreaker spear strike.")
                    .defineInRange("spellbreakerDamage", 8.0, 0.0, 100.0);
            lungeDistanceSpec = builder.comment(
                            "The distance of the Lunge dash of the Spellbreaker spear.")
                    .defineInRange("lungeDistance", 7.0, 0.0, 20.0);
            lungeDamageMultiplierSpec = builder.comment(
                            "The bonus damage, as a multiplier of the attacker's attack damage, that the",
                            "Spellbreaker spear lunge adds on top of each struck target's normal attack",
                            "damage. A value of 1.5 equates to 150% of the attack damage as extra damage.")
                    .defineInRange("lungeDamageMultiplier", 1.5, 0.0, 20.0);
            builder.pop();
        }

        void bake() {
            titanSlayerDamage = titanSlayerSpec.get();
            wardReflectance = wardReflectanceSpec.get();
            wardBarrierBoost = wardBarrierBoostSpec.get();
            avalancheDamage = avalancheDamageSpec.get();
            spellbreakerDamage = spellbreakerDamageSpec.get();
            lungeDistance = lungeDistanceSpec.get();
            lungeDamageMultiplier = lungeDamageMultiplierSpec.get();
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
        public double ichoriumCasterAuraDrain = 5.0;

        private final ModConfigSpec.BooleanValue alternateResearchBackgroundSpec;
        private final ModConfigSpec.DoubleValue ichoriumCasterAuraDrainSpec;

        MiscCategory(ModConfigSpec.Builder builder) {
            builder.comment("Miscellaneous Settings").push("misc");
            alternateResearchBackgroundSpec = builder.comment("Enables an alternate much less colorful research background, highly recommended if the default background is too bright and hard to look at.")
                    .define("alternateResearchBackground", false);
            ichoriumCasterAuraDrainSpec = builder.comment("The amount of vis per second the Ichorium Caster's Gauntlet draws from the aura in the 5x5 chunk area around its wielder.")
                    .defineInRange("ichoriumCasterAuraDrain", 5.0, 0.0, 100.0);
            builder.pop();
        }

        void bake() {
            alternateResearchBackground = alternateResearchBackgroundSpec.get();
            ichoriumCasterAuraDrain = ichoriumCasterAuraDrainSpec.get();
        }
    }

    private ConfigHandlerKami() {}
}
