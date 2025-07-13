package net.mangolise.testgame.combat.weapons;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.events.ProjectileCollideWithAnyEvent;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.projectiles.VanillaProjectile;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.EventListener;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.tag.Tag;

import java.util.Collection;
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

        var fireballEntity = new VanillaProjectile(user, attack, EntityType.FIREBALL);
        fireballEntity.setNoGravity(true);

        var playerScale = user.getAttribute(Attribute.SCALE).getValue();

        var point = user.getPosition().add(user.getPosition().direction().mul(attack.getTag(EXPLOSION_SIZE) / 2.0));
        fireballEntity.setInstance(instance, point.add(0, user.getEyeHeight() * playerScale, 0));
        
        var velocity = user.getPosition().direction().mul(attack.getTag(VELOCITY));

        fireballEntity.setVelocity(velocity);

        MinecraftServer.getGlobalEventHandler().addListener(EventListener.builder(ProjectileCollideWithAnyEvent.class)
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
}

