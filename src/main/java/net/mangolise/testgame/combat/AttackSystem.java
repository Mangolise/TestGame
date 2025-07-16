package net.mangolise.testgame.combat;

import net.mangolise.testgame.combat.mods.Mod;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface AttackSystem permits AttackSystemImpl {

    Tag<AttackSystem> ATTACK_SYSTEM_TAG = Tag.Transient("testgame.attacksystem.instance");

    static AttackSystem instance(Instance instance) {
        return instance.getTag(ATTACK_SYSTEM_TAG);
    }

    Map<Class<? extends Mod>, Mod> getModifiers(Entity entity);

    void upgradeMod(Entity entity, Class<? extends Mod> mod, Function<Mod, Integer> levelSupplier);

    void use(Entity entity, Weapon weapon);
    void use(Entity entity, Weapon weapon, Consumer<Attack> tags);
    
    /**z
     * Adds a mod to the entity.
     *
     * @param entity The entity to add the mod to.
     * @param mod The mod to add.
     * @return true if the mod was added successfully, false if the entity already has the mod.
     */
    boolean add(Entity entity, Mod mod);
    
    static void register(Instance instance) {
        AttackSystemImpl attackSystem = new AttackSystemImpl(instance);
        attackSystem.init();
        instance.setTag(ATTACK_SYSTEM_TAG, attackSystem);
    }
}
