package net.mangolise.testgame.combat.weapons;

import net.mangolise.testgame.combat.Attack;
import net.minestom.server.item.ItemStack;

import java.util.List;

public interface Weapon extends Attack.Node {
    void doWeaponAttack(List<Attack> attacks);
    ItemStack getItem();
}
