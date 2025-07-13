package net.mangolise.testgame.combat.weapons;

import net.krystilize.pathable.Path;
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

        Vec playerPos = new Vec(user.getPosition().x(), user.getPosition().y() + user.getEyeHeight() * user.getAttribute(Attribute.SCALE).getValue(), user.getPosition().z());

        Entity entity = originalEntity.getEntity();
        var entityScale = entity instanceof LivingEntity living ? living.getAttribute(Attribute.SCALE).getBaseValue() : 1.0;
        Vec entityPos = new Vec(entity.getPosition().x(), entity.getPosition().y() + entity.getEyeHeight() * entityScale, entity.getPosition().z());

        createLightningLine(playerPos, entityPos, user.getInstance());

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

        Collection<Entity> entities = instance.getNearbyEntities(originEntity.getPosition(), 3);
        for (Entity entity : entities) {
            if (!(entity instanceof AttackableMob mob) || chainedEntities.contains(entity.getUuid()) || (Math.random() + attack.getTag(ARC_CHANCE)) / depth < 0.65) {
                continue;
            }

            var originEntityScale = originEntity instanceof LivingEntity living ? living.getAttribute(Attribute.SCALE).getBaseValue() : 1.0;
            var entityScale = entity instanceof LivingEntity living ? living.getAttribute(Attribute.SCALE).getBaseValue() : 1.0;

            Vec start = originEntity.getPosition().asVec().add(0, originEntity.getEyeHeight() * originEntityScale, 0);
            Vec end = entity.getPosition().asVec().add(0, entity.getEyeHeight() * entityScale, 0);

            createLightningLine(start, end, instance);

            chainAttack(chainedEntities, mob, attack, depth + 1.0);
        }
    }

    private void createLightningLine(Vec start, Vec end, Instance instance) {
        Path path = Path.line(start, end);
        for (Path.Context context : path.equalIterate(0.2)) {
            Pos offset = new Pos(0, 0, 0);

            ParticlePacket particlePacket = new ParticlePacket(Particle.GLOW, false, true, context.pos(), offset, 0, 1);
            instance.sendGroupedPacket(particlePacket);

            ParticlePacket particlePacket2 = new ParticlePacket(Particle.BUBBLE_POP, false, true, context.pos(), offset, 0, 1);
            instance.sendGroupedPacket(particlePacket2);

            ParticlePacket particlePacket3 = new ParticlePacket(Particle.BUBBLE, false, true, context.pos(), offset, 0, 1);
            instance.sendGroupedPacket(particlePacket3);
        }
    }
}

