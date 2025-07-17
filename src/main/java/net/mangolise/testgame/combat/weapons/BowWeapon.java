package net.mangolise.testgame.combat.weapons;

import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.events.ProjectileCollideEntityEvent;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.projectiles.VanillaProjectile;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.EventListener;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

import java.util.List;
import java.util.function.Consumer;

public record BowWeapon() implements Weapon {

    public static final Tag<Double> VELOCITY = Tag.Double("testgame.attack.bow.velocity").defaultValue(48.0);
    public static final Tag<Vec> AIM_DIRECTION = Tag.Structure("testgame.attack.bow.aim", Vec.class);
    
    @Override
    public void attack(Attack attack, Consumer<Attack> next) {
        attack.setTag(Attack.DAMAGE, 2.0);
        attack.setTag(Attack.CRIT_CHANCE, 0.5);

        attack.setTag(Attack.COOLDOWN, -100.0);

        next.accept(attack);
    }

    @Override
    public void doWeaponAttack(List<Attack> attacks) {
        for (Attack attack : attacks) {
            // next == null means that we perform the attack
            LivingEntity user = attack.getTag(Attack.USER);
            if (user == null) {
                throw new IllegalStateException("BowWeapon attack called without a user set in the tags.");
            }

            // spawn single arrow
            var instance = user.getInstance();

            var arrow = new VanillaProjectile(user, EntityType.ARROW);
            var playerScale = user.getAttribute(Attribute.SCALE).getValue();
            arrow.setInstance(instance, user.getPosition().add(0, user.getEyeHeight() * playerScale, 0));

            Vec velocity = attack.hasTag(AIM_DIRECTION) ?
                    attack.getTag(AIM_DIRECTION).normalize().mul(attack.getTag(VELOCITY)) :
                    user.getPosition().direction().mul(attack.getTag(VELOCITY));

            double inaccuracy = attacks.size() - 1.0; // more attacks, more inaccuracy
            velocity = velocity.add((Math.random() - 0.5) * inaccuracy, (Math.random() - 0.5) * inaccuracy, (Math.random() - 0.5) * inaccuracy);

            arrow.setVelocity(velocity);

            arrow.eventNode().addListener(EventListener.builder(ProjectileCollideEntityEvent.class)
                    .handler(event -> {
                        if (!(event.getTarget() instanceof AttackableMob mob && attack.canTarget(mob))) {
                            event.setCancelled(true);
                            return;
                        }

                        mob.applyAttack(DamageType.ARROW, attack);
                    })
                    .expireCount(1)
                    .expireWhen(ignored -> arrow.isRemoved())
                    .build()
            );
        }
    }

    @Override
    public ItemStack.Builder generateItem() {
        return ItemStack.builder(Material.BOW)
                .customName(ChatUtil.toComponent("&r&6&lLiterally Just A Bow"))
                .lore(ChatUtil.toComponent("&7You clearly cheated to get this."));
    }

    @Override
    public String getId() {
        return "bow";
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }
}
