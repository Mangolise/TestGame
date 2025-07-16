package net.mangolise.testgame.combat.weapons;

import net.mangolise.testgame.combat.Attack;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Weapon extends Attack.Node {
    Tag<String> WEAPON_TAG = Tag.String("game.weapon");

    void doWeaponAttack(List<Attack> attacks);
    ItemStack getItem();
    String getId();

    static List<Weapon> weapons() {
        return List.of(
            new BowWeapon(),
            new CannonBallBallWeapon(),
            new MaceWeapon(),
            new SnakeWeapon(),
            new StaffWeapon(),
            new DirectDamageWeapon()
        );
    }

    static Map<String, Weapon> weaponsById() {
        Map<String, Weapon> map = new HashMap<>();
        for (Weapon weapon : weapons()) {
            map.put(weapon.getId(), weapon);
        }
        return map;
    }

    static Weapon weapon(String id) {
        return weaponsById().get(id);
    }

    static Weapon weapon(Class<? extends Weapon> weaponClass) {
        for (Weapon weapon : weapons()) {
            if (weapon.getClass().equals(weaponClass)) {
                return weapon;
            }
        }
        return null;
    }
}
