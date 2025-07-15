package net.mangolise.testgame.mobs;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.mangolise.testgame.combat.Attack;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;

public final class TestPlayer extends Player implements AttackableMob {
    public TestPlayer(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    @Override
    public void applyAttack(RegistryKey<DamageType> type, Attack attack) {
        // Apply the attack to the zombie, e.g., reduce health
        var user = attack.getTag(Attack.USER);
        double damage = attack.getTag(Attack.DAMAGE);

        // convert crit into additional damage
        double critMultiplier = 1.0 + attack.sampleCrits();

        // when the crit multiplier is above 1.9, ping a ding sound to the user if they are a player
        if (critMultiplier > 1.9 && user instanceof Player player) {
            player.playSound(Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.HOSTILE, 0.01f, 1f));
        }

        this.damage(new Damage(type, null, user, null, (float) (damage * critMultiplier)));
    }

    @Override
    public LivingEntity asEntity() {
        return this;
    }
}
