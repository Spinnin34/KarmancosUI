package p.karmancos.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.banner.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionType;
import p.karmancos.gui.utils.ColorTranslator;

import java.util.*;
import java.util.function.Consumer;

/**
 * Enhanced ItemBuilder with fluent API, cloning support, and wide compatibility.
 * <p>
 * Supports: display name, lore, enchantments, item flags, skull owners,
 * leather armor colors, potions, banners, fireworks, custom model data,
 * unbreakable, amount, glow effect, and consumer-based meta modification.
 */
public class ItemBuilder {

    private final ItemStack item;

    // ─── Constructors ───────────────────────────────────────────

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, Math.max(1, Math.min(64, amount)));
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
    }

    // ─── Clone ──────────────────────────────────────────────────

    /**
     * Clone this builder to create a new independent copy.
     */
    public ItemBuilder clone() {
        return new ItemBuilder(item);
    }

    // ─── Display name ───────────────────────────────────────────

    public ItemBuilder setDisplayName(Component name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder setDisplayName(String name) {
        return setDisplayName(ColorTranslator.translate(name));
    }

    // ─── Lore ───────────────────────────────────────────────────

    public ItemBuilder setLore(List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        return setLore(ColorTranslator.translateList(Arrays.asList(lore)));
    }

    public ItemBuilder setLoreFromStrings(List<String> lore) {
        return setLore(ColorTranslator.translateList(lore));
    }

    public ItemBuilder addLoreLines(List<Component> lines) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.lore())) : new ArrayList<>();
            lore.addAll(lines);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder addLoreLines(String... lines) {
        return addLoreLines(ColorTranslator.translateList(Arrays.asList(lines)));
    }

    public ItemBuilder addLoreLine(String line) {
        return addLoreLines(line);
    }

    public ItemBuilder addLoreLine(Component line) {
        return addLoreLines(Collections.singletonList(line));
    }

    /**
     * Insert a lore line at a specific index.
     */
    public ItemBuilder insertLoreLine(int index, String line) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.lore())) : new ArrayList<>();
            lore.add(Math.min(index, lore.size()), ColorTranslator.translate(line));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return this;
    }

    /**
     * Clear all lore.
     */
    public ItemBuilder clearLore() {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(null);
            item.setItemMeta(meta);
        }
        return this;
    }

    // ─── Amount ─────────────────────────────────────────────────

    public ItemBuilder setAmount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    // ─── Enchantments ───────────────────────────────────────────

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        item.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ItemBuilder removeEnchantment(Enchantment enchantment) {
        item.removeEnchantment(enchantment);
        return this;
    }

    /**
     * Add glow effect (enchantment + hide enchants flag).
     */
    public ItemBuilder setGlowing(boolean glowing) {
        if (glowing) {
            addEnchantment(Enchantment.UNBREAKING, 1);
            addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            removeEnchantment(Enchantment.UNBREAKING);
        }
        return this;
    }

    // ─── Item flags ─────────────────────────────────────────────

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder removeItemFlags(ItemFlag... flags) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.removeItemFlags(flags);
            item.setItemMeta(meta);
        }
        return this;
    }

    /**
     * Hide all item flags (enchants, attributes, unbreakable, etc.)
     */
    public ItemBuilder hideAllFlags() {
        return addItemFlags(ItemFlag.values());
    }

    // ─── Unbreakable ────────────────────────────────────────────

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
            item.setItemMeta(meta);
        }
        return this;
    }

    // ─── Custom model data ──────────────────────────────────────

    public ItemBuilder setCustomModelData(int data) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(data);
            item.setItemMeta(meta);
        }
        return this;
    }

    // ─── Skull ──────────────────────────────────────────────────

    public ItemBuilder setSkullOwner(OfflinePlayer player) {
        if (item.getType() == Material.PLAYER_HEAD && item.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            item.setItemMeta(meta);
        }
        return this;
    }

    // ─── Leather armor ──────────────────────────────────────────

    public ItemBuilder setLeatherColor(Color color) {
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return this;
    }

    // ─── Potions ────────────────────────────────────────────────

    public ItemBuilder setPotionType(PotionType type) {
        if (item.getItemMeta() instanceof PotionMeta meta) {
            try {
                meta.setBasePotionType(type);
            } catch (NoSuchMethodError e) {
                // Fallback for older versions
            }
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder setPotionColor(Color color) {
        if (item.getItemMeta() instanceof PotionMeta meta) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return this;
    }

    // ─── Banners ────────────────────────────────────────────────

    public ItemBuilder addBannerPattern(Pattern pattern) {
        if (item.getItemMeta() instanceof BannerMeta meta) {
            meta.addPattern(pattern);
            item.setItemMeta(meta);
        }
        return this;
    }

    // ─── Fireworks ──────────────────────────────────────────────

    public ItemBuilder setFireworkPower(int power) {
        if (item.getItemMeta() instanceof FireworkMeta meta) {
            meta.setPower(power);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder addFireworkEffect(FireworkEffect effect) {
        if (item.getItemMeta() instanceof FireworkMeta meta) {
            meta.addEffect(effect);
            item.setItemMeta(meta);
        }
        return this;
    }

    // ─── Generic meta modification ──────────────────────────────

    /**
     * Modify the ItemMeta directly using a consumer for maximum flexibility.
     */
    public ItemBuilder modifyMeta(Consumer<ItemMeta> modifier) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            modifier.accept(meta);
            item.setItemMeta(meta);
        }
        return this;
    }

    /**
     * Modify a specific type of ItemMeta.
     */
    @SuppressWarnings("unchecked")
    public <T extends ItemMeta> ItemBuilder modifyMeta(Class<T> metaClass, Consumer<T> modifier) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && metaClass.isInstance(meta)) {
            modifier.accept((T) meta);
            item.setItemMeta(meta);
        }
        return this;
    }

    // ─── Placeholders ───────────────────────────────────────────

    /**
     * Replace placeholders in the display name and lore.
     */
    public ItemBuilder replacePlaceholder(String placeholder, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return this;

        // Replace in display name
        if (meta.hasDisplayName()) {
            @SuppressWarnings("deprecation")
            String name = meta.getDisplayName().replace(placeholder, value);
            meta.displayName(ColorTranslator.translate(name));
        }

        // Replace in lore
        if (meta.hasLore() && meta.lore() != null) {
            List<Component> newLore = new ArrayList<>();
            for (Component line : meta.lore()) {
                // Serialize, replace, and re-deserialize
                String serialized = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                        .plainText().serialize(line);
                if (serialized.contains(placeholder)) {
                    newLore.add(ColorTranslator.translate(serialized.replace(placeholder, value)));
                } else {
                    newLore.add(line);
                }
            }
            meta.lore(newLore);
        }

        item.setItemMeta(meta);
        return this;
    }

    // ─── Build ──────────────────────────────────────────────────

    public ItemStack build() {
        return item;
    }
}
