package p.karmancos.gui.bedrock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import p.karmancos.gui.BaseGui;
import p.karmancos.gui.GuiItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Converts regular KarmancosUI inventory menus into Bedrock SimpleForm data.
 */
public final class BedrockMenuTranslator {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private BedrockMenuTranslator() {
    }

    public static TranslatedMenu translate(BaseGui gui, Player player, BedrockFormOptions options) {
        BedrockFormOptions safeOptions = options == null ? BedrockFormOptions.defaults() : options;
        String title = safeOptions.translate(PLAIN.serialize(gui.getTitle()));
        List<BedrockFormButton> buttons = new ArrayList<>();

        gui.getItems().entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(entry -> {
                    GuiItem item = entry.getValue();
                    ItemStack stack;
                    try {
                        stack = item.getItemStack(player);
                    } catch (RuntimeException ex) {
                        return;
                    }
                    if (stack == null || stack.getType().isAir()) {
                        return;
                    }

                    String text = buildButtonText(entry.getKey(), stack, safeOptions);
                    if (!text.isBlank()) {
                        buttons.add(new BedrockFormButton(entry.getKey(), text, item));
                    }
                });

        return new TranslatedMenu(title, safeOptions.translate(safeOptions.getContent()), buttons);
    }

    private static String buildButtonText(int slot, ItemStack stack, BedrockFormOptions options) {
        StringBuilder text = new StringBuilder();
        if (options.isIncludeSlotNumbers()) {
            text.append("#").append(slot).append(" ");
        }

        text.append(options.translate(resolveDisplayName(stack)));

        if (options.isIncludeLore()) {
            List<String> lore = resolveLore(stack);
            for (String line : lore) {
                if (!line.isBlank()) {
                    text.append('\n').append(options.translate(line));
                }
            }
        }

        return text.toString();
    }

    private static String resolveDisplayName(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            Component displayName = meta.displayName();
            if (displayName != null) {
                String plain = PLAIN.serialize(displayName);
                if (!plain.isBlank()) {
                    return plain;
                }
            }
            if (meta.hasDisplayName()) {
                return strip(meta.getDisplayName());
            }
        }

        Material type = stack.getType();
        String readable = type.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
    }

    private static List<String> resolveLore(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return List.of();
        }

        List<String> lore = new ArrayList<>();
        List<Component> componentLore = meta.lore();
        if (componentLore != null) {
            for (Component line : componentLore) {
                lore.add(PLAIN.serialize(line));
            }
            return lore;
        }

        if (meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                lore.add(strip(line));
            }
        }
        return lore;
    }

    private static String strip(String text) {
        String stripped = ChatColor.stripColor(text);
        return stripped == null ? "" : stripped;
    }

    public record TranslatedMenu(String title, String content, List<BedrockFormButton> buttons) {
    }
}
