package mod.emt.kami.registry;

import mod.emt.kami.Kami;
import mod.emt.kami.entities.EntityThrownAxe;
import mod.emt.kami.entities.EntityThrownTrident;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntitiesKAMI {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Kami.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<EntityThrownAxe>> THROWN_AWAKENED_ICHORIUM_AXE =
            ENTITY_TYPES.register("thrown_awakened_ichorium_axe", () -> EntityType.Builder.<EntityThrownAxe>of(EntityThrownAxe::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Kami.MOD_ID, "thrown_awakened_ichorium_axe"))));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityThrownTrident>> THROWN_ICHORIUM_TRIDENT =
            ENTITY_TYPES.register("thrown_ichorium_trident", () -> EntityType.Builder.<EntityThrownTrident>of(EntityThrownTrident::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Kami.MOD_ID, "thrown_ichorium_trident"))));

    private ModEntitiesKAMI() {}
}
