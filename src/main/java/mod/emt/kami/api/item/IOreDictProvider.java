package mod.emt.kami.api.item;

/**
 * Legacy Ore Dictionary hook. The Forge Ore Dictionary no longer exists on NeoForge;
 * item interoperability is handled with item tags instead. Retained as a no-op marker so
 * existing item classes keep compiling.
 */
@Deprecated
public interface IOreDictProvider {
    default void registerOreDicts() {}
}
