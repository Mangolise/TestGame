package net.mangolise.testgame.commands;

import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.mods.Mod;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class GiveModsCommand extends Command {
    public GiveModsCommand() {
        super("givemods");
        
        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return;
            }

            AttackSystem.INSTANCE.add(player, new Mod.DoubleAttack(3));
            AttackSystem.INSTANCE.add(player, new Mod.TripleAttack(3));
            AttackSystem.INSTANCE.add(player, new Mod.QuadAttack(3));
            AttackSystem.INSTANCE.add(player, new Mod.BowVelocity(1));
            AttackSystem.INSTANCE.add(player, new Mod.StaffArcChance(1));
//            AttackSystem.INSTANCE.add(player, new Mod.CritToDamage(3));
        });
    }
}
