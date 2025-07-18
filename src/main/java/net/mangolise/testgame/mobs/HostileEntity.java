package net.mangolise.testgame.mobs;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.mods.Mod;
import net.mangolise.testgame.combat.mods.BundleMenu;
import net.mangolise.testgame.mobs.spawning.WaveSystem;
import net.mangolise.testgame.util.Throttler;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.event.entity.EntityDespawnEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public abstract non-sealed class HostileEntity extends EntityCreature implements AttackableMob {
    public HostileEntity(@NotNull EntityType entityType) {
        super(entityType);
    }

    @Override
    public void applyAttack(RegistryKey<DamageType> type, Attack attack) {
        // Apply the attack to the zombie, e.g., reduce health
        var user = attack.getTag(Attack.USER);
        double damage = attack.getTag(Attack.DAMAGE);

        // convert crit into additional damage
        double critMultiplier = 1.0 + attack.sampleCrits();

        // when the crit multiplier is above 1.9, ping a ding sound to the user if they are a player
        if (critMultiplier > 1.9 && user instanceof Player player) {
            player.playSound(Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.HOSTILE, 0.01f, 1f));
        }

        this.damage(new Damage(type, null, user, null, (float) (damage * critMultiplier)));
    }

    @Override
    public EntityCreature asEntity() {
        return this;
    }
    
    public abstract void doTickUpdate(long time);

    public final void tick(long time) {
        // skip the ticks when we are not in an instance
        if (this.instance == null) {
            super.tick(time);
            return;
        }
        if (!this.isDead() && Throttler.shouldThrottle(this.instance, "testgame.hostileentity.tick", 40)) {
            return;
        }

        Throttler.useTime(this.instance, "testgame.hostileentity.tick", 40, () -> {
            // Update health tag
            final int segments = 10;
            double percentHealth = this.getHealth() / this.getAttribute(Attribute.MAX_HEALTH).getValue();
            int healthSegments = (int) (percentHealth * segments);
            int emptySegments = segments - healthSegments;
            healthSegments = Math.max(healthSegments, 0);
            emptySegments = Math.max(emptySegments, 0);
            String healthBar = "&a" + "█".repeat(healthSegments) + "&c" + "█".repeat(emptySegments);
            this.set(DataComponents.CUSTOM_NAME, ChatUtil.toComponent(healthBar));
            this.setCustomNameVisible(true);

            try {
                if (this.getTarget() != null && this.getTarget().isRemoved()) {
                    // if the target is removed, clear it
                    this.setTarget(null);
                }
                super.tick(time);
            } catch (NullPointerException e) {
                // TODO: (after the jam) report this bug
//                java.lang.NullPointerException: Unloaded chunk at -129,16,129
//                at net.minestom.server.instance.Instance.getBlock(Instance.java:720)
//                at net.minestom.server.entity.pathfinding.generators.GroundNodeGenerator.gravitySnap(GroundNodeGenerator.java:123)
                if (e.getMessage() != null && e.getMessage().contains("Unloaded chunk at")) {
                    // this is a known issue, we can safely ignore it
                    return;
                }
                // rethrow the exception if it's not the known issue
                throw e;
            }

            var instance = this.getInstance();

            if (instance == null) {
                return;
            }
            
            if (this.position.y() < instance.getCachedDimensionType().minY() || this.position.y() > instance.getCachedDimensionType().maxY()) {
                // if the entity is outside the world, remove it
                this.remove();
                return;
            }

            Vec movementFinisher = new Vec(1, 0.1, 1);
            Vec movementVec = Vec.ZERO;

            var entities = instance.getNearbyEntities(this.position, 3);
            for (Entity entity : entities) {
                if (!(entity instanceof HostileEntity other)) {
                    continue;
                }

                if (other == this) {
                    continue;
                }

                // when this entity is close to the other entity, we apply a force to move away from it
                Vec thisPosition = Vec.fromPoint(this.getPosition());
                Vec otherPosition = Vec.fromPoint(other.getPosition());
                Vec difference = thisPosition.sub(otherPosition);
                Vec direction = difference.normalize();

                // amplify the force based on the distance
                double distance = difference.length();
                if (distance < 0.2) {
                    distance = 0.2;
                }

                double force = 1.0 / (distance * distance);

                // increase the force based on the size of the other zombie
                double otherScale = other.getAttribute(Attribute.SCALE).getValue();
                force *= (otherScale * otherScale);

                movementVec = movementVec.add(direction.mul(force));
            }

            // apply the movement vector to the zombie
            if (!movementVec.equals(Vec.ZERO)) {
                this.setVelocity(this.getVelocity().add(movementVec.mul(movementFinisher)));
            }

            this.doTickUpdate(time);
        });
    }

    @Override
    protected void refreshIsDead(boolean isDead) {
        super.refreshIsDead(isDead);

        double random = Math.random();

        double currentWaveSize = WaveSystem.from(instance).getCurrentWaveSize();
        double currentWave = WaveSystem.from(instance).getCurrentWave();
        
        double expectedNumberOfDrops = Math.pow(1.1, currentWave);
        
        if (isDead && random <= (expectedNumberOfDrops / currentWaveSize)) {
            ItemEntity itemEntity = GameSdkUtils.dropItem(instance, this.position, BundleMenu.createBundleItem(false));
            itemEntity.setPickable(true);
            itemEntity.setGlowing(true);

            Entity displayEntity = createDisplayEntity(itemEntity);
            itemEntity.addPassenger(displayEntity);

            itemEntity.eventNode().addListener(EntityDespawnEvent.class, e -> {
                e.getEntity().getPassengers().stream().findAny().ifPresent(Entity::remove);
            });
        }
    }

    private Entity createDisplayEntity(ItemEntity itemEntity) {
        Entity entity = new Entity(EntityType.BLOCK_DISPLAY);

        Mod.Rarity rarity = itemEntity.getItemStack().getTag(BundleMenu.BUNDLE_RARITY);

        Vec scale = new Vec(0.1, 50, 0.1);

        if (rarity == Mod.Rarity.EPIC) {
            entity.editEntityMeta(BlockDisplayMeta.class, meta -> {
                meta.setBlockState(Block.PURPLE_STAINED_GLASS);
                meta.setScale(scale);
                meta.setHasNoGravity(true);
                meta.setHasGlowingEffect(true);
                meta.setGlowColorOverride(Color.MAGENTA.getRGB());
                meta.setTranslation(scale.withY(0).div(-2));
            });
        } else if (rarity == Mod.Rarity.RARE) {
            entity.editEntityMeta(BlockDisplayMeta.class, meta -> {
                meta.setBlockState(Block.BLUE_STAINED_GLASS);
                meta.setScale(scale);
                meta.setHasNoGravity(true);
                meta.setHasGlowingEffect(true);
                meta.setGlowColorOverride(Color.BLUE.getRGB());
                meta.setTranslation(scale.withY(0).div(-2));
            });
        } else {
            entity.editEntityMeta(BlockDisplayMeta.class, meta -> {
                meta.setBlockState(Block.LIGHT_GRAY_STAINED_GLASS);
                meta.setScale(scale);
                meta.setHasNoGravity(true);
                meta.setHasGlowingEffect(true);
                meta.setGlowColorOverride(Color.GRAY.getRGB());
                meta.setTranslation(scale.withY(0).div(-2));
            });
        }

        return entity;
    }
}
