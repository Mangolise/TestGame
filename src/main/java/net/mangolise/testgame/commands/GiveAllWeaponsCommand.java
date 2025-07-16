package net.mangolise.testgame.commands;

import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class GiveAllWeaponsCommand extends Command {

    public GiveAllWeaponsCommand() {
        super("giveallweapons");

        setDefaultExecutor(((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return;
            }

            // Assuming there's a method to give all weapons to the player
            for (Weapon weapon : Weapon.weapons()) {
                player.getInventory().addItemStack(weapon.getItem());
            }
            player.sendMessage("All weapons have been given to you.");
        }));
    }
}
