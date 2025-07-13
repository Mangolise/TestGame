package net.mangolise.testgame.combat;

import net.minestom.server.entity.Entity;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.tag.Taggable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Random;
import java.util.function.Consumer;

public class Attack implements Taggable {

    public static final Tag<Entity> USER = Tag.Transient("testgame.attack.user");
    public static final Tag<Double> DAMAGE = Tag.Double("testgame.attack.damage").defaultValue(0.0);
    public static final Tag<Double> CRIT_CHANCE = Tag.Double("testgame.attack.crit_chance").defaultValue(0.0);
    public static final Tag<Double> COOLDOWN = Tag.Double("testgame.attack.cooldown").defaultValue(1.0);

    private final int seed;
    private final TagHandler tagHandler;
    
    private static final int SAMPLE_CRITS_SEED_OFFSET = 234563456;

    public Attack(int seed) {
        this(seed, TagHandler.newHandler());
    }

    private Attack(int seed, TagHandler handler) {
        this.seed = seed;
        this.tagHandler = handler;
    }

    /**
     * Samples the critical hit chance based on the tags.
     * This will always return the same number given the same seed and tags.
     * @return the number of critical hits that should be applied.
     */
    public int sampleCrits() {
        double critChance = tagHandler.getTag(CRIT_CHANCE);
        if (critChance <= 0.0) {
            return 0;
        }
        Random random = new Random(SAMPLE_CRITS_SEED_OFFSET + seed);
        
        int crits = 0;
        while (random.nextDouble() < critChance) {
            critChance -= 1.0;
            crits++;
        }
        return crits;
    }

    public interface Node {
        /**
         * This method is called when an attack is being processed.
         * @param tags the attack tags to modify
         * @param next the next node in the attack chain to call, or null only if this is the last node (a weapon, for example).
         */
        default void attack(Attack tags, @UnknownNullability Consumer<Attack> next) {
            // Default implementation does nothing, subclasses should override this to
            // modify the attack tags and/or call the next node as many times as wanted.
            next.accept(tags);
        }

        /**
         * The priority of this node in the attack chain.
         * 0.5 = weapons
         * 1.5 = stat modifiers
         * 2.5 = additive modifiers
         * 3.5 = multiplicative modifiers
         * 4.5 = spawning effects (e.g. shoot a bomb every time you attack)
         * e.t.c
         * @return the priority of this node
         */
        double priority();
        
        double PRIORITY_WEAPON = 0.5;
        double PRIORITY_STAT_MODIFIER = 1.5;
        double PRIORITY_ADDITIVE_MODIFIER = 2.5;
        double PRIORITY_MULTIPLICATIVE_MODIFIER = 3.5;
        double PRIORITY_EFFECT_SPAWNER = 4.5;
    }

    @Override
    public @NotNull TagHandler tagHandler() {
        return tagHandler;
    }

    public Attack copy(boolean mixSeed) {
        int newSeed = mixSeed ? staffordMix13(this.seed) : this.seed;
        return new Attack(newSeed, tagHandler.copy());
    }

    /* David Stafford's (http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html)
     * "Mix13" variant of the 64-bit finalizer in Austin Appleby's MurmurHash3 algorithm. */
    private static int staffordMix13(int z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D;
        z = (z ^ (z >>> 27)) * 0x94D049BB;
        return z ^ (z >>> 31);
    }
}
