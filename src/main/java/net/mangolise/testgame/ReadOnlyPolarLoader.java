package net.mangolise.testgame;

import net.hollowcube.polar.PolarLoader;
import net.minestom.server.instance.Chunk;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * A PolarLoader that prevents saving chunks when they are unloaded.
 * This is useful for read-only instances where you do not want to modify the world.
 */
public class ReadOnlyPolarLoader extends PolarLoader {

    public ReadOnlyPolarLoader(@NotNull InputStream inputStream) throws IOException {
        super(inputStream);
    }

    @Override
    public void unloadChunk(Chunk chunk) {
        // Prevent polar from saving the chunks when they are unloaded
    }
}
