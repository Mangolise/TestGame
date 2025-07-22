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
import net.mangolise.testgame.mobs.ShooterMob;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.sound.SoundEvent;

import java.util.List;
import java.util.function.Supplier;

public final class Waves {

    private static final List<List<SpawnRecord>> WAVES;

    static {
        WAVES = List.of(
                List.of(  // Wave 1
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
                List.of(  // Wave 2
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
                List.of(  // Wave 3
                    new SpawnRecord(() -> new MeleeJockeyMob(
                        EntityType.CAMEL,
                        Sound.sound(SoundEvent.ENTITY_CAMEL_HURT, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                        Sound.sound(SoundEvent.ENTITY_CAMEL_DEATH, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                        Sound.sound(SoundEvent.ENTITY_CAMEL_STEP, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                        EntityType.CREEPER), 6, List.of()
                    )
                ),
                List.of(  // Wave 4
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
                List.of(  // Wave 5
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
                        warden(3, 64, MaceWeapon.class)
                ),
                List.of(  // Wave 6
                        new SpawnRecord(() -> new MeleeJockeyMob(
                                EntityType.MULE,
                                Sound.sound(SoundEvent.ENTITY_MULE_HURT, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                                Sound.sound(SoundEvent.ENTITY_MULE_HURT, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                                Sound.sound(SoundEvent.ENTITY_HORSE_STEP, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                                EntityType.HUSK), 6, List.of(new GenericMods.Jacob(5)
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
                        warden(1, 256, MaceWeapon.class),
                        rangedMob(
                                3,
                                EntityType.SKELETON,
                                1
                        )
                ),
                List.of(  // Wave 7
                        new SpawnRecord(() -> new MeleeJockeyMob(
                                EntityType.CHICKEN,
                                Sound.sound(SoundEvent.ENTITY_CHICKEN_HURT, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                                Sound.sound(SoundEvent.ENTITY_CHICKEN_DEATH, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                                Sound.sound(SoundEvent.ENTITY_CHICKEN_STEP, Sound.Source.NEUTRAL, 0.4f, 1.0f),
                                EntityType.ZOMBIE).withWeapon(new StaffWeapon()), 12, List.of(new StaffWeaponMods.ArcChance(5)
                        )),
                        meleeMob(
                                6,
                                EntityType.HUSK,
                                Sound.sound(SoundEvent.ENTITY_HUSK_HURT, Sound.Source.HOSTILE, 0.4f, 1.0f),
                                Sound.sound(SoundEvent.ENTITY_HUSK_DEATH, Sound.Source.HOSTILE, 0.4f, 1.0f),
                                Sound.sound(SoundEvent.ENTITY_HUSK_STEP, Sound.Source.HOSTILE, 0.4f, 1.0f),
                                32,
                                5
                        ),
                        warden(2, 256, MaceWeapon.class),
                        rangedMob(
                                4,
                                EntityType.SKELETON,
                                12
                        )
                ),
                List.of(  // Wave 8
                        warden(16, 312, MaceWeapon.class),
                        rangedMob(
                                3,
                                EntityType.SKELETON,
                                12
                        ),
                        rangedMob(
                                6,
                                EntityType.ZOMBIFIED_PIGLIN,
                                16
                        )
                ),
                List.of(  // Wave 9
                        meleeMob(
                                32,
                                EntityType.HUSK,
                                Sound.sound(SoundEvent.ENTITY_HUSK_HURT, Sound.Source.HOSTILE, 0.4f, 1.0f),
                                Sound.sound(SoundEvent.ENTITY_HUSK_DEATH, Sound.Source.HOSTILE, 0.4f, 1.0f),
                                Sound.sound(SoundEvent.ENTITY_HUSK_STEP, Sound.Source.HOSTILE, 0.4f, 1.0f),
                                32,
                                5
                        ),
                        warden(3, 448, MaceWeapon.class),
                        rangedMob(
                                6,
                                EntityType.SKELETON,
                                12
                        )
                ),
                List.of(  // Wave 10
                        meleeMob(
                                32,
                                EntityType.HUSK,
                                Sound.sound(SoundEvent.ENTITY_HUSK_HURT, Sound.Source.HOSTILE, 0.4f, 1.0f),
                                Sound.sound(SoundEvent.ENTITY_HUSK_DEATH, Sound.Source.HOSTILE, 0.4f, 1.0f),
                                Sound.sound(SoundEvent.ENTITY_HUSK_STEP, Sound.Source.HOSTILE, 0.4f, 1.0f),
                                64,
                                5
                        ),
                        warden(12, 512, MaceWeapon.class),
                        rangedMob(
                                6,
                                EntityType.SKELETON,
                                12
                        )
                )
        );
    }

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

    private static SpawnRecord rangedMob(int count, EntityType type, double health) {
        Supplier<AttackableMob> sup = () -> {
            ShooterMob mob = new ShooterMob(
                    type,
                    Sound.sound(SoundEvent.ENTITY_SKELETON_HORSE_HURT, Sound.Source.HOSTILE, 0.4f, 1.0f),
                    Sound.sound(SoundEvent.ENTITY_SKELETON_DEATH, Sound.Source.HOSTILE, 0.4f, 1.0f),
                    Sound.sound(SoundEvent.ENTITY_SKELETON_STEP, Sound.Source.HOSTILE, 0.4f, 1.0f)
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
        if (waveNumber >= WAVES.size()) {
            return getDefaultWave(waveNumber);
        }

        return WAVES.get(waveNumber);
    }

    public record SpawnRecord(Supplier<AttackableMob> entity, int count, List<Mod> mods) { }
}
