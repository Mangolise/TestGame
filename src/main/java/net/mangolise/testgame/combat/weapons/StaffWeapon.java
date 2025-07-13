package net.mangolise.testgame.combat.weapons;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.tag.Tag;

import java.util.*;
import java.util.function.Consumer;

public record StaffWeapon(int level) implements Attack.Node {
    
    public static final Tag<Player> STAFF_USER = Tag.Transient("testgame.attack.staff.user");
    public static final Tag<AttackableMob> HIT_ENTITY = Tag.Transient("testgame.attack.staff.hit_entity");
    public static final Tag<Double> ARC_CHANCE = Tag.Double("testgame.attack.staff.arc_chance").defaultValue(0.5);
    
    @Override
    public void attack(Attack attack, Consumer<Attack> next) {
        
        if (next != null) {
            // modifiers only
            
            // staff has a staff damage of 6, and increases by 0.5 per level
            attack.setTag(Attack.DAMAGE, 6 + level * 0.5);

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
        AttackableMob originalEntity = attack.getTag(HIT_ENTITY);

        Set<UUID> chainedEntities = new HashSet<>();
        chainAttack(chainedEntities, originalEntity, attack, 1.0);
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }

    private void chainAttack(Set<UUID> chainedEntities, AttackableMob attackableMob, Attack attack, double depth) {
        if (attackableMob == null) {
            return;
        }

        Entity originEntity = attackableMob.getEntity();

        chainedEntities.add(originEntity.getUuid());
        attackableMob.applyAttack(DamageType.PLAYER_ATTACK, attack);

        Instance instance = originEntity.getInstance();

        Collection<Entity> entities = instance.getNearbyEntities(originEntity.getPosition(), 2.5);
        for (Entity entity : entities) {
            if (!(entity instanceof AttackableMob mob) || chainedEntities.contains(entity.getUuid()) || (Math.random() + attack.getTag(ARC_CHANCE)) / depth < 0.65) {
                continue;
            }

            Vec end = entity.getPosition().asVec().add(0, entity.getEyeHeight(), 0);
            Vec start = originEntity.getPosition().asVec().add(0, originEntity.getEyeHeight(), 0);

            Vec direction = end.sub(start);
            double distance = originEntity.getDistance(entity);

            for (double d = 0; d < distance; d += 0.1) {
                Pos currentPos = originEntity.getPosition().add(direction.mul(d));

                ParticlePacket particlePacket = new ParticlePacket(Particle.SCULK_CHARGE_POP, false, true, new Pos(currentPos.x(), currentPos.y() + entity.getEyeHeight(), currentPos.z()), new Pos(0, 0, 0), 0, 1);
                instance.sendGroupedPacket(particlePacket);

                ParticlePacket particlePacket2 = new ParticlePacket(Particle.BUBBLE_POP, false, true, new Pos(currentPos.x(), currentPos.y() + entity.getEyeHeight(), currentPos.z()), new Pos(0, 0, 0), 0, 1);
                instance.sendGroupedPacket(particlePacket2);
            }

            chainAttack(chainedEntities, mob, attack, depth + 1.0);
        }
    }
}

