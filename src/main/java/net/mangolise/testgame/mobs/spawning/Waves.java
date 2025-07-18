package net.mangolise.testgame.mobs.spawning;

import net.kyori.adventure.sound.Sound;
import net.mangolise.testgame.combat.mods.GenericMods;
import net.mangolise.testgame.combat.mods.Mod;
import net.mangolise.testgame.combat.mods.StaffWeaponMods;
import net.mangolise.testgame.combat.weapons.MaceWeapon;
import net.mangolise.testgame.combat.weapons.StaffWeapon;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.mobs.MeleeJockeyMob;
import net.mangolise.testgame.mobs.MeleeMob;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.sound.SoundEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class Waves {

    private static final Map<Integer, List<SpawnRecord>> WAVES = Map.of(
            0, List.of(
                    meleeMob(
                            3,
                            EntityType.BOGGED,
                            Sound.sound(SoundEvent.ENTITY_BOGGED_HURT, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_BOGGED_DEATH, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_BOGGED_STEP, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            20,
                            3
                    )
            ),
            1, List.of(
                    meleeMob(
                            6,
                            EntityType.BOGGED,
                            Sound.sound(SoundEvent.ENTITY_BOGGED_HURT, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_BOGGED_DEATH, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_BOGGED_STEP, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            20,
                            3
                    )
            ),
            2, List.of(
                    new SpawnRecord(() -> new MeleeJockeyMob(
                            EntityType.CAMEL,
                            Sound.sound(SoundEvent.ENTITY_CAMEL_HURT, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_CAMEL_DEATH, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_CAMEL_STEP, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            EntityType.CREEPER), 6, List.of()
                    )
            ),
            3, List.of(
                    new SpawnRecord(() -> new MeleeJockeyMob(
                            EntityType.CAMEL,
                            Sound.sound(SoundEvent.ENTITY_CAMEL_HURT, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_CAMEL_DEATH, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_CAMEL_STEP, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            EntityType.CREEPER), 6, List.of()
                    ),
                    meleeMob(
                            6,
                            EntityType.HUSK,
                            Sound.sound(SoundEvent.ENTITY_HUSK_HURT, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_HUSK_DEATH, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_HUSK_STEP, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            20,
                            3
                    )
            ),
            4, List.of(
                    new SpawnRecord(() -> new MeleeJockeyMob(
                            EntityType.CAMEL,
                            Sound.sound(SoundEvent.ENTITY_CAMEL_HURT, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_CAMEL_DEATH, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_CAMEL_STEP, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            EntityType.CREEPER), 6, List.of()
                    ),
                    meleeMob(
                            6,
                            EntityType.HUSK,
                            Sound.sound(SoundEvent.ENTITY_HUSK_HURT, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_HUSK_DEATH, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_HUSK_STEP, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            64,
                            5
                    ),
                    warden(3, 128, MaceWeapon.class)
            ),
            5, List.of(
                    new SpawnRecord(() -> new MeleeJockeyMob(
                            EntityType.MULE,
                            Sound.sound(SoundEvent.ENTITY_MULE_HURT, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_MULE_HURT, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_HORSE_STEP, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            EntityType.HUSK), 12, List.of(new GenericMods.Jacob(5)
                    )),
                    meleeMob(
                            6,
                            EntityType.HUSK,
                            Sound.sound(SoundEvent.ENTITY_HUSK_HURT, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_HUSK_DEATH, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_HUSK_STEP, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            64,
                            5
                    ),
                    warden(5, 256, MaceWeapon.class)
            ),
            6, List.of(
                    new SpawnRecord(() -> new MeleeJockeyMob(
                            EntityType.CHICKEN,
                            Sound.sound(SoundEvent.ENTITY_CHICKEN_HURT, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_CHICKEN_DEATH, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_CHICKEN_STEP, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                            EntityType.ZOMBIE).withWeapon(new StaffWeapon()), 16, List.of(new StaffWeaponMods.ArcChance(5)
                    )),
                    meleeMob(
                            6,
                            EntityType.HUSK,
                            Sound.sound(SoundEvent.ENTITY_HUSK_HURT, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_HUSK_DEATH, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            Sound.sound(SoundEvent.ENTITY_HUSK_STEP, Sound.Source.HOSTILE, 0.4f, 1.0f),
                            64,
                            5
                    ),
                    warden(10, 312, MaceWeapon.class)
            ),
            7, List.of(
                    warden(46, 412, MaceWeapon.class)
            )
    );

    private static List<SpawnRecord> getDefaultWave(int waveNumber) {
        return List.of(warden(30 + (waveNumber * 5), 128, MaceWeapon.class));
    }

    private static SpawnRecord meleeMob(int count, EntityType type, Sound hurtSound, Sound deathSound, Sound walkSound, double health, double dmg) {
        Supplier<AttackableMob> sup = () -> {
            MeleeMob mob = new MeleeMob(
                    type,
                    hurtSound,
                    deathSound,
                    walkSound,
                    dmg
            );
            mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
            mob.heal();
            return mob;
        };
        return new SpawnRecord(sup, count, List.of());
    }

    private static SpawnRecord warden(int count, float health, Class<? extends Weapon> weaponClass) {
        Supplier<AttackableMob> sup = () -> {
            MeleeMob mob = new MeleeMob(
                    EntityType.WARDEN,
                    Sound.sound(SoundEvent.ENTITY_WARDEN_HURT, Sound.Source.HOSTILE, 0.4f, 1.0f),
                    Sound.sound(SoundEvent.ENTITY_WARDEN_DEATH, Sound.Source.HOSTILE, 0.4f, 1.0f),
                    Sound.sound(SoundEvent.ENTITY_WARDEN_STEP, Sound.Source.HOSTILE, 0.4f, 1.0f),
                    20
            );
            mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
            mob.heal();

            if (weaponClass != null) {
                Weapon weapon = Weapon.weapon(weaponClass);
                if (weapon != null) {
                    mob.weapon = weapon;
                }
            }
            return mob;
        };

        return new SpawnRecord(sup, count, List.of());
    }

    public static List<SpawnRecord> getWave(int waveNumber) {
        List<SpawnRecord> wave = WAVES.get(waveNumber);
        if (wave != null) {
            return wave;
        }
        // If no specific wave is defined, return a default wave
        return getDefaultWave(waveNumber);
    }

    public record SpawnRecord(Supplier<AttackableMob> entity, int count, List<Mod> mods) { }
}
