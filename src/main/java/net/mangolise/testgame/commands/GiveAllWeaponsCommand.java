package net.mangolise.testgame.commands;

import net.mangolise.gamesdk.features.commands.MangoliseCommand;
import net.mangolise.testgame.combat.weapons.Weapon;

public class GiveAllWeaponsCommand extends MangoliseCommand {

    public GiveAllWeaponsCommand() {
        super("giveallweapons");

        addPlayerSyntax(((player, context) -> {
            // Assuming there's a method to give all weapons to the player
            for (Weapon weapon : Weapon.weapons()) {
                player.getInventory().addItemStack(weapon.getItem());
            }
            player.sendMessage("All weapons have been given to you.");
        }));
    }

    @Override
    protected String getPermission() {
        return "game.command.giveallweapons";
    }
}
