package net.mangolise.testgame;

import net.kyori.adventure.sound.Sound;
import net.mangolise.gamesdk.Game;
import net.mangolise.gamesdk.instance.InstanceAnalysis;
import net.mangolise.gamesdk.log.Log;
import net.mangolise.gamesdk.util.ChatUtil;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FindTheButtonFeature implements Game.Feature<TestGame> {
    private static final Tag<List<BlockVec>> FOUND_BUTTONS_TAG = Tag.Transient("found_buttons");
    private static final int BUTTONS;

    static {
        Log.logger().info("Scanning for buttons...");
        Map<Point, Block> blocksMap = InstanceAnalysis.scanForBlocks(TestGame.worldLoader().world(), b -> b.name().endsWith("_button"));
        BUTTONS = blocksMap.size();
        Log.logger().info("Found {} buttons", BUTTONS);
    }

    @Override
    public void setup(Context<TestGame> context) {
        context.game().instance().eventNode().addListener(PlayerBlockInteractEvent.class, e -> {
            if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
                return;  // spectators cannot play the game
            }

            if (!e.getBlock().name().endsWith("_button")) {
                return;
            }

            Player player = e.getPlayer();
            if (!context.game().hasTag(FOUND_BUTTONS_TAG)) {
                context.game().setTag(FOUND_BUTTONS_TAG, new ArrayList<>());
            }

            List<BlockVec> foundButtons = context.game().getTag(FOUND_BUTTONS_TAG);

            if (foundButtons.size() == BUTTONS) {
                player.sendMessage(ChatUtil.toComponent("&cYou have already found all &7" + BUTTONS + "&c buttons!"));
                return;
            }

            if (foundButtons.stream().anyMatch(pos -> pos.sameBlock(e.getBlockPosition()))) {
                player.sendMessage(ChatUtil.toComponent("&cYou have already found this button &7(" + foundButtons.size() + "/" + BUTTONS + ")"));
                return;
            }

            foundButtons.add(e.getBlockPosition());
            context.game().instance().sendMessage(ChatUtil.toComponent("&aYou found a button &7(" + foundButtons.size() + "/" + BUTTONS + ")"));
            player.playSound(Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.BLOCK, 0.2f, 1f));
            context.game().instance().setBlock(e.getBlockPosition(), Block.AIR);

            if (foundButtons.size() == BUTTONS) {
                context.game().instance().sendMessage(ChatUtil.toComponent("&a&lYOU FOUND ALL THE BUTTONS!"));
            }
        });
    }
}
