package net.mangolise.testgame.combat.weapons;

import net.krystilize.pathable.Path;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.util.ThrottledScheduler;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.AttributeList;
import net.minestom.server.item.component.TooltipDisplay;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public record StaffWeapon() implements Weapon {
    public static final Tag<Double> ARC_CHANCE = Tag.Double("testgame.attack.staff.arc_chance").defaultValue(0.75);
    public static final Tag<Double> ARC_RADIUS = Tag.Double("testgame.attack.staff.arc_radius").defaultValue(1.5);

    @Override
    public void attack(Attack attack, Consumer<Attack> next) {
        attack.setTag(Attack.DAMAGE, 6.0);
        attack.setTag(Attack.CRIT_CHANCE, 0.5);

        next.accept(attack);
    }

    @Override
    public void doWeaponAttack(List<Attack> attacks) {

        Set<UUID> chainedEntities = ConcurrentHashMap.newKeySet();
        
        for (Attack attack : attacks) {
            LivingEntity user = attack.getTag(Attack.USER);
            if (user == null) {
                throw new IllegalStateException("StaffWeapon attack called without a user set in the tags.");
            }

            // spawn spell
            Entity target = attack.getTag(Attack.TARGET);
            if (!(target instanceof AttackableMob originalEntity && attack.canTarget(originalEntity))) {
                return;
            }

            LivingEntity entity = originalEntity.asEntity();
            double entityScale = entity.getAttribute(Attribute.SCALE).getValue();
            Vec entityPos = new Vec(entity.getPosition().x(), entity.getPosition().y() + entity.getEyeHeight() * entityScale, entity.getPosition().z());

            Entity lightning = new Entity(EntityType.LIGHTNING_BOLT);
            lightning.setInstance(user.getInstance(), entityPos);

            user.getInstance().scheduler().scheduleTask(lightning::remove, TaskSchedule.millis(300), TaskSchedule.stop());

            user.getInstance().playSound(Sound.sound(Key.key("minecraft:item.trident.thunder"), Sound.Source.NEUTRAL, 0.1f, 1f), entityPos);

            chainAttack(chainedEntities, originalEntity, attack, 1);
        }
    }

    @Override
    public ItemStack.Builder generateItem() {
        return ItemStack.builder(Material.BREEZE_ROD)
                .set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .set(DataComponents.ATTRIBUTE_MODIFIERS, new AttributeList(new AttributeList.Modifier(Attribute.ENTITY_INTERACTION_RANGE, new AttributeModifier("testgame.weapons.staff.modifier", 1024.0, AttributeOperation.ADD_VALUE), EquipmentSlotGroup.HAND)))
                .set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, Set.of(DataComponents.ATTRIBUTE_MODIFIERS)))
                .customName(ChatUtil.toComponent("&r&b&lStaff"))
                .lore(
                        ChatUtil.toComponent("&7A magical staff that can chain lightning attacks."),
                        ChatUtil.toComponent("&7Hit groups of enemies to cause a chain reaction "),
                        ChatUtil.toComponent("&7of forking lightning bolts!")
                )
                .set(DataComponents.ITEM_MODEL, "minecraft:staff");
    }

    @Override
    public String getId() {
        return "staff";
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }

    private void chainAttack(Set<UUID> chainedEntities, AttackableMob attackableMob, Attack attack, int depth) {
        if (attackableMob == null) {
            return;
        }

        LivingEntity originEntity = attackableMob.asEntity();

        if (originEntity.isRemoved() || originEntity.isDead()) {
            return;
        }

        chainedEntities.add(originEntity.getUuid());
        attackableMob.applyAttack(DamageType.PLAYER_ATTACK, attack);

        Instance instance = originEntity.getInstance();

        Collection<Entity> entities = instance.getNearbyEntities(originEntity.getPosition(), attack.getTag(ARC_RADIUS));
        for (Entity entity : entities) {
            if (!(entity instanceof AttackableMob mob && attack.canTarget(mob)) ||
                    chainedEntities.contains(entity.getUuid()) ||
                    attack.getTag(ARC_CHANCE) / ((float)depth) < Math.random() ||
                    entity.isRemoved() ||
                    (mob.asEntity().isDead() || mob.asEntity().isInvulnerable())
            ) {
                continue;
            }

            var originEntityScale = originEntity.getAttribute(Attribute.SCALE).getValue();
            var entityScale = ((LivingEntity)entity).getAttribute(Attribute.SCALE).getValue();

            instance.scheduler().scheduleTask(() -> {
                ThrottledScheduler.use(instance, "staff-weapon-chain-attack", 4, () -> {
                    Vec start = originEntity.getPosition().asVec().add(0, originEntity.getEyeHeight() * originEntityScale, 0);
                    Vec end = entity.getPosition().asVec().add(0, entity.getEyeHeight() * entityScale, 0);

                    createLightningLine(start, end, instance);
                    Attack arcAttack = attack.copy(false);
                    arcAttack.updateTag(StaffWeapon.ARC_CHANCE, arc -> arc * 0.75);
                    chainAttack(chainedEntities, mob, attack, depth + 1);
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
        });

        Vec delta = end.sub(start);
        if (delta.length() < 0.1) {
            return;
        }

        Pos rotation = Pos.ZERO.withDirection(delta);

        Pos centeredSpawnPos = new Pos(start.x(), start.y(), start.z(), rotation.yaw(), rotation.pitch());
        displayEntity.setInstance(instance, centeredSpawnPos);

        instance.scheduler().scheduleTask(() -> ThrottledScheduler.use(
                instance, "staff-lightning-display", 10, displayEntity::remove),
                TaskSchedule.millis(300), TaskSchedule.stop()
        );
    }
}
