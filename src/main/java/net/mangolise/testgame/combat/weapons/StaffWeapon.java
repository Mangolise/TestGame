package net.mangolise.testgame.combat.weapons;

import net.kyori.adventure.text.Component;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.SweepResult;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.event.trait.RecursiveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.block.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public record StaffWeapon(int level) implements Attack.Node {
    
    public static final Tag<Player> STAFF_USER = Tag.Transient("testgame.attack.staff.user");
    public static final Tag<Double> VELOCITY = Tag.Double("testgame.attack.staff.velocity").defaultValue(56.0);
    public static final Tag<Double> EXPLOSION_SIZE = Tag.Double("testgame.attack.staff.explosion_size").defaultValue(3.0);
    
    @Override
    public void attack(Attack attack, Consumer<Attack> next) {
        
        if (next != null) {
            // modifiers only
            
            // staff has a staff damage of 3, and increases by 0.5 per level
            attack.setTag(Attack.DAMAGE, 3 + level * 0.5);

            // staff has a base crit change of 0.5, and increases by 0.1 per level
            attack.setTag(Attack.CRIT_CHANCE, 0.5 + level * 0.1);

            next.accept(attack);
            return;
        }
        
        // next == null means that we perform the attack
        Player user = attack.getTag(STAFF_USER);
        if (user == null) {
            throw new IllegalStateException("StaffWeapon attack called without a user set in the tags.");
        }
        
        // spawn spell
        var instance = user.getInstance();

        var fireballEntity = new FireBallEntity(user, attack);
        fireballEntity.setNoGravity(true);

        var playerScale = user.getAttribute(Attribute.SCALE).getValue();

        var point = user.getPosition().add(user.getPosition().direction().mul(attack.getTag(EXPLOSION_SIZE) / 2.0));
        fireballEntity.setInstance(instance, point.add(0, user.getEyeHeight() * playerScale, 0));
        
        var velocity = user.getPosition().direction().mul(attack.getTag(VELOCITY));

        fireballEntity.setVelocity(velocity);

        MinecraftServer.getGlobalEventHandler().addListener(EventListener.builder(ProjectileCollideEvent.class)
                .handler(event -> {
                    Collection<Entity> entities = instance.getNearbyEntities(event.getCollisionPosition(), attack.getTag(EXPLOSION_SIZE));
                    for (Entity entity : entities) {
                        if (entity instanceof AttackableMob mob) {
                            mob.applyAttack(DamageType.ARROW, attack);
                        }
                    }

                    instance.sendGroupedPacket(new ParticlePacket(Particle.SMOKE, false, true, fireballEntity.getPosition(), new Pos(0, 0, 0), 1, 75));
                    instance.sendGroupedPacket(new ParticlePacket(Particle.EXPLOSION, false, true, fireballEntity.getPosition(), new Pos(0, 0, 0), 1, 5));
                    instance.sendGroupedPacket(new ParticlePacket(Particle.LAVA, false, true, fireballEntity.getPosition(), new Pos(0, 0, 0), 1, 25));
                    fireballEntity.remove();
                })
                .filter(event -> event.getEntity() == fireballEntity)
                .expireCount(1)
                .expireWhen(ignored -> fireballEntity.isRemoved())
                .build()
        );
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }

    class FireBallEntity extends Entity {
        private final Player shooter;

        public FireBallEntity(@Nullable Player shooter, Attack attack) {
            super(EntityType.FIREBALL);
            this.shooter = shooter;

            hasPhysics = false;
//        editEntityMeta(ArrowMeta.class, meta -> {
//            meta.setShooter(shooter);
//        });

            setBoundingBox(new BoundingBox(0, 0, 0));
        }

        @Nullable
        public Entity getShooter() {
            return this.shooter;
        }

        private double remainingParticleDistance = 0;

        @Override
        public void tick(long time) {
            final Pos from = getPosition();
            super.tick(time);

            if (isRemoved()) {
                return;
            }

            Pos to = getPosition();

            // Create particles
            double step = 5;

            double travelled = remainingParticleDistance;
            remainingParticleDistance += to.distance(from);

            while (travelled < remainingParticleDistance) {
                Pos pos = from.add(to.sub(from).asVec().normalize().mul(travelled));
                ParticlePacket packet = new ParticlePacket(Particle.CRIT, pos, Vec.ZERO, 0, 1);
                sendPacketToViewers(packet);
                travelled += step;
            }

            if (isStuck(from, to)) {
                remove();
            }
        }

        /**
         * Checks whether an arrow is stuck in block / hit an entity.
         *
         * @param from position right before current tick.
         * @param to   position after current tick.
         * @return if an arrow is stuck in block / hit an entity.
         */
        private boolean isStuck(Pos from, Pos to) {
            Instance instance = getInstance();
            if (instance == null) {
                return false;
            }

            if (from.samePoint(to)) {
                return true;
            }

            BoundingBox boundingBox = getBoundingBox();

            Vec movement = to.asVec().sub(from);
            double length = movement.length();
            Vec dir = movement.div(length);
            long aliveTicks = getAliveTicks();

            // Check collision with blocks
            Iterable<Point> iterable = () -> new BlockIterator(from.asVec(), dir, 0, length);
            for (Point pos : iterable) {
                Block block = instance.getBlock(pos);

                if (!block.isSolid()) {
                    continue;
                }

                BlockVec posBlock = new BlockVec(pos);

                // do collision check with shape
                SweepResult result = new SweepResult(Double.MAX_VALUE, 0, 0, 0, null, 0, 0, 0, 0, 0, 0);
                if (!block.registry().collisionShape().intersectBoxSwept(from, movement, posBlock.asVec(), boundingBox, result)) {
                    continue;
                }

                Pos posPos = getCollidedPosition(result).asPosition().withView(from);

                ProjectileCollideEvent event = new ProjectileCollideEvent(this, posPos);
                EventDispatcher.call(event);
                if (!event.isCancelled()) {
                    if (!this.isRemoved()) {
                        teleport(posPos);
                    }
                    return true;
                }
            }

            // Collide with entities
            for (Entity entity : instance.getNearbyEntities(from, length * 4d)) {
                if (!(entity instanceof LivingEntity) ||
                        (entity instanceof Player player && player.getGameMode().equals(GameMode.SPECTATOR)) ||
                        (aliveTicks < 3 && entity == shooter)) {
                    continue;
                }

                if (!(entity instanceof AttackableMob)) {
                    continue;
                }

                SweepResult result = new SweepResult(Double.MAX_VALUE, 0, 0, 0, null, 0, 0, 0, 0, 0, 0);
                if (entity.getBoundingBox().intersectBoxSwept(from.asVec(), movement, entity.getPosition(), boundingBox, result)) {
                    Pos posPos = getCollidedPosition(result).asPosition().withView(from);
                    final ProjectileCollideEvent event = new ProjectileCollideEvent(this, posPos);
                    EventDispatcher.call(event);
                    if (!event.isCancelled()) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    // Getting the collided position is package-private, so we use reflection :(
    private Vec getCollidedPosition(SweepResult result) {
        return new Vec(
                getCollidedVar(result, 'X'),
                getCollidedVar(result, 'Y'),
                getCollidedVar(result, 'Z')
        );
    }

    private double getCollidedVar(SweepResult result, char coord) {
        try {
            Field field = SweepResult.class.getDeclaredField("collidedPosition" + coord);
            field.setAccessible(true);
            return (double) field.get(result);
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}

class ProjectileCollideEvent implements EntityInstanceEvent, CancellableEvent, RecursiveEvent {

    private final @NotNull Entity projectile;
    private final @NotNull Pos position;
    private boolean cancelled;

    protected ProjectileCollideEvent(@NotNull Entity projectile, @NotNull Pos position) {
        this.projectile = projectile;
        this.position = position;
    }

    @Override
    public @NotNull Entity getEntity() {
        return projectile;
    }

    public @NotNull Pos getCollisionPosition() {
        return position;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
}

