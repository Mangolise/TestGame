package net.mangolise.testgame.combat.weapons;

import net.krystilize.pathable.Path;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.util.ThrottledScheduler;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public record StaffWeapon(int level) implements Weapon {
    
    public static final Tag<Player> STAFF_USER = Tag.Transient("testgame.attack.staff.user");
    public static final Tag<AttackableMob> HIT_ENTITY = Tag.Transient("testgame.attack.staff.hit_entity");
    public static final Tag<Double> ARC_CHANCE = Tag.Double("testgame.attack.staff.arc_chance").defaultValue(0.5);
    
    @Override
    public void attack(Attack attack, Consumer<Attack> next) {
        // staff has a staff damage of 6, and increases by 0.5 per level
        attack.setTag(Attack.DAMAGE, 6 + level * 0.5);

        // staff has a base crit change of 0.5, and increases by 0.1 per level
        attack.setTag(Attack.CRIT_CHANCE, 0.5 + level * 0.1);

        next.accept(attack);
    }

    @Override
    public void doWeaponAttack(List<Attack> attacks) {

        Set<UUID> chainedEntities = new HashSet<>();
        
        for (Attack attack : attacks) {
            // next == null means that we perform the attack
            Player user = attack.getTag(STAFF_USER);
            if (user == null) {
                throw new IllegalStateException("StaffWeapon attack called without a user set in the tags.");
            }

            // spawn spell
            AttackableMob originalEntity = attack.getTag(HIT_ENTITY);

            Vec playerPos = new Vec(user.getPosition().x(), user.getPosition().y() + user.getEyeHeight() * user.getAttribute(Attribute.SCALE).getValue(), user.getPosition().z());

            Entity entity = originalEntity.asEntity();
            var entityScale = entity instanceof LivingEntity living ? living.getAttribute(Attribute.SCALE).getBaseValue() : 1.0;
            Vec entityPos = new Vec(entity.getPosition().x(), entity.getPosition().y() + entity.getEyeHeight() * entityScale, entity.getPosition().z());

            Entity lightning = new Entity(EntityType.LIGHTNING_BOLT);
            lightning.setInstance(user.getInstance(), entityPos);

            user.getInstance().scheduler().scheduleTask(lightning::remove, TaskSchedule.millis(300), TaskSchedule.stop());

            user.playSound(Sound.sound(Key.key("minecraft:item.trident.thunder"), Sound.Source.NEUTRAL, 1.0f, 1f));
            user.playSound(Sound.sound(Key.key("minecraft:entity.lightning_bolt.impact"), Sound.Source.NEUTRAL, 1.0f, 1f));

            chainAttack(chainedEntities, originalEntity, attack, 1.0);
        }
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }

    private void chainAttack(Set<UUID> chainedEntities, AttackableMob attackableMob, Attack attack, double depth) {
        if (attackableMob == null) {
            return;
        }

        Entity originEntity = attackableMob.asEntity();
        
        if (originEntity.isRemoved() || (originEntity instanceof LivingEntity living && living.isDead())) {
            return;
        }

        chainedEntities.add(originEntity.getUuid());
        attackableMob.applyAttack(DamageType.PLAYER_ATTACK, attack);

        Instance instance = originEntity.getInstance();

        Collection<Entity> entities = instance.getNearbyEntities(originEntity.getPosition(), 3);
        for (Entity entity : entities) {
            if (!(entity instanceof AttackableMob mob) ||
                    chainedEntities.contains(entity.getUuid()) ||
                    (Math.random() + attack.getTag(ARC_CHANCE)) / depth < 0.65 ||
                    entity.isRemoved() ||
                    (entity instanceof LivingEntity living && (living.isDead() || living.isInvulnerable()))
            ) {
                continue;
            }

            var originEntityScale = originEntity instanceof LivingEntity living ? living.getAttribute(Attribute.SCALE).getBaseValue() : 1.0;
            var entityScale = entity instanceof LivingEntity living ? living.getAttribute(Attribute.SCALE).getBaseValue() : 1.0;

            instance.scheduler().scheduleTask(() -> {
                ThrottledScheduler.use(instance, "staff-weapon-chain-attack", 4, () -> {
                    Vec start = originEntity.getPosition().asVec().add(0, originEntity.getEyeHeight() * originEntityScale, 0);
                    Vec end = entity.getPosition().asVec().add(0, entity.getEyeHeight() * entityScale, 0);

                    createLightningLine(start, end, instance);
                    chainAttack(chainedEntities, mob, attack, depth + 1.0);
                });
            }, TaskSchedule.millis(100), TaskSchedule.stop());
        }
    }

    private void createLightningLine(Vec start, Vec end, Instance instance) {
        createDisplayEntity(start, end, instance, Block.GLASS, new Vec(0.2, 0.2));
        createDisplayEntity(start, end, instance, Block.WHITE_STAINED_GLASS, new Vec(0.1, 0.1));
        createDisplayEntity(start, end, instance, Block.SNOW_BLOCK, new Vec(0.05, 0.05));

        Path path = Path.line(start, end);
        for (Path.Context context : path.equalIterate(0.5)) {
            Pos offset = new Pos(0, 0, 0);

            ParticlePacket particlePacket = new ParticlePacket(Particle.GLOW, false, true, context.pos(), offset, 0, 1);
            instance.sendGroupedPacket(particlePacket);
        }
    }

    /**
     * @param scale width = x, height = z, y is unused.
     */
    private static void createDisplayEntity(Vec start, Vec end, Instance instance, Block type, Vec scale) {
        Entity displayEntity = new Entity(EntityType.BLOCK_DISPLAY);

        displayEntity.editEntityMeta(BlockDisplayMeta.class, meta -> {
            meta.setBlockState(type);
            meta.setScale(new Vec(scale.x(), scale.z(), start.distance(end)));
            meta.setHasNoGravity(true);
            meta.setTranslation(scale.withY(scale.z()).withZ(0).div(-2));
            meta.setBrightness(100, 0);
        });

        Vec delta = end.sub(start);
        Pos rotation = Pos.ZERO.withDirection(delta);

        Pos centeredSpawnPos = new Pos(start.x(), start.y(), start.z(), rotation.yaw(), rotation.pitch());
        displayEntity.setInstance(instance, centeredSpawnPos);

        instance.scheduler().scheduleTask(() -> ThrottledScheduler.use(
                instance, "staff-lightning-display", 10, displayEntity::remove),
                TaskSchedule.millis(300), TaskSchedule.stop()
        );
    }
}

