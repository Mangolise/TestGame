package net.mangolise.testgame.combat;

import net.mangolise.testgame.combat.mods.Mod;
import net.minestom.server.entity.Entity;

import java.util.function.Consumer;

public sealed interface AttackSystem permits AttackSystemImpl {
    
    AttackSystem INSTANCE = new AttackSystemImpl();

    void use(Entity entity, Attack.Node weapon);
    void use(Entity entity, Attack.Node weapon, Consumer<Attack> tags);
    
    /**
     * Adds a mod to the entity.
     *
     * @param entity The entity to add the mod to.
     * @param mod The mod to add.
     * @return true if the mod was added successfully, false if the entity already has the mod.
     */
    boolean add(Entity entity, Mod mod);
    
    static void register() {
        switch (INSTANCE) {
            case AttackSystemImpl impl -> impl.init();
        }
    }
}
