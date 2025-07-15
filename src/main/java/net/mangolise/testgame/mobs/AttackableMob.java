package net.mangolise.testgame.mobs;

import net.mangolise.testgame.combat.Attack;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.registry.RegistryKey;

public sealed interface AttackableMob permits HostileEntity, TestPlayer {
    void applyAttack(RegistryKey<DamageType> type, Attack attack);
    LivingEntity asEntity();
}
