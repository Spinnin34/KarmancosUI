package p.karmancos.gui;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Enhanced GuiItem with multi-click support, cooldowns, and sounds.
 */
public class GuiItem {

    private final ItemProvider itemProvider;
    private Consumer<InventoryClickEvent> action;
    private final Map<ClickType, Consumer<InventoryClickEvent>> clickActions = new HashMap<>();
    private Sound clickSound;
    private float clickSoundVolume = 1.0f;
    private float clickSoundPitch = 1.0f;
    private Consumer<Player> bedrockAction;
    private long cooldownMs = 0;
    private final Map<String, Long> lastClick = new HashMap<>();

    // ─── Constructors ───────────────────────────────────────────

    public GuiItem(ItemStack itemStack) {
        this(itemStack, null);
    }

    public GuiItem(ItemStack itemStack, Consumer<InventoryClickEvent> action) {
        this.itemProvider = player -> itemStack;
        this.action = action;
    }

    public GuiItem(ItemBuilder itemBuilder) {
        this(itemBuilder.build(), null);
    }

    public GuiItem(ItemBuilder itemBuilder, Consumer<InventoryClickEvent> action) {
        this(itemBuilder.build(), action);
    }

    public GuiItem(ItemProvider itemProvider) {
        this(itemProvider, null);
    }

    public GuiItem(ItemProvider itemProvider, Consumer<InventoryClickEvent> action) {
        this.itemProvider = itemProvider;
        this.action = action;
    }

    // ─── Item stack ─────────────────────────────────────────────

    public ItemStack getItemStack(Player player) {
        return itemProvider.getItem(player);
    }

    public ItemStack getItemStack() {
        return itemProvider.getItem(null);
    }

    // ─── Click handling ─────────────────────────────────────────

    /**
     * Set click action for a specific click type.
     */
    public GuiItem onClick(ClickType clickType, Consumer<InventoryClickEvent> action) {
        clickActions.put(clickType, action);
        return this;
    }

    /**
     * Set default click action.
     */
    public GuiItem onClick(Consumer<InventoryClickEvent> action) {
        this.action = action;
        return this;
    }

    /**
     * Get the default click action (may be null).
     */
    public Consumer<InventoryClickEvent> getAction() {
        return this.action;
    }

    /**
     * Set an action that can be executed from a Bedrock form.
     * <p>
     * Bedrock forms do not create Bukkit InventoryClickEvent instances, so
     * callbacks that need only the Player should be registered here.
     */
    public GuiItem onBedrockClick(Consumer<Player> action) {
        this.bedrockAction = action;
        return this;
    }

    /**
     * Get the Bedrock form action, or null when this item only supports Java inventory clicks.
     */
    public Consumer<Player> getBedrockAction() {
        return bedrockAction;
    }

    /**
     * Execute the Bedrock form action if one is registered.
     *
     * @return true if an action was executed
     */
    public boolean handleBedrockClick(Player player) {
        if (bedrockAction == null || player == null) {
            return false;
        }
        if (!passesCooldown(player)) {
            return true;
        }
        playClickSound(player);
        bedrockAction.accept(player);
        return true;
    }

    /**
     * Set click sound.
     */
    public GuiItem setClickSound(Sound sound) {
        this.clickSound = sound;
        return this;
    }

    /**
     * Set click sound with volume and pitch.
     */
    public GuiItem setClickSound(Sound sound, float volume, float pitch) {
        this.clickSound = sound;
        this.clickSoundVolume = volume;
        this.clickSoundPitch = pitch;
        return this;
    }

    /**
     * Set cooldown in milliseconds.
     */
    public GuiItem setCooldown(long cooldownMs) {
        this.cooldownMs = cooldownMs;
        return this;
    }

    /**
     * Handle click event with cooldown, sound, and multi-click support.
     */
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!passesCooldown(player)) {
            return;
        }

        playClickSound(player);

        // Try specific click type action first
        ClickType clickType = event.getClick();
        Consumer<InventoryClickEvent> specificAction = clickActions.get(clickType);
        if (specificAction != null) {
            specificAction.accept(event);
            return;
        }

        // Fallback to default action
        if (action != null) {
            action.accept(event);
        }
    }

    private boolean passesCooldown(Player player) {
        if (cooldownMs <= 0) {
            return true;
        }
        String key = player.getUniqueId().toString();
        long now = System.currentTimeMillis();
        Long last = lastClick.get(key);
        if (last != null && (now - last) < cooldownMs) {
            return false;
        }
        lastClick.put(key, now);
        return true;
    }

    private void playClickSound(Player player) {
        if (clickSound != null) {
            player.playSound(player.getLocation(), clickSound, clickSoundVolume, clickSoundPitch);
        }
    }
}
