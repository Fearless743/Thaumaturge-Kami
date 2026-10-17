package mod.emt.kami.registry;

import mod.emt.kami.Kami;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound events registered by KAMI. Access a sound via {@link #EQUIP_ROBE}.get().
 */
public final class ModSoundsKAMI {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Kami.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> EQUIP_BAUBLE = sound("equip.bauble");
    public static final DeferredHolder<SoundEvent, SoundEvent> EQUIP_ROBE = sound("equip.robe");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_ICHOR_BURY = sound("item.ichor.bury");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_ICHOR_DIG = sound("item.ichor.dig");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_ICHOR_JUMP = sound("item.ichor.jump");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_ICHOR_TOGGLE = sound("item.ichor.toggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_ICHOR_THROW = sound("item.ichor.throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNEQUIP_BAUBLE = sound("unequip.bauble");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Kami.MOD_ID, path)));
    }

    private ModSoundsKAMI() {}
}
