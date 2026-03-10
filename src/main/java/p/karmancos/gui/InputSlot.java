package p.karmancos.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Represents an interactive slot inside a GUI where players can place and retrieve items.
 * <p>
 * Features:
 * <ul>
 *   <li>Optional item validator — only items matching the predicate are accepted.</li>
 *   <li>onChange callback — fired whenever the slot content changes.</li>
 *   <li>Anti-dupe protection — the slot is managed server-side; no client-side exploits.</li>
 *   <li>Optional placeholder item shown when the slot is empty.</li>
 * </ul>
 * <p>
 * Example:
 * <pre>
 * InputSlot slot = new InputSlot(22)
 *     .setValidator(item -> item.getType() == Material.DIAMOND)
 *     .setPlaceholder(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("&7Drop a diamond here").build())
 *     .onChange((player, item) -> player.sendMessage("You placed: " + item.getType()));
 * gui.addInputSlot(slot);
 * </pre>
 */
public class InputSlot {

    private final int slot;
    private ItemStack currentItem = null;
    private ItemStack placeholder = null;
    private Predicate<ItemStack> validator = null;
    private BiConsumer<Player, ItemStack> onChange = null;

    /**
     * Create an InputSlot at the given inventory slot index.
     *
     * @param slot the inventory slot index (0-based)
     */
    public InputSlot(int slot) {
        this.slot = slot;
    }

    // ─── Configuration ──────────────────────────────────────────

    /**
     * Set a validator predicate. Only items for which this returns {@code true} will be accepted.
     * A {@code null} or {@link ItemStack} with AIR type will always be rejected regardless of the validator.
     *
     * @param validator predicate receiving the item the player is trying to place
     * @return this (fluent)
     */
    public InputSlot setValidator(Predicate<ItemStack> validator) {
        this.validator = validator;
        return this;
    }

    /**
     * Set a placeholder item that is shown when the slot is empty.
     * The placeholder is purely visual — it cannot be taken by the player.
     *
     * @param placeholder the item to display when empty, or {@code null} to show nothing
     * @return this (fluent)
     */
    public InputSlot setPlaceholder(ItemStack placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    /**
     * Register a callback that fires every time the slot content changes (item placed or removed).
     * The second argument is the new item stack ({@code null} when the slot has been cleared).
     *
     * @param onChange callback receiving the player who triggered the change and the new item
     * @return this (fluent)
     */
    public InputSlot onChange(BiConsumer<Player, ItemStack> onChange) {
        this.onChange = onChange;
        return this;
    }

    // ─── Internal ───────────────────────────────────────────────

    /**
     * Return the inventory slot index this InputSlot occupies.
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Return the item currently stored in this slot, or {@code null} if empty.
     */
    public ItemStack getCurrentItem() {
        return currentItem != null ? currentItem.clone() : null;
    }

    /** Force-set the stored item without triggering the onChange callback. */
    void setCurrentItemSilent(ItemStack item) {
        this.currentItem = (item != null && !item.getType().isAir()) ? item.clone() : null;
    }

    /** Package-private: returns the validator, or null. Used by BaseGui for shift-click deposits. */
    java.util.function.Predicate<ItemStack> getValidator() {
        return validator;
    }

    /** Package-private: fire onChange externally (used by BaseGui for shift-click deposits). */
    void fireOnChangeExternal(Player player, ItemStack newItem) {
        fireOnChange(player, newItem);
    }

    /**
     * Return the placeholder item, or {@code null} if none is configured.
     */
    public ItemStack getPlaceholder() {
        return placeholder != null ? placeholder.clone() : null;
    }

    /**
     * Whether this slot currently holds a player item.
     */
    public boolean isEmpty() {
        return currentItem == null;
    }

    /**
     * Handle a click event on this slot. Returns {@code true} if the event was consumed.
     * Anti-dupe: the event is always cancelled; item movement is handled manually.
     */
    boolean handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return true;

        ItemStack cursor = event.getCursor();
        boolean cursorHasItem = !cursor.getType().isAir();

        switch (event.getAction()) {
            // ── Player placing an item into the slot ──────────────
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> {
                if (!cursorHasItem) break;

                // Validate
                if (validator != null && !validator.test(cursor)) break;

                ItemStack toPlace;
                ItemStack newCursor = null;

                if (event.getAction() == org.bukkit.event.inventory.InventoryAction.PLACE_ONE) {
                    // Place exactly one
                    toPlace = cursor.clone();
                    toPlace.setAmount(1);
                    if (cursor.getAmount() > 1) {
                        newCursor = cursor.clone();
                        newCursor.setAmount(cursor.getAmount() - 1);
                    }
                } else {
                    // PLACE_ALL / SWAP_WITH_CURSOR → place whole stack
                    toPlace = cursor.clone();
                    // If there was an old item, give it back via cursor (swap)
                    if (currentItem != null) {
                        newCursor = currentItem.clone();
                    }
                }

                currentItem = toPlace;
                // Update cursor server-side to prevent dupe
                player.setItemOnCursor(newCursor);
                fireOnChange(player, currentItem);
            }

            // ── Player taking the item from the slot ──────────────
            case PICKUP_ALL, PICKUP_ONE, PICKUP_HALF, PICKUP_SOME -> {
                if (currentItem == null) break;

                if (event.getAction() == org.bukkit.event.inventory.InventoryAction.PICKUP_ONE) {
                    ItemStack taken = currentItem.clone();
                    taken.setAmount(1);
                    player.setItemOnCursor(taken);
                    if (currentItem.getAmount() > 1) {
                        currentItem.setAmount(currentItem.getAmount() - 1);
                    } else {
                        currentItem = null;
                    }
                } else if (event.getAction() == org.bukkit.event.inventory.InventoryAction.PICKUP_HALF) {
                    int half = (int) Math.ceil(currentItem.getAmount() / 2.0);
                    ItemStack taken = currentItem.clone();
                    taken.setAmount(half);
                    player.setItemOnCursor(taken);
                    int remaining = currentItem.getAmount() - half;
                    if (remaining <= 0) {
                        currentItem = null;
                    } else {
                        currentItem.setAmount(remaining);
                    }
                } else {
                    // PICKUP_ALL / PICKUP_SOME → take everything
                    player.setItemOnCursor(currentItem.clone());
                    currentItem = null;
                }
                fireOnChange(player, currentItem);
            }

            // ── Shift-click from player inventory into this slot ──
            case MOVE_TO_OTHER_INVENTORY -> {
                // This fires when shift-clicking from player inv into GUI —
                // already cancelled; nothing to do extra.
            }

            // ── Double-click / collect — block to prevent dupes ───
            case COLLECT_TO_CURSOR -> { /* blocked */ }

            default -> { /* any other action is already cancelled */ }
        }

        return true;
    }

    /**
     * Returns the item that should be rendered in the inventory for this slot right now.
     * Shows the real item if present, otherwise the placeholder.
     */
    ItemStack getRenderedItem() {
        if (currentItem != null) return currentItem.clone();
        return placeholder != null ? placeholder.clone() : null;
    }

    private void fireOnChange(Player player, ItemStack newItem) {
        if (onChange != null) {
            onChange.accept(player, newItem != null ? newItem.clone() : null);
        }
    }
}
