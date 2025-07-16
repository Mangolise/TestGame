package net.mangolise.testgame.combat.weapons;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.TooltipDisplay;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public record MaceWeapon() implements Weapon {
    public static final Tag<Boolean> IS_LAUNCH_ATTACK = Tag.Boolean("testgame.attack.mace.is_launch_attack");
    public static final Tag<Double> SLAM_RADIUS = Tag.Double("testgame.attack.mace.slam_radius").defaultValue(3.5);
    public static final Tag<Double> SLAM_INTENSITY = Tag.Double("testgame.attack.mace.slam_intensity").defaultValue(20.0);
    public static final Tag<Double> SWING_RADIUS = Tag.Double("testgame.attack.mace.swing_radius").defaultValue(2.0);

    @Override
    public void attack(Attack attack, Consumer<Attack> next) {
        if (attack.getTag(IS_LAUNCH_ATTACK)) {
            attack.setTag(Attack.DAMAGE, 8.0);
        } else {
            attack.setTag(Attack.DAMAGE, 12.0);
        }

        attack.setTag(Attack.COOLDOWN, 1.5);
        attack.setTag(Attack.CRIT_CHANCE, 0.1);

        next.accept(attack);
    }

    @Override
    public void doWeaponAttack(List<Attack> attacks) {
        for (Attack attack : attacks) {
            LivingEntity user = attack.getTag(Attack.USER);
            if (user == null) {
                throw new IllegalStateException("MaceWeapon attack called without a user set in the tags.");
            }

            Instance instance = user.getInstance();

            if (attack.getTag(IS_LAUNCH_ATTACK)) {
                Collection<Entity> entities = instance.getNearbyEntities(user.getPosition(), attack.getTag(SLAM_RADIUS));

                displayBlockVisuals(instance, user.getPosition().asVec());
                instance.sendGroupedPacket(new ParticlePacket(Particle.CAMPFIRE_COSY_SMOKE, false, true, user.getPosition(), new Vec(0, 0, 0), 0.1f, 100));
                instance.playSound(Sound.sound(Key.key("minecraft:block.anvil.land"), Sound.Source.PLAYER, 0.5f, 0.5f), user.getPosition());
                instance.playSound(Sound.sound(Key.key("minecraft:entity.dragon_fireball.explode"), Sound.Source.PLAYER, 0.8f, 1.0f), user.getPosition());

                for (Entity entity : entities) {
                    if (!(entity instanceof AttackableMob mob && attack.canTarget(mob))) {
                        continue;
                    }

                    Entity target = mob.asEntity();

                    Vec newVel = target.getVelocity().withY(target.getVelocity().y() + attack.getTag(SLAM_INTENSITY));
                    target.setVelocity(newVel);
                    mob.applyAttack(DamageType.PLAYER_ATTACK, attack);
                }
            }

            Entity target = attack.getTag(Attack.TARGET);
            if (!(target instanceof AttackableMob mob && attack.canTarget(mob))) {
                continue;
            }

            Collection<Entity> entities = instance.getNearbyEntities(target.getPosition(), attack.getTag(SWING_RADIUS));
            for (Entity entity : entities) {
                if (!(entity instanceof AttackableMob e) || entity instanceof Player) {
                    continue;
                }

                entity.setVelocity(entity.getVelocity().add(user.getPosition().direction().mul(40)));
                e.applyAttack(DamageType.PLAYER_ATTACK, attack);
            }
        }
    }

    private void displayBlockVisuals(Instance instance, Vec position) {
        for (int x = -5; x < 5; x++) {
            for (int z = -5; z < 5; z++) {
                double dist = new Vec(x, z).length();
                if (dist > 5) {
                    continue;
                }

                Vec pos = new Vec(x, 5, z).add(position);
                Block block = instance.getBlock(pos);

                for (int i = 0; !block.isSolid() && i < 10; i++) {
                    pos = pos.add(0, -1, 0);
                    block = instance.getBlock(pos);
                }

                if (!block.isSolid()) {
                    continue;
                }

                Block finalBlock = block;

                Entity entity = new Entity(EntityType.BLOCK_DISPLAY);
                entity.editEntityMeta(BlockDisplayMeta.class, meta -> {
                    meta.setBlockState(finalBlock);
                    meta.setHasNoGravity(true);
                    meta.setPosRotInterpolationDuration(40);
                    meta.setBrightness(15, 15);
                });

                Vec delta = pos.sub(position);
                Pos rotation = Pos.ZERO.withDirection(delta);
                entity.setInstance(instance, pos.asPosition().add(0, 1.0, 0).withPitch(45).withYaw(rotation.yaw()));
                entity.teleport(pos.asPosition().withY(pos.y() - 2.0));

                MinecraftServer.getSchedulerManager().scheduleTask(entity::remove, TaskSchedule.tick(40), TaskSchedule.stop());
            }
        }
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.of(Material.MACE)
                .withTag(Weapon.WEAPON_TAG, getId())
                .with(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .withCustomName(ChatUtil.toComponent("&r&7&c&lMace"))
                .with(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, Set.of(DataComponents.ATTRIBUTE_MODIFIERS)))
                .withLore(
                        ChatUtil.toComponent("&7A heavy weapon that can launch enemies into the air."),
                        ChatUtil.toComponent("&7Slow but powerful, deal damage to enemies close by.")
                );
    }

    @Override
    public String getId() {
        return "mace";
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }
}
