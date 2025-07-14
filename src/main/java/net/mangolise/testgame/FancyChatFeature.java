package net.mangolise.testgame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.mangolise.gamesdk.Game;
import net.mangolise.gamesdk.util.ChatUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerChatEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

public class FancyChatFeature implements Game.Feature<Game> {
    private static final Map<String, Function<Map.Entry<String, String>, Component>> CREATOR_FORMATS = Map.of(
            "iVec4", (entry) -> ChatUtil.toComponent("&c&l[CREATOR] &r&c" + entry.getKey() + "&r&f: " + entry.getValue()),
            "Calcilore", (entry) -> Component.text("orange").color(TextColor.color(224, 139, 40))
    );
    private static final Function<Map.Entry<String, String>, Component> DEFAULT_CREATOR_FORMAT = entry ->
            ChatUtil.toComponent("&c&l[CREATOR] &r&c%s&r&f: %s".formatted(entry.getKey(), entry.getValue()));
    private static final Function<Map.Entry<String, String>, Component> REGULAR_FORMAT = entry ->
            ChatUtil.toComponent("&7%s&r&f: %s".formatted(entry.getKey(), entry.getValue()));
    private static final Function<Map.Entry<String, String>, Component> MINESTOM_FORMAT = entry ->
            ChatUtil.toComponent("&b&l[MINESTOM] &r&b%s&r&f: %s".formatted(entry.getKey(), entry.getValue()));

    @Override
    public void setup(Context context) {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerChatEvent.class, e -> {
            Function<Map.Entry<String, String>, Component> format = REGULAR_FORMAT;
            if (Arrays.stream(GameConstants.CREATORS).anyMatch(c -> c.equalsIgnoreCase(e.getPlayer().getUsername()))) {
                format = CREATOR_FORMATS.getOrDefault(e.getPlayer().getUsername(), DEFAULT_CREATOR_FORMAT);
            } else if (Arrays.stream(GameConstants.MINESTOM_OFFICIALS).anyMatch(c -> c.equalsIgnoreCase(e.getPlayer().getUsername()))) {
                format = MINESTOM_FORMAT;
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
            }));
        });
    }
}
