package p.karmancos.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import p.karmancos.gui.utils.ColorTranslator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Enhanced BaseGui - core of the KarmancosGUI library.
 * <p>
 * Features:
 * - No getInstance() dependency — plugin is injected or resolved automatically
 * - Auto-update system with configurable intervals
 * - Memory-optimized viewer tracking
 * - Slot-level update timestamps
 * - Concurrent access support
 * - Player inventory interaction control
 * - Drag event handling
 * - Structure-based layout support
 * - PreventClose with next-tick reopen
 */
public abstract class BaseGui implements InventoryHolder {

    protected Inventory inventory;
    protected final Map<Integer, GuiItem> items;
    protected Component title;
    protected int rows;
    protected InventoryType type;
    protected Consumer<InventoryCloseEvent> closeAction;
    protected Consumer<InventoryOpenEvent> openAction;
    protected Consumer<InventoryClickEvent> outsideClickAction;
    protected Consumer<InventoryClickEvent> playerInventoryClickAction;
    protected Consumer<InventoryDragEvent> dragAction;
    protected boolean preventClose = false;
    protected boolean allowPlayerInventoryClick = false;
    protected boolean allowDrag = false;
    protected final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    protected BukkitTask updateTask;
    protected boolean autoUpdate = false;
    protected long updateInterval = 20L;
    protected final Map<Integer, Long> slotUpdateTimestamps = new ConcurrentHashMap<>();
    protected UpdatePriority updatePriority = UpdatePriority.NORMAL;
    protected Plugin plugin;

    // ─── Constructors ───────────────────────────────────────────

    public BaseGui(int rows, Component title) {
        this.rows = Math.max(1, Math.min(6, rows));
        this.title = title;
        this.items = new ConcurrentHashMap<>();
        this.inventory = Bukkit.createInventory(this, this.rows * 9,
                LegacyComponentSerializer.legacySection().serialize(title));
        this.type = InventoryType.CHEST;
    }

    public BaseGui(int rows, String title) {
        this(rows, ColorTranslator.translate(title));
    }

    public BaseGui(InventoryType type, Component title) {
        this.type = type;
        this.title = title;
        this.rows = 0;
        this.items = new ConcurrentHashMap<>();
        this.inventory = Bukkit.createInventory(this, type,
                LegacyComponentSerializer.legacySection().serialize(title));
    }

    public BaseGui(InventoryType type, String title) {
        this(type, ColorTranslator.translate(title));
    }

    // ─── Plugin injection ───────────────────────────────────────

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    protected Plugin getPlugin() {
        if (plugin != null) return plugin;
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        for (Plugin p : plugins) {
            if (p.isEnabled()) {
                this.plugin = p;
                return p;
            }
        }
        return null;
    }

    // ─── Item management ────────────────────────────────────────

    /**
     * Set an item at a specific slot.
     */
    public void setItem(int slot, GuiItem item) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        items.put(slot, item);
        slotUpdateTimestamps.put(slot, System.currentTimeMillis());

        try {
            ItemStack itemStack = item.getItemStack(null);
            if (itemStack != null) {
                inventory.setItem(slot, itemStack);
            }
        } catch (Exception e) {
            // Item provider requires player — resolved on open()
        }
    }

    /**
     * Set an item at row, column (0-based).
     */
    public void setItem(int row, int col, GuiItem item) {
        setItem(row * 9 + col, item);
    }

    /**
     * Remove an item from a slot.
     */
    public void removeItem(int slot) {
        items.remove(slot);
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, null);
        }
    }

    /**
     * Get the GuiItem at a specific slot.
     */
    public GuiItem getItem(int slot) {
        return items.get(slot);
    }

    /**
     * Update a single slot for all viewers.
     */
    public void updateSlot(int slot) {
        GuiItem item = items.get(slot);
        if (item == null) return;

        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                ItemStack itemStack = item.getItemStack(viewer);
                inventory.setItem(slot, itemStack);
            }
        }
        slotUpdateTimestamps.put(slot, System.currentTimeMillis());
    }

    /**
     * Update a specific slot for a specific player.
     */
    public void updateSlot(int slot, Player player) {
        GuiItem item = items.get(slot);
        if (item == null) return;

        ItemStack itemStack = item.getItemStack(player);
        if (player.getOpenInventory().getTopInventory().equals(inventory)) {
            inventory.setItem(slot, itemStack);
        }
        slotUpdateTimestamps.put(slot, System.currentTimeMillis());
    }

    /**
     * Add an item to the first empty slot.
     */
    public void addItem(GuiItem item) {
        int slot = inventory.firstEmpty();
        if (slot != -1) {
            setItem(slot, item);
        }
    }

    /**
     * Fill all empty slots with an item.
     */
    public void fill(GuiItem item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack is = inventory.getItem(i);
            if (is == null || is.getType().isAir()) {
                setItem(i, item);
            }
        }
    }

    /**
     * Fill the border of the inventory.
     */
    public void fillBorder(GuiItem item) {
        if (type != InventoryType.CHEST || rows == 0) return;

        int size = inventory.getSize();
        int rowCount = size / 9;

        for (int i = 0; i < 9; i++) {
            setItem(i, item);
            setItem(size - 9 + i, item);
        }

        for (int i = 1; i < rowCount - 1; i++) {
            setItem(i * 9, item);
            setItem(i * 9 + 8, item);
        }
    }

    /**
     * Fill a specific row (0-based).
     */
    public void fillRow(int row, GuiItem item) {
        if (row < 0 || row >= rows) return;
        int startSlot = row * 9;
        for (int i = 0; i < 9; i++) {
            setItem(startSlot + i, item);
        }
    }

    /**
     * Fill a specific column (0-based, 0-8).
     */
    public void fillColumn(int column, GuiItem item) {
        if (column < 0 || column >= 9) return;
        for (int i = 0; i < rows; i++) {
            setItem(i * 9 + column, item);
        }
    }

    /**
     * Fill a rectangular area (inclusive, 0-based).
     */
    public void fillArea(int startRow, int startCol, int endRow, int endCol, GuiItem item) {
        for (int row = startRow; row <= endRow && row < rows; row++) {
            for (int col = startCol; col <= endCol && col < 9; col++) {
                setItem(row * 9 + col, item);
            }
        }
    }

    /**
     * Clear all items.
     */
    public void clear() {
        items.clear();
        inventory.clear();
        slotUpdateTimestamps.clear();
    }

    // ─── Structure support ──────────────────────────────────────

    /**
     * Apply a structure layout to the GUI.
     * <p>
     * Each character in the structure maps to an ingredient.
     * Spaces between chars are ignored.
     *
     * @param structure     Array of strings (e.g. "# # # # # # # # #")
     * @param ingredients   Map of char to GuiItem
     * @param contentMarker Character that marks content slots (returned)
     * @return array of slot indices matching the content marker
     */
    public int[] applyStructure(String[] structure, Map<Character, GuiItem> ingredients, char contentMarker) {
        List<Integer> contentSlots = new ArrayList<>();
        int slot = 0;

        for (String row : structure) {
            String cleaned = row.replace(" ", "");
            for (int i = 0; i < cleaned.length() && slot < inventory.getSize(); i++) {
                char c = cleaned.charAt(i);
                if (c == contentMarker) {
                    contentSlots.add(slot);
                } else if (ingredients.containsKey(c)) {
                    setItem(slot, ingredients.get(c));
                }
                slot++;
            }
        }

        return contentSlots.stream().mapToInt(Integer::intValue).toArray();
    }

    // ─── Open / Close ───────────────────────────────────────────

    /**
     * Open the GUI for a player.
     */
    public void open(Player player) {
        for (Map.Entry<Integer, GuiItem> entry : items.entrySet()) {
            ItemStack itemStack = entry.getValue().getItemStack(player);
            if (itemStack != null) {
                inventory.setItem(entry.getKey(), itemStack);
            }
        }

        viewers.add(player.getUniqueId());
        player.openInventory(inventory);

        if (autoUpdate && updateTask == null) {
            startAutoUpdate();
        }
    }

    /**
     * Close the GUI for a specific player.
     */
    public void close(Player player) {
        viewers.remove(player.getUniqueId());
        player.closeInventory();

        if (viewers.isEmpty() && updateTask != null) {
            stopAutoUpdate();
        }
    }

    /**
     * Close the GUI for all viewers.
     */
    public void closeAll() {
        for (UUID viewerId : new ArrayList<>(viewers)) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                viewer.closeInventory();
            }
        }
        viewers.clear();
        stopAutoUpdate();
    }

    // ─── Update system ──────────────────────────────────────────

    /**
     * Update all items for all viewers.
     */
    public void update() {
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                updateForPlayer(viewer);
            }
        }
    }

    /**
     * Update all items for a specific player.
     */
    public void updateForPlayer(Player player) {
        if (!viewers.contains(player.getUniqueId())) return;

        for (Map.Entry<Integer, GuiItem> entry : items.entrySet()) {
            ItemStack itemStack = entry.getValue().getItemStack(player);
            if (itemStack != null) {
                inventory.setItem(entry.getKey(), itemStack);
            }
        }
    }

    /**
     * Enable auto-update with custom interval.
     */
    public void setAutoUpdate(boolean autoUpdate, long intervalTicks) {
        this.autoUpdate = autoUpdate;
        this.updateInterval = intervalTicks;

        if (autoUpdate && !viewers.isEmpty() && updateTask == null) {
            startAutoUpdate();
        } else if (!autoUpdate && updateTask != null) {
            stopAutoUpdate();
        }
    }

    protected void startAutoUpdate() {
        Plugin p = getPlugin();
        if (p == null) return;

        updateTask = Bukkit.getScheduler().runTaskTimer(p, this::update,
                updatePriority.getDelayTicks(), updateInterval);
    }

    protected void stopAutoUpdate() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    // ─── Configuration setters ──────────────────────────────────

    public void setCloseAction(Consumer<InventoryCloseEvent> closeAction) {
        this.closeAction = closeAction;
    }

    public void setOpenAction(Consumer<InventoryOpenEvent> openAction) {
        this.openAction = openAction;
    }

    public void setOutsideClickAction(Consumer<InventoryClickEvent> outsideClickAction) {
        this.outsideClickAction = outsideClickAction;
    }

    public void setPlayerInventoryClickAction(Consumer<InventoryClickEvent> action) {
        this.playerInventoryClickAction = action;
    }

    public void setDragAction(Consumer<InventoryDragEvent> dragAction) {
        this.dragAction = dragAction;
    }

    public void setAllowPlayerInventoryClick(boolean allow) {
        this.allowPlayerInventoryClick = allow;
    }

    public void setAllowDrag(boolean allow) {
        this.allowDrag = allow;
    }

    public void setPreventClose(boolean preventClose) {
        this.preventClose = preventClose;
    }

    public void setUpdatePriority(UpdatePriority priority) {
        this.updatePriority = priority;
        if (updateTask != null) {
            stopAutoUpdate();
            if (autoUpdate && !viewers.isEmpty()) {
                startAutoUpdate();
            }
        }
    }

    public boolean isPreventClose() {
        return preventClose;
    }

    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    public int getViewerCount() {
        return viewers.size();
    }

    public int getRows() {
        return rows;
    }

    public int getSize() {
        return inventory.getSize();
    }

    public Component getTitle() {
        return title;
    }

    // ─── Event handlers ─────────────────────────────────────────

    /**
     * Handle click events.
     */
    public void handleClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == inventory) {
            event.setCancelled(true);
            int slot = event.getSlot();
            GuiItem item = items.get(slot);
            if (item != null) {
                item.handleClick(event);
            }
        } else if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
            if (!allowPlayerInventoryClick) {
                event.setCancelled(true);
            }
            if (playerInventoryClickAction != null) {
                playerInventoryClickAction.accept(event);
            }
        } else if (event.getClickedInventory() == null) {
            if (outsideClickAction != null) {
                outsideClickAction.accept(event);
            }
        }
    }

    /**
     * Handle drag events.
     */
    public void handleDrag(InventoryDragEvent event) {
        if (!allowDrag) {
            for (int slot : event.getRawSlots()) {
                if (slot < inventory.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (dragAction != null) {
            dragAction.accept(event);
        }
    }

    /**
     * Handle close events with preventClose support.
     */
    public void handleClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        viewers.remove(player.getUniqueId());

        if (preventClose && player.isOnline()) {
            Plugin p = getPlugin();
            if (p != null) {
                Bukkit.getScheduler().runTaskLater(p, () -> {
                    if (player.isOnline()) {
                        open(player);
                    }
                }, 1L);
                return;
            }
        }

        if (closeAction != null) {
            closeAction.accept(event);
        }

        if (viewers.isEmpty()) {
            stopAutoUpdate();
            cleanup();
        }
    }

    public void handleOpen(InventoryOpenEvent event) {
        if (openAction != null) {
            openAction.accept(event);
        }
    }

    /**
     * Cleanup resources when GUI is closed.
     */
    protected void cleanup() {
        stopAutoUpdate();
        slotUpdateTimestamps.clear();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
