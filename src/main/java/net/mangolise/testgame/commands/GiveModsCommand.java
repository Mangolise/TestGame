package net.mangolise.testgame.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.mangolise.gamesdk.features.commands.MangoliseCommand;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.mods.Mod;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GiveModsCommand extends MangoliseCommand {
    
    private final List<String> modNames = Mod.values()
            .stream()
            .map(GiveModsCommand::nameFromMod)
            .toList();
    
    public GiveModsCommand() {
        super("givemods");
        
        addPlayerSyntax(this::usageAll, ArgumentType.Literal("all"), ArgumentType.Integer("level").setDefaultValue(3));
        addPlayerSyntax(this::usageModName, ArgumentType.Word("mod_name").from(modNames.toArray(String[]::new)), ArgumentType.Integer("level").setDefaultValue(3));

        addPlayerSyntax((sender, context) ->
                sender.sendMessage(Component.text("Usage: /givemods <all|mod_name> [level]")
                .append(Component.newline())
                .append(Component.text("Available mods: " + String.join(", ", modNames)))));
    }

    private void usageModName(Player player, CommandContext context) {
        String modName = context.get("mod_name");
        Mod.Factory factory = modFromName(modName);
        
        if (factory == null) {
            player.sendMessage(Component.text("Unknown mod: " + modName));
            return;
        }
        
        int level = context.get("level");
        Mod mod = factory.create(level);
        
        giveMods(player, List.of(mod));
    }

    private void usageAll(Player player, CommandContext context) {
        int level = context.get("level");
        
        List<Mod> mods = Mod.values().stream()
                .map(factory -> factory.create(level))
                .toList();
        giveMods(player, mods);
    }
    
    private void giveMods(@NotNull Player player, List<Mod> mods) {
        for (Mod mod : mods) {
            if (AttackSystem.instance(player.getInstance()).add(player, mod)) {
                player.sendMessage(Component.text()
                        .append(Component.text("Gave you mod: "))
                        .append(mod.name()));
            } else {
                AttackSystem.instance(player.getInstance()).upgradeMod(player, mod.getClass(), m -> mod.level());
                player.sendMessage(Component.text()
                        .append(Component.text("Upgraded mod: "))
                        .append(mod.name()));
            }
        }
    }
    
    private static String nameFromMod(Mod.Factory factory) {
        return ((TextComponent) factory.create(0).name()).content();
    }
    
    private static @Nullable Mod.Factory modFromName(String name) {
        return Mod.values().stream()
                .filter(factory -> nameFromMod(factory).equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
    }

    @Override
    protected String getPermission() {
        return "game.command.givemods";
    }
}
