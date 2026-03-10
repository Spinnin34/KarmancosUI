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
 * - InputSlot support — interactive slots where players can place/take items with anti-dupe protection
 */
public abstract class BaseGui implements InventoryHolder {

    protected Inventory inventory;
    protected final Map<Integer, GuiItem> items;
    /** Interactive input slots indexed by their slot number. */
    protected final Map<Integer, InputSlot> inputSlots = new LinkedHashMap<>();
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
        // Re-render input slot placeholders so they are not wiped
        renderInputSlots();
    }

    // ─── InputSlot management ────────────────────────────────────

    /**
     * Register an {@link InputSlot} in this GUI.
     * An InputSlot is a slot where the player can freely place and take items,
     * with optional validation, placeholder and change callbacks.
     * <p>
     * The slot must not overlap with a normal {@link GuiItem} slot — if it does,
     * the InputSlot takes priority and the GuiItem is removed.
     *
     * @param inputSlot the slot to register
     * @return this (fluent)
     */
    public BaseGui addInputSlot(InputSlot inputSlot) {
        int s = inputSlot.getSlot();
        if (s < 0 || s >= inventory.getSize()) return this;
        // Remove any static GuiItem that was occupying the same slot
        items.remove(s);
        inputSlots.put(s, inputSlot);
        inventory.setItem(s, inputSlot.getRenderedItem());
        return this;
    }

    /**
     * Remove the {@link InputSlot} at the given slot index.
     * Any item currently held in that slot is dropped — see {@link #removeInputSlot(int, Player)}.
     *
     * @param slot the inventory slot index
     */
    public void removeInputSlot(int slot) {
        InputSlot is = inputSlots.remove(slot);
        if (is != null) {
            inventory.setItem(slot, null);
        }
    }

    /**
     * Remove the {@link InputSlot} at the given slot index, returning the stored item to the player.
     *
     * @param slot   the inventory slot index
     * @param player the player that receives the item (if any)
     */
    public void removeInputSlot(int slot, Player player) {
        InputSlot is = inputSlots.remove(slot);
        if (is != null) {
            ItemStack stored = is.getCurrentItem();
            if (stored != null && player != null) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(stored);
                // Drop leftovers at feet to prevent item loss
                leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            }
            inventory.setItem(slot, null);
        }
    }

    /**
     * Get the {@link InputSlot} registered at the given index, or {@code null}.
     *
     * @param slot the inventory slot index
     */
    public InputSlot getInputSlot(int slot) {
        return inputSlots.get(slot);
    }

    /**
     * Return an unmodifiable view of all registered InputSlots.
     */
    public Collection<InputSlot> getInputSlots() {
        return Collections.unmodifiableCollection(inputSlots.values());
    }

    /**
     * Clear every InputSlot, returning all stored items to the given player.
     * Pass {@code null} to drop items at the player's last location instead.
     *
     * @param player the player that receives the items, or {@code null} to discard
     */
    public void clearInputSlots(Player player) {
        for (InputSlot is : inputSlots.values()) {
            ItemStack stored = is.getCurrentItem();
            if (stored != null) {
                if (player != null) {
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(stored);
                    leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                }
                is.setCurrentItemSilent(null);
            }
            inventory.setItem(is.getSlot(), is.getRenderedItem());
        }
    }

    /** Re-render every InputSlot in the inventory (placeholder or real item). */
    protected void renderInputSlots() {
        for (InputSlot is : inputSlots.values()) {
            inventory.setItem(is.getSlot(), is.getRenderedItem());
        }
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
        // Render input slots (real item or placeholder) on top of static items
        renderInputSlots();

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
            // Skip slots occupied by an InputSlot so we don't overwrite the player's item
            if (inputSlots.containsKey(entry.getKey())) continue;
            ItemStack itemStack = entry.getValue().getItemStack(player);
            if (itemStack != null) {
                inventory.setItem(entry.getKey(), itemStack);
            }
        }
        renderInputSlots();
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
            int slot = event.getSlot();

            // ── InputSlot takes priority ──────────────────────────
            InputSlot inputSlot = inputSlots.get(slot);
            if (inputSlot != null) {
                inputSlot.handleClick(event);
                // Sync rendered item back to inventory after the interaction
                inventory.setItem(slot, inputSlot.getRenderedItem());
                return;
            }

            // ── Normal GuiItem ────────────────────────────────────
            event.setCancelled(true);
            GuiItem item = items.get(slot);
            if (item != null) {
                item.handleClick(event);
            }
        } else if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
            // Shift-click from player inventory: if it would land on an InputSlot, redirect there
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack moved = event.getCurrentItem();
                if (moved != null && !moved.getType().isAir()) {
                    // Find first InputSlot that accepts this item
                    for (InputSlot is : inputSlots.values()) {
                        if (is.isEmpty()) {
                            // Synthesize a fake click event is not possible cleanly,
                            // so we handle it manually here.
                            event.setCancelled(true);
                            boolean accepted = tryDepositIntoInputSlot(is, moved, (Player) event.getWhoClicked());
                            if (accepted) {
                                event.setCurrentItem(null); // remove from player inv
                            }
                            return;
                        }
                    }
                }
            }

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
     * Attempt to deposit {@code item} into {@code inputSlot} on behalf of {@code player}.
     * Returns {@code true} if the item was accepted.
     */
    private boolean tryDepositIntoInputSlot(InputSlot inputSlot, ItemStack item, Player player) {
        if (item == null || item.getType().isAir()) return false;
        // Run the InputSlot's validator (package-private accessor)
        java.util.function.Predicate<ItemStack> validator = inputSlot.getValidator();
        if (validator != null && !validator.test(item)) return false;

        inputSlot.setCurrentItemSilent(item);
        inventory.setItem(inputSlot.getSlot(), inputSlot.getRenderedItem());
        inputSlot.fireOnChangeExternal(player, item);
        return true;
    }

    /**
     * Handle drag events.
     */
    public void handleDrag(InventoryDragEvent event) {
        // Block drags that touch any InputSlot or (if !allowDrag) any GUI slot
        for (int slot : event.getRawSlots()) {
            if (slot < inventory.getSize()) {
                if (inputSlots.containsKey(slot) || !allowDrag) {
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

        // Return items stored in InputSlots to the player who closed the GUI
        if (!inputSlots.isEmpty()) {
            for (InputSlot is : inputSlots.values()) {
                ItemStack stored = is.getCurrentItem();
                if (stored != null) {
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(stored);
                    leftover.values().forEach(item ->
                            player.getWorld().dropItemNaturally(player.getLocation(), item));
                    is.setCurrentItemSilent(null);
                    inventory.setItem(is.getSlot(), is.getRenderedItem());
                }
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
