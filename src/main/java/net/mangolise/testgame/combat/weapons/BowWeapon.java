package net.mangolise.testgame.combat.weapons;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.SweepResult;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.projectile.ArrowMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.block.BlockIterator;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;

import static net.mangolise.testgame.combat.weapons.BowWeapon.BOW_USER;

public record BowWeapon(int level) implements Attack.Node {
    
    public static final Tag<Player> BOW_USER = Tag.Transient("testgame.attack.bow.user");
    public static final Tag<Double> VELOCITY = Tag.Double("testgame.attack.bow.velocity").defaultValue(48.0);
    
    @Override
    public void attack(Attack attack, Consumer<Attack> next) {
        
        if (next != null) {
            // modifiers only
            
            // bow has a base damage of 2.0, and increases by 0.5 per level
            attack.setTag(Attack.DAMAGE, 2.0 + level * 0.5);

            // bow has a base crit change of 0.5, and increases by 0.1 per level
            attack.setTag(Attack.CRIT_CHANCE, 0.5 + level * 0.1);

            next.accept(attack);
            return;
        }
        
        // next == null means that we perform the attack
        Player user = attack.getTag(BOW_USER);
        if (user == null) {
            throw new IllegalStateException("BowWeapon attack called without a user set in the tags.");
        }
        
        // spawn single arrow
        var instance = user.getInstance();
        
        var arrow = new ArrowEntity(user);
        var playerScale = user.getAttribute(Attribute.SCALE).getValue();
        arrow.setInstance(instance, user.getPosition().add(0, user.getEyeHeight() * playerScale, 0));
        
        var velocity = user.getPosition().direction().mul(attack.getTag(VELOCITY));
        
        // add some randomness (it's nicer for multiple arrow shots)
        double delta = 12;
        velocity = velocity.add((Math.random() - 0.5) * delta, (Math.random() - 0.5) * delta, (Math.random() - 0.5) * delta);
        
        arrow.setVelocity(velocity);

        MinecraftServer.getGlobalEventHandler().addListener(EventListener.builder(ProjectileCollideWithEntityEvent.class)
                .handler(event -> {
                    AttackableMob target = (AttackableMob) event.getTarget();
                    target.applyAttack(DamageType.ARROW, attack);
                    arrow.remove();
                })
                .filter(event -> event.getTarget() instanceof AttackableMob && event.getEntity() == arrow)
                .expireCount(1)
                .expireWhen(ignored -> arrow.isRemoved())
                .build());
        
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }
}

class ArrowEntity extends Entity {
    private final Player shooter;

    public ArrowEntity(@Nullable Player shooter) {
        super(EntityType.IRON_GOLEM);
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

            ProjectileCollideWithBlockEvent event = new ProjectileCollideWithBlockEvent(this, posPos, block);
            EventDispatcher.call(event);
            if (!event.isCancelled()) {
                teleport(posPos);
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

            SweepResult result = new SweepResult(Double.MAX_VALUE, 0, 0, 0, null, 0, 0, 0, 0, 0, 0);
            if (entity.getBoundingBox().intersectBoxSwept(from.asVec(), movement, entity.getPosition(), boundingBox, result)) {
                final ProjectileCollideWithEntityEvent event = new ProjectileCollideWithEntityEvent(this, from, entity);
                EventDispatcher.call(event);
                if (!event.isCancelled()) {
                    return true;
                }
            }
        }

        return false;
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
