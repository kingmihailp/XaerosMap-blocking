package com.github.xaeroblock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which non-OP players have been explicitly granted map access.
 * OPs always have access regardless of this list.
 * The list is persisted to disk between server restarts.
 */
public final class MapAccessManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapAccessManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type UUID_SET_TYPE = new TypeToken<Set<String>>() {}.getType();

    private static Path saveFile = null;
    private static final Set<UUID> allowedPlayers = new HashSet<>();

    private MapAccessManager() {}

    // -----------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------

    public static void init(Path configDir) {
        saveFile = configDir.resolve("xaeroblock_allowed.json");
        load();
    }

    private static void load() {
        if (saveFile == null || !Files.exists(saveFile)) return;
        try (Reader r = Files.newBufferedReader(saveFile)) {
            Set<String> raw = GSON.fromJson(r, UUID_SET_TYPE);
            allowedPlayers.clear();
            if (raw != null) {
                raw.forEach(s -> {
                    try { allowedPlayers.add(UUID.fromString(s)); }
                    catch (IllegalArgumentException ignored) {}
                });
            }
            LOGGER.info("[XaeroBlock] Loaded {} explicitly allowed player(s).", allowedPlayers.size());
        } catch (IOException e) {
            LOGGER.error("[XaeroBlock] Failed to load allowed-players file: {}", e.getMessage());
        }
    }

    public static void save() {
        if (saveFile == null) return;
        try {
            Files.createDirectories(saveFile.getParent());
            Set<String> raw = new HashSet<>();
            allowedPlayers.forEach(uuid -> raw.add(uuid.toString()));
            try (Writer w = Files.newBufferedWriter(saveFile)) {
                GSON.toJson(raw, w);
            }
        } catch (IOException e) {
            LOGGER.error("[XaeroBlock] Failed to save allowed-players file: {}", e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // Access logic
    // -----------------------------------------------------------------

    /** Returns true if the player is allowed to use both maps. */
    public static boolean canUseMap(ServerPlayer player) {
        if (player.hasPermissions(2)) return true;   // OP level 2+
        return allowedPlayers.contains(player.getUUID());
    }

    public static void allow(UUID uuid) {
        allowedPlayers.add(uuid);
        save();
    }

    public static void deny(UUID uuid) {
        allowedPlayers.remove(uuid);
        save();
    }

    public static boolean isExplicitlyAllowed(UUID uuid) {
        return allowedPlayers.contains(uuid);
    }
}
