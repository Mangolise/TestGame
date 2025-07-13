package net.mangolise.testgame.combat.weapons;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.events.ProjectileCollideEntityEvent;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.projectiles.VanillaProjectile;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.EventListener;
import net.minestom.server.tag.Tag;

import java.util.function.Consumer;

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
        
        var arrow = new VanillaProjectile(user, EntityType.ARROW);
        var playerScale = user.getAttribute(Attribute.SCALE).getValue();
        arrow.setInstance(instance, user.getPosition().add(0, user.getEyeHeight() * playerScale, 0));
        
        var velocity = user.getPosition().direction().mul(attack.getTag(VELOCITY));
        
        // add some randomness (it's nicer for multiple arrow shots)
        double delta = 12;
        velocity = velocity.add((Math.random() - 0.5) * delta, (Math.random() - 0.5) * delta, (Math.random() - 0.5) * delta);
        
        arrow.setVelocity(velocity);

        MinecraftServer.getGlobalEventHandler().addListener(EventListener.builder(ProjectileCollideEntityEvent.class)
                .handler(event -> {
                    AttackableMob target = (AttackableMob) event.getTarget();
                    target.applyAttack(DamageType.ARROW, attack);
                    arrow.remove();
                })
                .filter(event -> event.getTarget() instanceof AttackableMob && event.getEntity() == arrow)
                .expireCount(1)
                .expireWhen(ignored -> arrow.isRemoved())
                .build()
        );
        
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }
}
