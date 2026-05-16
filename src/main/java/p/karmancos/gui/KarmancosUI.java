package p.karmancos.gui;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import p.karmancos.gui.bedrock.BedrockFormService;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Small bootstrap/helper class for projects using KarmancosUI.
 */
public final class KarmancosUI {

    private static final Set<Plugin> REGISTERED_PLUGINS = Collections.newSetFromMap(new WeakHashMap<>());

    private KarmancosUI() {
    }

    /**
     * Register the global GUI listener once for the given plugin.
     *
     * @return true when a new listener was registered, false if it was already registered
     */
    public static boolean register(Plugin plugin) {
        if (plugin == null || REGISTERED_PLUGINS.contains(plugin)) {
            return false;
        }
        Bukkit.getPluginManager().registerEvents(new GuiListener(), plugin);
        REGISTERED_PLUGINS.add(plugin);
        return true;
    }

    /**
     * Whether Geyser/Floodgate classes are visible and Bedrock forms can be attempted.
     */
    public static boolean isBedrockFormsAvailable() {
        return BedrockFormService.isAvailable();
    }
}
