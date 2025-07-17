package net.mangolise.testgame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.gamesdk.Game;
import net.mangolise.gamesdk.util.ChatUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerChatEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

public class FancyChatFeature implements Game.Feature<Game> {
    private static final Map<String, Function<Map.Entry<String, String>, Component>> CUSTOM_FORMATS = Map.of(
            "iVec4", (entry) -> Component.text("").append(Component.text("[").color(NamedTextColor.GRAY).append(Component.text("uniform").color(TextColor.color(232, 43, 242))).append(Component.text("]").color(NamedTextColor.GRAY))).append(Component.text(" iVec4").color(TextColor.color(NamedTextColor.DARK_AQUA))).append().append(ChatUtil.toComponent(": " + entry.getValue()).color(NamedTextColor.WHITE)).clickEvent(ClickEvent.openUrl("https://github.com/EclipsedMango")),
            "CoPokBl", (entry) -> ChatUtil.toComponent("&7[&9Creator&7] &9CoPokBl").clickEvent(ClickEvent.openUrl("https://github.com/CoPokBl")).append(ChatUtil.toComponent("&f: " + entry.getValue())),
            "Calcilore", (entry) -> Component.text("").color(TextColor.color(224, 139, 40)).append(Component.text("[FIRE]").decorate(TextDecoration.BOLD)).append(Component.text(" Calcilore")).append().append(Component.text(": ", NamedTextColor.WHITE).append(ChatUtil.toComponent(entry.getValue())))
    );
    private static final Function<Map.Entry<String, String>, Component> DEFAULT_CREATOR_FORMAT = entry ->
            ChatUtil.toComponent("&7[&cCreator&7] &r&c%s&r&f: %s".formatted(entry.getKey(), entry.getValue()));
    private static final Function<Map.Entry<String, String>, Component> REGULAR_FORMAT = entry ->
            ChatUtil.toComponent("&7%s&r&f: %s".formatted(entry.getKey(), entry.getValue()));
    private static final Function<Map.Entry<String, String>, Component> MINESTOM_FORMAT = entry ->
            ChatUtil.toComponent("&7[&bMinestom&7] &r&b%s&r&f: %s".formatted(entry.getKey(), entry.getValue()));

    @Override
    public void setup(Context context) {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerChatEvent.class, e -> {
            Function<Map.Entry<String, String>, Component> format = REGULAR_FORMAT;
            Component hoverText = null;
            if (Arrays.stream(GameConstants.CREATORS).anyMatch(c -> c.equalsIgnoreCase(e.getPlayer().getUsername()))) {
                format = CUSTOM_FORMATS.getOrDefault(e.getPlayer().getUsername(), DEFAULT_CREATOR_FORMAT);
                hoverText = ChatUtil.toComponent("&7This user created this game.");
            } else if (Arrays.stream(GameConstants.MINESTOM_OFFICIALS).anyMatch(c -> c.equalsIgnoreCase(e.getPlayer().getUsername()))) {
                format = MINESTOM_FORMAT;
                hoverText = ChatUtil.toComponent("&7This user is a Minestom official.");
            } else if (CUSTOM_FORMATS.containsKey(e.getPlayer().getUsername())) {
                format = CUSTOM_FORMATS.get(e.getPlayer().getUsername());
            }

            e.setFormattedMessage(format.apply(new Map.Entry<>() {
                @Override
                public String getKey() {
                    return e.getPlayer().getUsername();
                }

                @Override
                public String getValue() {
                    return e.getRawMessage();
                }

                @Override
                public String setValue(String s) {
                    throw new UnsupportedOperationException("Not supported.");
                }
            }).hoverEvent(hoverText == null ? null : HoverEvent.showText(hoverText)));
        });
    }
}
