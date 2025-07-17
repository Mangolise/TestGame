package net.mangolise.testgame.combat.weapons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.Tag;

import java.util.List;

public record DirectDamageWeapon() implements Weapon {
    public static final Tag<RegistryKey<DamageType>> DAMAGE_TYPE_TAG = Tag.<RegistryKey<DamageType>>Transient("testgame.attack.directdamage.damage_type").defaultValue(() -> DamageType.MOB_ATTACK);

    @Override
    public void doWeaponAttack(List<Attack> attacks) {
        for (Attack attack : attacks) {
            Entity target = attack.getTag(Attack.TARGET);
            if (!(target instanceof AttackableMob targetMob)) {
                throw new IllegalStateException("DirectDamageWeapon attack called without a AttackableMob target set in the tags.");
            }

            targetMob.applyAttack(attack.getTag(DAMAGE_TYPE_TAG), attack);
        }
    }

    @Override
    public ItemStack.Builder generateItem() {
        return ItemStack.builder(Material.WAXED_WEATHERED_CUT_COPPER_SLAB)
                .customName(Component.text("Hi there", TextColor.color(12, 12, 12)))
                .lore(ChatUtil.toComponent("&7This is a dev tool noob!"));
    }

    @Override
    public String getId() {
        return "direct_damage";
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }
}
