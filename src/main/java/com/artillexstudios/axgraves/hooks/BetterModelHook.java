package com.artillexstudios.axgraves.hooks;

import com.artillexstudios.axapi.utils.logging.LogUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.artillexstudios.axgraves.AxGraves.CONFIG;

/**
 * Hook do MagmaCore ModelService — spawnuje modele 3D z BetterModel
 * bez bezpośredniej zależności od BetterModel API.
 */
public class BetterModelHook {

    private static boolean available = false;

    public static void init() {
        if (Bukkit.getPluginManager().getPlugin("MagmaCore") == null) return;

        try {
            var service = pl.magmacore.MagmaCore.getInstance().getModelService();
            available = service != null && service.isAvailable();
        } catch (Throwable t) {
            available = false;
        }

        if (available) {
            LogUtils.info("BetterModel hook enabled (via MagmaCore ModelService)!");
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static boolean isEnabled() {
        return available && CONFIG.getBoolean("better-model.enabled", false);
    }

    /**
     * Spawnuje model 3D na podanej lokalizacji.
     * Zwraca ModelHandle (jako Object) lub null.
     */
    @Nullable
    public static Object spawnModel(@NotNull Location location, @NotNull OfflinePlayer offlinePlayer) {
        if (!isEnabled()) return null;

        String modelName = CONFIG.getString("better-model.model-name", "steve");
        boolean usePlayerModel = CONFIG.getBoolean("better-model.use-player-model", true);
        boolean lookAtPlayer = CONFIG.getBoolean("better-model.look-at-player", true);
        double lookRange = CONFIG.getDouble("better-model.look-range", 16.0);
        String idleAnimation = CONFIG.getString("better-model.idle-animation", "idle");

        try {
            var options = pl.magmacore.core.api.model.ModelOptions.builder()
                    .lookAtPlayer(lookAtPlayer)
                    .lookRange(lookRange)
                    .idleAnimation(idleAnimation)
                    .build();

            var service = pl.magmacore.MagmaCore.getInstance().getModelService();

            if (usePlayerModel) {
                return service.spawnPlayerModel(location, modelName, offlinePlayer, options);
            } else {
                return service.spawnModel(location, modelName, options);
            }
        } catch (Throwable t) {
            LogUtils.warn("BetterModel: failed to spawn model via MagmaCore: {}", t.getMessage());
            return null;
        }
    }

    /**
     * Tick modelu — spawn dla nowych graczy, aktualizacja rotacji.
     */
    public static void tickModel(@Nullable Object handle, @NotNull Location graveLoc) {
        if (handle == null) return;
        try {
            if (handle instanceof pl.magmacore.core.api.model.ModelHandle mh) {
                mh.tick();
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    /**
     * Usuwa model.
     */
    public static void removeModel(@Nullable Object handle) {
        if (handle == null) return;
        try {
            if (handle instanceof pl.magmacore.core.api.model.ModelHandle mh) {
                mh.remove();
            }
        } catch (Throwable t) {
            LogUtils.warn("BetterModel: failed to remove model: {}", t.getMessage());
        }
    }
}
