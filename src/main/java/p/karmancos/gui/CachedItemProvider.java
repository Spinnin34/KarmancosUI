package p.karmancos.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Advanced ItemProvider with caching and localization support
 */
public abstract class CachedItemProvider implements ItemProvider {

    private final Map<String, ItemStack> cache = new HashMap<>();
    private final boolean enableCache;
    private final long cacheDuration;
    private final Map<String, Long> cacheTimestamps = new HashMap<>();

    public CachedItemProvider(boolean enableCache, long cacheDurationMs) {
        this.enableCache = enableCache;
        this.cacheDuration = cacheDurationMs;
    }

    public CachedItemProvider() {
        this(true, 5000); // 5 seconds default
    }

    @Override
    public ItemStack getItem(Player player) {
        if (!enableCache) {
            return createItem(player);
        }

        String cacheKey = getCacheKey(player);
        long currentTime = System.currentTimeMillis();

        if (cache.containsKey(cacheKey)) {
            Long timestamp = cacheTimestamps.get(cacheKey);
            if (timestamp != null && (currentTime - timestamp) < cacheDuration) {
                return cache.get(cacheKey).clone();
            }
        }

        ItemStack item = createItem(player);
        cache.put(cacheKey, item.clone());
        cacheTimestamps.put(cacheKey, currentTime);

        return item;
    }

    protected abstract ItemStack createItem(Player player);

    protected String getCacheKey(Player player) {
        return player != null ? player.getUniqueId().toString() : "default";
    }

    public void invalidateCache() {
        cache.clear();
        cacheTimestamps.clear();
    }

    public void invalidateCache(Player player) {
        String key = getCacheKey(player);
        cache.remove(key);
        cacheTimestamps.remove(key);
    }
}

