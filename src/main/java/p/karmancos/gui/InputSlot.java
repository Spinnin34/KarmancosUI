package p.karmancos.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Represents an interactive server-managed slot inside a GUI where players can
 * place and retrieve items without relying on client-side inventory movement.
 */
public class InputSlot {

    private final int slot;
    private ItemStack currentItem = null;
    private ItemStack placeholder = null;
    private Predicate<ItemStack> validator = null;
    private BiConsumer<Player, ItemStack> onChange = null;

    public InputSlot(int slot) {
        this.slot = slot;
    }

    public InputSlot setValidator(Predicate<ItemStack> validator) {
        this.validator = validator;
        return this;
    }

    public InputSlot setPlaceholder(ItemStack placeholder) {
        this.placeholder = placeholder != null && !placeholder.getType().isAir() ? placeholder.clone() : null;
        return this;
    }

    public InputSlot onChange(BiConsumer<Player, ItemStack> onChange) {
        this.onChange = onChange;
        return this;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getCurrentItem() {
        return currentItem != null ? currentItem.clone() : null;
    }

    public InputSlot setCurrentItem(ItemStack item) {
        setCurrentItemSilent(item);
        return this;
    }

    public InputSlot clearItem() {
        this.currentItem = null;
        return this;
    }

    void setCurrentItemSilent(ItemStack item) {
        this.currentItem = item != null && !item.getType().isAir() ? item.clone() : null;
    }

    Predicate<ItemStack> getValidator() {
        return validator;
    }

    void fireOnChangeExternal(Player player, ItemStack newItem) {
        fireOnChange(player, newItem);
    }

    public ItemStack getPlaceholder() {
        return placeholder != null ? placeholder.clone() : null;
    }

    public boolean isEmpty() {
        return currentItem == null;
    }

    /**
     * Consumes a click on this slot. The Bukkit event is always cancelled; all
     * movement is mirrored into currentItem and the player's cursor manually.
     */
    boolean handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return true;
        }

        ItemStack cursor = event.getCursor();
        boolean cursorHasItem = cursor != null && !cursor.getType().isAir();
        InventoryAction action = event.getAction();

        switch (action) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> handlePlace(player, cursor, cursorHasItem, action);
            case PICKUP_ALL, PICKUP_ONE, PICKUP_HALF, PICKUP_SOME -> handlePickup(player, cursor, cursorHasItem, action);
            case MOVE_TO_OTHER_INVENTORY, COLLECT_TO_CURSOR -> {
                // Blocked to keep this server-side slot authoritative.
            }
            default -> {
                // Other actions are intentionally ignored while cancelled.
            }
        }

        return true;
    }

    ItemStack getRenderedItem() {
        if (currentItem != null) {
            return currentItem.clone();
        }
        return placeholder != null ? placeholder.clone() : null;
    }

    private void handlePlace(Player player, ItemStack cursor, boolean cursorHasItem, InventoryAction action) {
        if (!cursorHasItem) {
            return;
        }
        if (validator != null && !validator.test(cursor.clone())) {
            return;
        }

        ItemStack toPlace;
        ItemStack newCursor = null;

        if (action == InventoryAction.SWAP_WITH_CURSOR) {
            toPlace = cursor.clone();
            if (currentItem != null) {
                newCursor = currentItem.clone();
            }
        } else if (currentItem != null) {
            return;
        } else if (action == InventoryAction.PLACE_ONE) {
            toPlace = cursor.clone();
            toPlace.setAmount(1);
            if (cursor.getAmount() > 1) {
                newCursor = cursor.clone();
                newCursor.setAmount(cursor.getAmount() - 1);
            }
        } else {
            toPlace = cursor.clone();
        }

        currentItem = toPlace;
        player.setItemOnCursor(newCursor);
        fireOnChange(player, currentItem);
    }

    private void handlePickup(Player player, ItemStack cursor, boolean cursorHasItem, InventoryAction action) {
        if (currentItem == null) {
            return;
        }
        if (cursorHasItem && !cursor.isSimilar(currentItem)) {
            return;
        }

        int cursorAmount = cursorHasItem ? cursor.getAmount() : 0;
        int maxStack = Math.min(currentItem.getMaxStackSize(), currentItem.getType().getMaxStackSize());
        int freeSpace = maxStack - cursorAmount;
        if (freeSpace <= 0) {
            return;
        }

        int requested;
        if (action == InventoryAction.PICKUP_ONE) {
            requested = 1;
        } else if (action == InventoryAction.PICKUP_HALF) {
            requested = (int) Math.ceil(currentItem.getAmount() / 2.0);
        } else {
            requested = currentItem.getAmount();
        }

        int amountToTake = Math.min(requested, freeSpace);
        ItemStack taken = currentItem.clone();
        taken.setAmount(amountToTake);
        player.setItemOnCursor(mergeCursor(cursor, taken));

        int remaining = currentItem.getAmount() - amountToTake;
        if (remaining <= 0) {
            currentItem = null;
        } else {
            currentItem.setAmount(remaining);
        }
        fireOnChange(player, currentItem);
    }

    private ItemStack mergeCursor(ItemStack cursor, ItemStack added) {
        if (cursor == null || cursor.getType().isAir()) {
            return added.clone();
        }
        ItemStack merged = cursor.clone();
        merged.setAmount(cursor.getAmount() + added.getAmount());
        return merged;
    }

    private void fireOnChange(Player player, ItemStack newItem) {
        if (onChange != null) {
            onChange.accept(player, newItem != null ? newItem.clone() : null);
        }
    }
}
