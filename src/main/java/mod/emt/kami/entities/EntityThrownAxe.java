package mod.emt.kami.entities;

import mod.emt.kami.items.tools.ItemAwakenedAxe;
import mod.emt.kami.registry.ModEntitiesKAMI;
import mod.emt.kami.registry.ModItemsKAMI;
import mod.emt.kami.registry.ModSoundsKAMI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The thrown Awakened Ichorium Axe. Flies out, then curves back to its thrower like a boomerang.
 * Ported from the 1.12 {@code EntityThrowable} implementation onto
 * {@link ThrowableItemProjectile} (which owns the synced item stack).
 */
public class EntityThrownAxe extends ThrowableItemProjectile {
    public static final int MAX_THROW_TIME = 20;
    public static final float MAX_BOOMERANG_SPEED = 1.0F;
    public static final int BOOMERANG_DAMAGE = 10;

    public int returnTime;
    public int thrownFromSlot;
    public float damage;
    public float speed;

    public EntityThrownAxe(EntityType<EntityThrownAxe> type, Level level) {
        super(type, level);
        this.setGlowingTag(true);
        this.returnTime = MAX_THROW_TIME;
        this.noPhysics = false;
        this.damage = BOOMERANG_DAMAGE;
        this.speed = MAX_BOOMERANG_SPEED;
    }

    public EntityThrownAxe(Level level, LivingEntity thrower, ItemStack axeThrown) {
        super(ModEntitiesKAMI.THROWN_AWAKENED_ICHORIUM_AXE.get(), thrower, level, axeThrown);
        this.setGlowingTag(true);
        this.returnTime = MAX_THROW_TIME;
        this.noPhysics = false;
        this.damage = BOOMERANG_DAMAGE;
        this.speed = MAX_BOOMERANG_SPEED;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItemsKAMI.AWAKENED_ICHORIUM_AXE.get();
    }

    @Override
    protected double getDefaultGravity() {
        // The boomerang flies in a straight/curved line rather than falling.
        return 0.0D;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return this.damage;
    }

    public void setSpeedModifier(float modifier) {
        this.speed *= modifier;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setAxeItem(ItemStack stack) {
        this.setItem(stack);
    }

    public ItemStack getAxeItem() {
        return this.getItem();
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            Player thrower = this.getOwner() instanceof Player player ? player : null;
            if (thrower != null) {
                if (thrower.getAbilities().instabuild) {
                    int slot = thrower.getInventory().getSelectedSlot();
                    ItemStack hand = thrower.getInventory().getItem(slot);
                    if (ItemStack.isSameItem(this.getAxeItem(), hand)) {
                        thrower.getInventory().setItem(slot, ItemStack.EMPTY);
                    }
                }

                if (this.returnTime == MAX_THROW_TIME) {
                    thrower.swing(InteractionHand.MAIN_HAND);
                    this.shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0.0F, this.getSpeed(), 0.0F);
                }

                if (this.returnTime % 3 == 0) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSoundsKAMI.ITEM_ICHOR_THROW.get(), SoundSource.PLAYERS, 2.0F,
                            3.0F + 1.0F / (1.0F + this.distanceTo(thrower) / 16.0F));
                }

                this.returnTime--;

                if (this.returnTime == 0) {
                    this.reverseDirection(true);
                }

                if (this.returnTime <= 0) {
                    Vec3 motion = this.getDeltaMovement();
                    float currentHeading = (float) Math.atan2(motion.z, motion.x);
                    float headingToThrower = (float) Math.atan2(thrower.getZ() - this.getZ(), thrower.getX() - this.getX());
                    float curveScale = Math.max(0.3F, this.returnTime * 0.007F);
                    float newHeading = this.updateRotationRadians(currentHeading, headingToThrower, curveScale);
                    double currentPitch = Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z));
                    double targetPitch = Math.atan2(
                            thrower.getY() + thrower.getEyeHeight() - this.getY(),
                            Math.sqrt((thrower.getX() - this.getX()) * (thrower.getX() - this.getX())
                                    + (thrower.getZ() - this.getZ()) * (thrower.getZ() - this.getZ())));
                    float newPitch = this.updateRotationRadians((float) currentPitch, (float) targetPitch, curveScale * 0.3F);
                    this.shoot(Math.cos(newHeading), Math.sin(newPitch), Math.sin(newHeading), this.getSpeed(), 0.0F);
                }
            }

            if (this.getVehicle() == null) {
                for (Entity e : this.level().getEntities(this, this.getBoundingBox().inflate(1.5D),
                        e -> e instanceof ItemEntity)) {
                    e.startRiding(this);
                    break;
                }
            }

            if (this.returnTime < -100) {
                if (this.getOwner() instanceof Player player && player.level() == this.level()) {
                    this.returnBoomerangToPlayer(player);
                } else if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(),
                            this.getAxeItem().copy()));
                }
                this.discard();
            }
        } else {
            Vec3 motion = this.getDeltaMovement();
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(ParticleTypes.CRIT,
                        this.getX() + motion.x * (double) i / 4.0D,
                        this.getY() + motion.y * (double) i / 4.0D,
                        this.getZ() + motion.z * (double) i / 4.0D,
                        -motion.x, -motion.y + 0.2D, -motion.z);
            }
        }

        super.tick();

        // Legacy spin: the axe rotates about its flight axis.
        this.xRotO = (this.returnTime + 1) * 50.0F;
        this.setXRot(this.returnTime * 50.0F);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity hit = result.getEntity();
        Entity owner = this.getOwner();

        if (hit == owner && this.returnTime <= 0 && owner instanceof Player player) {
            this.returnBoomerangToPlayer(player);
            if (this.isAlive() && this.level() instanceof ServerLevel) {
                this.playSound(SoundEvents.ITEM_PICKUP, 0.2F,
                        ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            }
            this.discard();
            return;
        }

        if (hit != owner && owner != null && hit instanceof LivingEntity living) {
            ItemAwakenedAxe.EnumImpactMode mode = ItemAwakenedAxe.EnumImpactMode.getMode(this.getAxeItem());
            mode.onImpact(this, living);

            ItemStack axe = this.getAxeItem();
            if (owner instanceof LivingEntity thrower && this.level() instanceof ServerLevel serverLevel) {
                axe.hurtAndBreak(1, serverLevel, thrower, item -> this.level().playSound(null,
                        this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_BREAK.value(),
                        SoundSource.PLAYERS, 1.0F, 0.8F));
            }
            if (axe.isEmpty()) {
                this.discard();
            }
            this.reverseDirection(true);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide()) {
            return;
        }
        BlockPos pos = result.getBlockPos();
        BlockState state = this.level().getBlockState(pos);
        boolean solid = !state.getCollisionShape(this.level(), pos).isEmpty();
        if (!this.noPhysics && solid) {
            ItemAwakenedAxe.EnumImpactMode mode = ItemAwakenedAxe.EnumImpactMode.getMode(this.getAxeItem());
            mode.onImpact(this, null);
            this.reverseDirection(true);
        }
    }

    private void returnBoomerangToPlayer(Player player) {
        if (!this.level().isClientSide()) {
            ItemStack crowdedItem = player.getInventory().getItem(this.thrownFromSlot);
            player.getInventory().setItem(this.thrownFromSlot, this.getAxeItem());
            if (!crowdedItem.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, crowdedItem);
            }
        }
    }

    private void reverseDirection(boolean noClip) {
        this.returnTime = Math.min(this.returnTime, 0);
        this.noPhysics = noClip;
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.scale(-1.0D));
    }

    private float updateRotationRadians(float currHeading, float throwerHeading, float curveScale) {
        float diff = (throwerHeading - currHeading);
        diff %= (float) Math.PI * 2;
        if (diff >= Math.PI) diff -= (float) Math.PI * 2;
        if (diff < -Math.PI) diff += (float) Math.PI * 2;
        if (diff > curveScale) diff = curveScale;
        if (diff < -curveScale) diff = -curveScale;
        return currHeading + diff;
    }
}
