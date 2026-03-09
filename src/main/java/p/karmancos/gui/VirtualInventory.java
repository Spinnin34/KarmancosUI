package p.karmancos.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;


public class VirtualInventory extends BaseGui {

    private final Map<Integer, ItemStack> storedItems;
    private final Map<Integer, Integer> maxStackSizes;
    private int defaultMaxStackSize = 64;
    private Predicate<ItemStack> itemFilter;
    private BiConsumer<Player, ItemStack> onItemAdd;
    private BiConsumer<Player, ItemStack> onItemRemove;
    private boolean autoSort = false;

    public VirtualInventory(int rows, Component title) {
        super(rows, title);
        this.storedItems = new HashMap<>();
        this.maxStackSizes = new HashMap<>();
    }

    public VirtualInventory(int rows, String title) {
        super(rows, title);
        this.storedItems = new HashMap<>();
        this.maxStackSizes = new HashMap<>();
    }

    /**
     * Set default maximum stack size for all slots
     */
    public void setDefaultMaxStackSize(int size) {
        this.defaultMaxStackSize = Math.max(1, Math.min(64, size));
        inventory.setMaxStackSize(this.defaultMaxStackSize);
    }

    /**
     * Set maximum stack size for a specific slot
     */
    public void setMaxStackSize(int slot, int size) {
        maxStackSizes.put(slot, Math.max(1, Math.min(64, size)));
    }

    /**
     * Set item filter to restrict which items can be placed
     */
    public void setItemFilter(Predicate<ItemStack> filter) {
        this.itemFilter = filter;
    }

    /**
     * Set callback when item is added
     */
    public void setOnItemAdd(BiConsumer<Player, ItemStack> callback) {
        this.onItemAdd = callback;
    }

    /**
     * Set callback when item is removed
     */
    public void setOnItemRemove(BiConsumer<Player, ItemStack> callback) {
        this.onItemRemove = callback;
    }

    /**
     * Enable/disable auto-sorting
     */
    public void setAutoSort(boolean autoSort) {
        this.autoSort = autoSort;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == inventory) {
            int slot = event.getSlot();
            ItemStack clicked = event.getCurrentItem();
            ItemStack cursor = event.getCursor();

            // Check filter
            if (cursor != null && !cursor.getType().isAir()) {
                if (itemFilter != null && !itemFilter.test(cursor)) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Check custom max stack size
            int maxStack = maxStackSizes.getOrDefault(slot, defaultMaxStackSize);
            if (clicked != null && clicked.getAmount() > maxStack) {
                clicked.setAmount(maxStack);
            }

            // Don't cancel - allow normal interaction
            // But track changes
            Plugin p = getPlugin();
            if (p == null) return;
            Bukkit.getScheduler().runTaskLater(p, () -> {
                ItemStack newItem = inventory.getItem(slot);

                if (clicked != null && !clicked.getType().isAir() &&
                    (newItem == null || newItem.getType().isAir())) {
                    // Item removed
                    if (onItemRemove != null && event.getWhoClicked() instanceof Player) {
                        onItemRemove.accept((Player) event.getWhoClicked(), clicked);
                    }
                } else if ((clicked == null || clicked.getType().isAir()) &&
                           newItem != null && !newItem.getType().isAir()) {
                    // Item added
                    if (onItemAdd != null && event.getWhoClicked() instanceof Player) {
                        onItemAdd.accept((Player) event.getWhoClicked(), newItem);
                    }
                }

                // Update stored items
                storedItems.put(slot, newItem);

                if (autoSort) {
                    sortInventory();
                }
            }, 1L);

            // Let GuiItem handle if present
            GuiItem item = items.get(slot);
            if (item != null) {
                event.setCancelled(true);
                item.handleClick(event);
            }
        }
    }

    /**
     * Sort inventory by item type and amount
     */
    public void sortInventory() {
        Map<ItemStack, Integer> itemCounts = new HashMap<>();

        // Collect all items
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                boolean found = false;
                for (ItemStack key : itemCounts.keySet()) {
                    if (key.isSimilar(item)) {
                        itemCounts.put(key, itemCounts.get(key) + item.getAmount());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    itemCounts.put(item.clone(), item.getAmount());
                }
            }
        }

        // Clear inventory
        inventory.clear();

        // Re-add sorted items
        int slot = 0;
        for (Map.Entry<ItemStack, Integer> entry : itemCounts.entrySet()) {
            ItemStack item = entry.getKey();
            int amount = entry.getValue();

            while (amount > 0 && slot < inventory.getSize()) {
                int stackSize = Math.min(amount, item.getMaxStackSize());
                ItemStack stack = item.clone();
                stack.setAmount(stackSize);

                inventory.setItem(slot, stack);
                storedItems.put(slot, stack);

                amount -= stackSize;
                slot++;
            }
        }
    }

    /**
     * Add an item to the inventory
     */
    public boolean addItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (itemFilter != null && !itemFilter.test(item)) return false;

        Map<Integer, ItemStack> leftover = inventory.addItem(item);

        // Update stored items
        for (int i = 0; i < inventory.getSize(); i++) {
            storedItems.put(i, inventory.getItem(i));
        }

        return leftover.isEmpty();
    }

    /**
     * Remove an item from the inventory
     */
    public boolean removeItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        Map<Integer, ItemStack> leftover = inventory.removeItem(item);

        // Update stored items
        for (int i = 0; i < inventory.getSize(); i++) {
            storedItems.put(i, inventory.getItem(i));
        }

        return leftover.isEmpty();
    }

    /**
     * Check if inventory contains an item
     */
    public boolean contains(ItemStack item) {
        return inventory.contains(item);
    }

    /**
     * Count specific item in inventory
     */
    public int count(ItemStack item) {
        int count = 0;
        for (ItemStack stored : inventory.getContents()) {
            if (stored != null && stored.isSimilar(item)) {
                count += stored.getAmount();
            }
        }
        return count;
    }

    /**
     * Get first empty slot
     */
    public int firstEmpty() {
        return inventory.firstEmpty();
    }

    /**
     * Check if inventory is full
     */
    public boolean isFull() {
        return inventory.firstEmpty() == -1;
    }

    /**
     * Clear all items
     */
    public void clearAll() {
        inventory.clear();
        storedItems.clear();
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        super.handleClose(event);
        // Save items to map
        for (int i = 0; i < inventory.getSize(); i++) {
            storedItems.put(i, inventory.getItem(i));
        }
    }

    @Override
    public void handleOpen(InventoryOpenEvent event) {
        super.handleOpen(event);
        // Restore items
        for (Map.Entry<Integer, ItemStack> entry : storedItems.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Get stored items map
     */
    public Map<Integer, ItemStack> getStoredItems() {
        // Update from current inventory state if open
        if (!viewers.isEmpty()) {
            for (int i = 0; i < inventory.getSize(); i++) {
                storedItems.put(i, inventory.getItem(i));
            }
        }
        return new HashMap<>(storedItems);
    }

    /**
     * Get all items as array
     */
    public ItemStack[] getContents() {
        return inventory.getContents();
    }
}

