package net.mangolise.testgame.mobs;

import net.mangolise.testgame.combat.Attack;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.registry.RegistryKey;

public interface AttackableMob {
    void applyAttack(RegistryKey<DamageType> type, Attack attack);
    Entity getEntity();
}
