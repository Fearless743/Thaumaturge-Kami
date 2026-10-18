package mod.emt.kami.items.tools;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.casters.ICaster;
import com.leclowndu93150.thaumaturge.api.items.IArchitect;
import com.leclowndu93150.thaumaturge.api.items.IChanneledItem;
import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import com.leclowndu93150.thaumaturge.api.wands.WandCap;
import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.api.wands.WandVis;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.effect.EffectDispatch;
import com.leclowndu93150.thaumaturge.content.particle.VisSparkleParticleOptions;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.content.wands.WandParts;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import mod.emt.kami.Kami;
import mod.emt.kami.config.ConfigHandlerKami;
import mod.emt.kami.items.IDyeableGear;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Ichorium Caster's Gauntlet of the Ascended: a functional casting gauntlet.
 *
 * <p>In the 1.12 original this extended ThaumicAPI's {@code AbstractItemCaster}; here it implements
 * Thaumaturge's {@link ICaster}, the capability the spell engine, caster HUD, workbench wand slot
 * and focus-swap key all operate on. Casting behaviour is delegated to a hidden {@link ItemWand}
 * prototype: {@code ItemWand} keeps all its state in data components on the held stack (never in
 * the item instance), so delegating produces exactly wand behaviour for the gauntlet's stacks. The
 * gauntlet ships pre-assembled with the {@code kami:ichorium} cap/rod registered below, which give
 * it the 30% vis cost reduction and a 500 vis reserve.</p>
 *
 * <p>Deliberately <em>not</em> a subclass of {@code ItemWand}: Thaumaturge's client
 * {@code WandHandRenderer} replaces the held-item model of every {@code ItemWand} with an assembled
 * rod-and-cap wand, which would hide the gauntlet model in first person. Implementing
 * {@code ICaster} directly keeps the gauntlet model everywhere while remaining a full caster.</p>
 *
 * <p>Thaumaturge offers no per-cast hook into its wand vis storage, so the original's "remaining
 * vis drain is distributed in a 5x5 chunk area" is adapted as an aura feed: while held, the
 * gauntlet siphons vis evenly from the aura of the surrounding 5x5 chunks into its own reserve
 * (see {@link #inventoryTick}), so spells are ultimately paid for by the local aura.</p>
 */
public class ItemIchoriumCaster extends Item implements ICaster, IArchitect, IChanneledItem, IDyeableGear, IWarpingGear {
    public static final int DEFAULT_DYED_COLOR = 16748295;

    /** Ticks between aura drain pulses (20 = once per second). */
    private static final int AURA_DRAIN_INTERVAL = 20;
    /** Chunk radius of the aura drain area, centred on the wielder (2 = 5x5 chunks). */
    private static final int AURA_DRAIN_RADIUS = 2;

    /** Ticks between node-drain sparkle pulses while channelling a node (1 = every tick). */
    private static final int VIS_STREAM_INTERVAL = 1;
    /** Vis sparkles per pulse. */
    private static final int VIS_STREAM_COUNT = 4;
    /** Random spread (blocks) around the node centre for the sparkle origins. */
    private static final float VIS_STREAM_SPREAD = 2.0F;

    /**
     * Never registered: a behaviour prototype whose stack-driven methods (casting, focusing,
     * architect aiming, vis read-out) this gauntlet delegates to. Reuses Thaumaturge's own
     * registered wand — {@code Item} cannot be instantiated after bootstrap (the frozen item
     * registry rejects intrusive holders), and {@code ItemWand} keeps all its state in data
     * components on the held stack (never in the item instance), so any registered wand instance
     * behaves identically for the gauntlet's stacks.
     */
    private static ItemWand wand() {
        return TTItems.WAND.get();
    }

    /**
     * The cap/rod instances are created eagerly as plain constants and registered by reference, so
     * {@link #applyWandParts} can build the default {@code WAND_PARTS} component during item
     * registration without touching the wand-cap/wand-rod registries — those custom registries
     * fill <em>after</em> the vanilla item registry in this NeoForge, so resolving
     * {@code DeferredHolder#get()} there would crash with an unbound value. The registered
     * instances are only needed later, for {@code WandParts} codec (de)serialisation by key.
     */
    private static final WandCap ICHORIUM_CAP_INSTANCE = new WandCap(0.7F, List.of(), 0.0F, 5,
            borrowedPartTexture("wand_cap_void"));
    private static final WandRod ICHORIUM_ROD_INSTANCE = new WandRod(500, 10,
            borrowedPartTexture("wand_rod_silverwood"), null, false, false, false);

    /** Wand caps registered into Thaumaturge's wand-cap registry (kami namespace). */
    public static final DeferredRegister<WandCap> CAPS = DeferredRegister.create(WandCap.REGISTRY_KEY, Kami.MOD_ID);
    /** Wand rods registered into Thaumaturge's wand-rod registry (kami namespace). */
    public static final DeferredRegister<WandRod> RODS = DeferredRegister.create(WandRod.REGISTRY_KEY, Kami.MOD_ID);

    /**
     * 30% vis cost reduction on every aspect (cap cost modifier 0.7); no per-aspect special costs.
     */
    public static final DeferredHolder<WandCap, WandCap> ICHORIUM_CAP = CAPS.register("ichorium", () -> ICHORIUM_CAP_INSTANCE);
    /** 500 vis reserve. */
    public static final DeferredHolder<WandRod, WandRod> ICHORIUM_ROD = RODS.register("ichorium", () -> ICHORIUM_ROD_INSTANCE);

    public ItemIchoriumCaster(Item.Properties properties) {
        super(applyWandParts(properties));
    }

    /**
     * Applies the gauntlet's identity as an {@code Item.Properties} default: the pre-assembled
     * {@code WAND_PARTS} component makes every stack cast with the ichorium cap/rod without going
     * through workbench assembly.
     */
    private static Item.Properties applyWandParts(Item.Properties properties) {
        return properties.rarity(Rarity.EPIC).fireResistant().stacksTo(1)
                .component(TTDataComponents.WAND_PARTS.get(), new WandParts(ICHORIUM_CAP_INSTANCE, ICHORIUM_ROD_INSTANCE, false));
    }

    /**
     * The gauntlet renders with its own item model, and its fixed parts can never be re-assembled
     * in the workbench (there are no ichorium cap/rod items), so the part textures only ever appear
     * in workbench preview slots that the gauntlet cannot enter. Borrowing existing Thaumaturge
     * textures avoids shipping near-duplicate art and prevents missing-texture log spam in any
     * code path that does resolve the identifier.
     */
    private static Identifier borrowedPartTexture(String existing) {
        return Identifier.fromNamespaceAndPath("thaumaturge", "textures/models/" + existing + ".png");
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.descriptionId);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        wand().appendHoverText(stack, context, display, consumer, flag);
        //The vis read-out and cost percentages above already reflect the ichorium cap's 30%
        //discount; this line explains where the vis comes from.
        consumer.accept(Component.translatable("tooltip.kami.ichorium_caster.aura")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    //Casting behaviour: delegated to the wand prototype, which drives everything from the held
    //stack's data components (socketed focus, wand parts, stored vis).

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return wand().use(level, player, hand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return wand().onItemUseFirst(stack, context);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return wand().getUseDuration(stack, entity);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return wand().getUseAnimation(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        wand().onUseTick(level, entity, stack, ticksRemaining);
        //The vanilla node-tap drain only tints the node itself; stream vis sparkles from the node
        //to the wielder so the absorption is clearly visible.
        if (level instanceof ServerLevel serverLevel && entity instanceof Player player
                && ticksRemaining % VIS_STREAM_INTERVAL == 0) {
            BlockEntityNode node = targetedAuraNode(player);
            if (node != null && !node.getAspects().isEmpty() && hasReserveRoom(stack)) {
                spawnDrainStream(serverLevel, node, player);
            }
        }
    }

    /** Mirrors the wand's own node-targeting check (right-click aimed at an aura node). */
    private static BlockEntityNode targetedAuraNode(Player player) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        return hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                && player.level().getBlockEntity(blockHit.getBlockPos()) instanceof BlockEntityNode node ? node : null;
    }

    private static boolean hasReserveRoom(ItemStack stack) {
        WandVis reserve = WandVisHelper.getAllVis(stack);
        int max = WandVisHelper.getMaxVis(stack);
        for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
            if (reserve.amount(primal) < max) {
                return true;
            }
        }
        return false;
    }

    /**
     * Streams vis sparkles from the node to the wielder's eyes, coloured by the node's dominant
     * primal aspect. Spawned server-side ({@code EffectDispatch} broadcasts to tracking clients).
     */
    private static void spawnDrainStream(ServerLevel level, BlockEntityNode node, Player player) {
        Vec3 nodePos = Vec3.atCenterOf(node.getBlockPos());
        Vec3 eye = player.getEyePosition();
        int color = dominantAspectColor(node);

        for (int i = 0; i < VIS_STREAM_COUNT; i++) {
            Vec3 jitter = new Vec3(
                    (level.getRandom().nextFloat() - 0.5F) * VIS_STREAM_SPREAD,
                    (level.getRandom().nextFloat() - 0.5F) * VIS_STREAM_SPREAD,
                    (level.getRandom().nextFloat() - 0.5F) * VIS_STREAM_SPREAD);
            EffectDispatch.spawnVisSparkle(level, nodePos.add(jitter), eye.add(jitter.scale(0.25)), color);
        }
    }

    /** The colour of the node's most abundant primal aspect, as used for the node drain tint. */
    private static int dominantAspectColor(BlockEntityNode node) {
        int best = 0;
        int bestAmount = 0;
        int color = VisSparkleParticleOptions.DEFAULT_COLOR;
        for (AspectInstance instance : node.getAspects().entries()) {
            if (instance.aspect().value().isPrimal() && instance.amount() > bestAmount) {
                bestAmount = instance.amount();
                color = instance.aspect().value().color();
                best++;
            }
        }
        return best > 0 ? color : VisSparkleParticleOptions.DEFAULT_COLOR;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        return wand().releaseUsing(stack, level, entity, remainingTime);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        //Delegated so vis changes while held don't re-trigger the equip animation.
        return wand().shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    //ICaster

    @Override
    public ItemStack getFocusStack(ItemStack stack) {
        return wand().getFocusStack(stack);
    }

    @Override
    public void setFocus(ItemStack stack, ItemStack focus) {
        wand().setFocus(stack, focus);
    }

    @Override
    public float getConsumptionModifier(ItemStack stack, Player player, boolean crafting) {
        return wand().getConsumptionModifier(stack, player, crafting);
    }

    @Override
    public boolean consumeVis(ItemStack stack, Player player, float amount, boolean crafting, boolean simulate) {
        return wand().consumeVis(stack, player, amount, crafting, simulate);
    }

    @Override
    public BlockState getPickedBlock(ItemStack stack) {
        return wand().getPickedBlock(stack);
    }

    //IArchitect

    @Override
    public HitResult aim(ItemStack stack, Level level, LivingEntity caster) {
        return wand().aim(stack, level, caster);
    }

    @Override
    public boolean replacesBlockHighlight(ItemStack stack) {
        return wand().replacesBlockHighlight(stack);
    }

    @Override
    public List<BlockPos> previewBlocks(ItemStack stack, Level level, BlockPos pos, Direction side, Player player) {
        return wand().previewBlocks(stack, level, pos, side, player);
    }

    @Override
    public boolean showsAxis(ItemStack stack, Level level, Player player, Direction side, Direction.Axis axis) {
        return wand().showsAxis(stack, level, player, side, axis);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player) || !player.isAlive()
                || (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND)
                || player.tickCount % AURA_DRAIN_INTERVAL != 0) {
            return;
        }
        drainAuraIntoReserve(stack, level, player);
    }

    /**
     * Drains vis from the aura of the 5x5 chunk area around the wielder and pours it into the
     * gauntlet's reserve, spending each chunk equally (the original "distributed in a 5x5 chunk
     * area" behaviour). The drained vis fills whichever primal aspects are most depleted first.
     */
    private static void drainAuraIntoReserve(ItemStack stack, ServerLevel level, Player player) {
        WandVis reserve = WandVisHelper.getAllVis(stack);
        int max = WandVisHelper.getMaxVis(stack);

        int deficit = 0;
        for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
            deficit += Math.max(0, max - reserve.amount(primal));
        }
        if (deficit <= 0) {
            return;
        }

        int budget = (int) Math.min(deficit, ConfigHandlerKami.misc.ichoriumCasterAuraDrain * 100);
        float drained = drainAuraChunks(level, player.blockPosition(), budget);
        if (drained < 1.0F) {
            return;
        }
        distribute(stack, reserve, max, (int) drained);
    }

    /** Attempts to drain {@code budget} centivis, taking an equal share from each of the 25 chunks. */
    private static float drainAuraChunks(ServerLevel level, BlockPos center, int budget) {
        float drained = 0.0F;
        float perChunk = budget / (float) ((AURA_DRAIN_RADIUS * 2 + 1) * (AURA_DRAIN_RADIUS * 2 + 1));
        ChunkPos centerChunk = ChunkPos.containing(center);

        for (int x = -AURA_DRAIN_RADIUS; x <= AURA_DRAIN_RADIUS && drained < budget; x++) {
            for (int z = -AURA_DRAIN_RADIUS; z <= AURA_DRAIN_RADIUS && drained < budget; z++) {
                BlockPos pos = new ChunkPos(centerChunk.x() + x, centerChunk.z() + z).getWorldPosition();
                drained += AuraHelper.drainVis(level, pos, Math.min(perChunk, budget - drained), false);
            }
        }
        return drained;
    }

    /** Fills the most depleted primal aspects first; the aspect split mirrors wand-vis semantics. */
    private static void distribute(ItemStack stack, WandVis reserve, int max, int centivis) {
        List<ResourceKey<IAspect>> order = new ArrayList<>(TTAspects.PRIMALS);
        order.sort(Comparator.comparingInt(reserve::amount));

        WandVis result = reserve;
        for (ResourceKey<IAspect> primal : order) {
            if (centivis <= 0) {
                break;
            }
            int space = max - result.amount(primal);
            if (space <= 0) {
                continue;
            }
            int add = Math.min(space, centivis);
            result = result.with(primal, result.amount(primal) + add);
            centivis -= add;
        }
        WandVisHelper.setAllVis(stack, result);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        //Right-clicking a filled cauldron washes the dye out.
        if (state.getBlock() instanceof LayeredCauldronBlock
                && state.getValue(LayeredCauldronBlock.LEVEL) > 0
                && getDyedColor(stack) != DEFAULT_DYED_COLOR) {
            setDyedColor(stack, DEFAULT_DYED_COLOR);
            if (!level.isClientSide()) {
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getDyedColor(ItemStack stack) {
        return stack.getOrDefault(DataComponents.DYED_COLOR, new DyedItemColor(DEFAULT_DYED_COLOR)).rgb();
    }

    @Override
    public void setDyedColor(ItemStack stack, int color) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 3;
    }
}
