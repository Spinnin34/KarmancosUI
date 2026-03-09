package p.karmancos.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.Plugin;
import p.karmancos.gui.utils.ColorTranslator;

/**
 * Fluent builder for creating different types of GUIs.
 * <p>
 * Usage:
 * <pre>
 * BaseGui gui = new GuiBuilder()
 *     .setRows(6)
 *     .setTitle("&eMy GUI")
 *     .setPaged(true)
 *     .build();
 * </pre>
 */
public class GuiBuilder {

    private int rows = 3;
    private Component title = Component.text("Gui");
    private InventoryType type = InventoryType.CHEST;
    private GuiType guiType = GuiType.NORMAL;
    private Plugin plugin;
    private boolean autoUpdate = false;
    private long updateInterval = 20L;
    private UpdatePriority updatePriority = UpdatePriority.NORMAL;
    private boolean preventClose = false;
    private boolean allowPlayerInventoryClick = false;

    public enum GuiType {
        NORMAL, PAGED, SCROLL, ANIMATED, TAB, VIRTUAL
    }

    /**
     * Create a GuiBuilder without a plugin reference.
     * Plugin will be auto-resolved if needed.
     */
    public GuiBuilder() {
    }

    /**
     * Create a GuiBuilder with a plugin reference for scheduling.
     */
    public GuiBuilder(Plugin plugin) {
        this.plugin = plugin;
    }

    public GuiBuilder setPlugin(Plugin plugin) {
        this.plugin = plugin;
        return this;
    }

    public GuiBuilder setRows(int rows) {
        this.rows = Math.max(1, Math.min(6, rows));
        return this;
    }

    public GuiBuilder setTitle(Component title) {
        this.title = title;
        return this;
    }

    public GuiBuilder setTitle(String title) {
        this.title = ColorTranslator.translate(title);
        return this;
    }

    public GuiBuilder setType(InventoryType type) {
        this.type = type;
        return this;
    }

    public GuiBuilder setPaged(boolean paged) {
        if (paged) this.guiType = GuiType.PAGED;
        return this;
    }

    public GuiBuilder setScroll(boolean scroll) {
        if (scroll) this.guiType = GuiType.SCROLL;
        return this;
    }

    public GuiBuilder setAnimated(boolean animated) {
        if (animated) this.guiType = GuiType.ANIMATED;
        return this;
    }

    public GuiBuilder setTabs(boolean tabs) {
        if (tabs) this.guiType = GuiType.TAB;
        return this;
    }

    public GuiBuilder setVirtual(boolean virtual) {
        if (virtual) this.guiType = GuiType.VIRTUAL;
        return this;
    }

    public GuiBuilder setGuiType(GuiType guiType) {
        this.guiType = guiType;
        return this;
    }

    public GuiBuilder setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
        return this;
    }

    public GuiBuilder setUpdateInterval(long ticks) {
        this.updateInterval = Math.max(1, ticks);
        return this;
    }

    public GuiBuilder setUpdatePriority(UpdatePriority priority) {
        this.updatePriority = priority;
        return this;
    }

    public GuiBuilder setPreventClose(boolean preventClose) {
        this.preventClose = preventClose;
        return this;
    }

    public GuiBuilder setAllowPlayerInventoryClick(boolean allow) {
        this.allowPlayerInventoryClick = allow;
        return this;
    }

    public BaseGui build() {
        BaseGui gui;

        switch (guiType) {
            case ANIMATED:
                if (plugin == null) {
                    throw new IllegalStateException("AnimatedGui requires a Plugin reference. Use setPlugin() or the GuiBuilder(Plugin) constructor.");
                }
                gui = new AnimatedGui(plugin, rows, title);
                break;
            case PAGED:
                gui = new PagedGui(rows, title);
                break;
            case SCROLL:
                gui = new ScrollGui(rows, title);
                break;
            case TAB:
                gui = new TabGui(rows, title);
                break;
            case VIRTUAL:
                gui = new VirtualInventory(rows, title);
                break;
            case NORMAL:
            default:
                if (type == InventoryType.CHEST) {
                    gui = new BaseGui(rows, title) {};
                } else {
                    gui = new BaseGui(type, title) {};
                }
                break;
        }

        // Apply plugin reference
        if (plugin != null) {
            gui.setPlugin(plugin);
        }

        // Apply common configurations
        if (autoUpdate) {
            gui.setAutoUpdate(true, updateInterval);
        }
        gui.setUpdatePriority(updatePriority);
        gui.setPreventClose(preventClose);
        gui.setAllowPlayerInventoryClick(allowPlayerInventoryClick);

        return gui;
    }
}
