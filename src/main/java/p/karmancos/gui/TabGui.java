package p.karmancos.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced TabGui with dynamic tab management and smooth transitions
 */
public class TabGui extends BaseGui {

    private final Map<Integer, BaseGui> tabs;
    private final Map<Integer, String> tabNames;
    private int currentTab = 0;
    private int[] tabSlots;
    private final List<GuiItem> tabIcons;
    private Material selectedTabMaterial = Material.LIME_STAINED_GLASS_PANE;
    private Material unselectedTabMaterial = Material.GRAY_STAINED_GLASS_PANE;
    private boolean closeOnTabSwitch = false;

    public TabGui(int rows, Component title) {
        super(rows, title);
        this.tabs = new HashMap<>();
        this.tabNames = new HashMap<>();
        this.tabIcons = new ArrayList<>();

        // Default tab slots (top row)
        this.tabSlots = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    }

    public TabGui(int rows, String title) {
        super(rows, title);
        this.tabs = new HashMap<>();
        this.tabNames = new HashMap<>();
        this.tabIcons = new ArrayList<>();

        // Default tab slots (top row)
        this.tabSlots = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    }

    /**
     * Add a tab with GUI and icon
     */
    public void addTab(int index, BaseGui gui, GuiItem icon, String name) {
        tabs.put(index, gui);
        tabNames.put(index, name);

        while (tabIcons.size() <= index) {
            tabIcons.add(null);
        }
        tabIcons.set(index, icon);
    }

    /**
     * Add a tab with automatic icon generation
     */
    public void addTab(int index, BaseGui gui, String name) {
        GuiItem icon = new GuiItem(new ItemBuilder(Material.PAPER)
                .setDisplayName("&e" + name));
        addTab(index, gui, icon, name);
    }

    /**
     * Remove a tab
     */
    public void removeTab(int index) {
        tabs.remove(index);
        tabNames.remove(index);
        if (index < tabIcons.size()) {
            tabIcons.set(index, null);
        }

        // Switch to first available tab if current was removed
        if (index == currentTab && !tabs.isEmpty()) {
            currentTab = tabs.keySet().stream().findFirst().orElse(0);
        }
    }

    /**
     * Get the GUI for a specific tab
     */
    public BaseGui getTab(int index) {
        return tabs.get(index);
    }

    /**
     * Get the current tab index
     */
    public int getCurrentTab() {
        return currentTab;
    }

    /**
     * Get tab count
     */
    public int getTabCount() {
        return tabs.size();
    }

    public void setTabSlots(int... slots) {
        this.tabSlots = slots;
    }

    public void setSelectedTabMaterial(Material material) {
        this.selectedTabMaterial = material;
    }

    public void setUnselectedTabMaterial(Material material) {
        this.unselectedTabMaterial = material;
    }

    public void setCloseOnTabSwitch(boolean close) {
        this.closeOnTabSwitch = close;
    }

    @Override
    public void open(Player player) {
        openTab(player, currentTab);
    }

    /**
     * Open a specific tab for a player
     */
    public void openTab(Player player, int tabIndex) {
        if (!tabs.containsKey(tabIndex)) return;

        if (closeOnTabSwitch && currentTab != tabIndex) {
            player.closeInventory();
        }

        currentTab = tabIndex;
        BaseGui gui = tabs.get(tabIndex);

        // Add tab buttons to the target GUI
        updateTabButtons(gui, player);

        gui.open(player);
    }

    /**
     * Switch to next tab
     */
    public void nextTab(Player player) {
        List<Integer> tabIndices = new ArrayList<>(tabs.keySet());
        tabIndices.sort(Integer::compareTo);

        int currentIndex = tabIndices.indexOf(currentTab);
        if (currentIndex < tabIndices.size() - 1) {
            openTab(player, tabIndices.get(currentIndex + 1));
        } else if (!tabIndices.isEmpty()) {
            // Wrap to first tab
            openTab(player, tabIndices.getFirst());
        }
    }

    /**
     * Switch to previous tab
     */
    public void previousTab(Player player) {
        List<Integer> tabIndices = new ArrayList<>(tabs.keySet());
        tabIndices.sort(Integer::compareTo);

        int currentIndex = tabIndices.indexOf(currentTab);
        if (currentIndex > 0) {
            openTab(player, tabIndices.get(currentIndex - 1));
        } else if (!tabIndices.isEmpty()) {
            // Wrap to last tab
            openTab(player, tabIndices.getLast());
        }
    }

    private void updateTabButtons(BaseGui gui, Player player) {
        for (int i = 0; i < tabIcons.size() && i < tabSlots.length; i++) {
            GuiItem icon = tabIcons.get(i);
            if (icon != null && tabs.containsKey(i)) {
                final int targetTab = i;

                // Create tab button
                ItemStack baseItem = icon.getItemStack(player);
                Material tabMaterial = (i == currentTab) ? selectedTabMaterial : unselectedTabMaterial;

                ItemBuilder builder = new ItemBuilder(baseItem != null ? baseItem : new ItemStack(tabMaterial));
                String tabName = tabNames.getOrDefault(i, "Tab " + (i + 1));

                if (i == currentTab) {
                    builder.setDisplayName("&a&l" + tabName + " &7(Actual)");
                    builder.addEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
                    builder.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                } else {
                    builder.setDisplayName("&7" + tabName);
                    builder.addLoreLines("&eClick para cambiar");
                }

                GuiItem tabButton = new GuiItem(builder.build(), event -> {
                    if (targetTab != currentTab) {
                        openTab(player, targetTab);
                    }
                });

                gui.setItem(tabSlots[i], tabButton);
            }
        }
    }

    /**
     * Update all tabs for all viewers
     */
    public void updateAllTabs() {
        BaseGui currentGui = tabs.get(currentTab);
        if (currentGui != null) {
            currentGui.update();
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        for (BaseGui tab : tabs.values()) {
            if (tab != null) {
                tab.cleanup();
            }
        }
    }
}

