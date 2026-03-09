package p.karmancos.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ScrollGui extends BaseGui {

    private final List<GuiItem> allItems;
    private int scroll = 0;
    private int[] contentSlots;
    private GuiItem scrollUpItem;
    private GuiItem scrollDownItem;
    private GuiItem scrollBarItem;
    private int scrollUpSlot = -1;
    private int scrollDownSlot = -1;
    private int[] scrollBarSlots;
    private int itemsPerPage;
    private int scrollStep = 9; // Scroll one row by default

    public ScrollGui(int rows, Component title) {
        super(rows, title);
        this.allItems = new ArrayList<>();

        // Default content slots (all except right column)
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < 8; j++) {
                slots.add(i * 9 + j);
            }
        }
        this.contentSlots = slots.stream().mapToInt(j -> j).toArray();
        this.itemsPerPage = contentSlots.length;

        // Default scroll bar (right column)
        scrollBarSlots = new int[rows];
        for (int i = 0; i < rows; i++) {
            scrollBarSlots[i] = i * 9 + 8;
        }
    }

    public ScrollGui(int rows, String title) {
        super(rows, title);
        this.allItems = new ArrayList<>();

        // Default content slots (all except right column)
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < 8; j++) {
                slots.add(i * 9 + j);
            }
        }
        this.contentSlots = slots.stream().mapToInt(i -> i).toArray();
        this.itemsPerPage = contentSlots.length;

        // Default scroll bar slots (right column)
        List<Integer> barSlots = new ArrayList<>();
        for (int i = 1; i < rows - 1; i++) {
            barSlots.add(i * 9 + 8);
        }
        this.scrollBarSlots = barSlots.stream().mapToInt(i -> i).toArray();
    }

    public void addContent(GuiItem item) {
        allItems.add(item);
    }

    public void setContent(List<GuiItem> items) {
        allItems.clear();
        allItems.addAll(items);
        scroll = 0; // Reset scroll when content changes
    }

    public void clearContent() {
        allItems.clear();
        scroll = 0;
    }

    public void setContentSlots(int... slots) {
        this.contentSlots = slots;
        this.itemsPerPage = slots.length;
    }

    public void setScrollStep(int step) {
        this.scrollStep = Math.max(1, step);
    }

    public void setScrollControls(int upSlot, GuiItem upItem, int downSlot, GuiItem downItem) {
        this.scrollUpSlot = upSlot;
        this.scrollUpItem = upItem;
        this.scrollDownSlot = downSlot;
        this.scrollDownItem = downItem;
    }

    public void setScrollBar(int[] barSlots, GuiItem barItem) {
        this.scrollBarSlots = barSlots;
        this.scrollBarItem = barItem;
    }

    public void setScrollControlsDefault() {
        int size = inventory.getSize();
        this.scrollUpSlot = 8;
        this.scrollDownSlot = size - 1;

        this.scrollUpItem = new GuiItem(new ItemBuilder(Material.ARROW)
                .setDisplayName("&aDesplazar arriba"), event -> {
            scrollUp();
        });

        this.scrollDownItem = new GuiItem(new ItemBuilder(Material.ARROW)
                .setDisplayName("&aDesplazar abajo"), event -> {
            scrollDown();
        });

        this.scrollBarItem = new GuiItem(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName("&7Scroll"));
    }

    /**
     * Scroll up
     */
    public void scrollUp() {
        if (scroll > 0) {
            scroll = Math.max(0, scroll - scrollStep);
            updateScroll();
        }
    }

    /**
     * Scroll down
     */
    public void scrollDown() {
        int maxScroll = Math.max(0, allItems.size() - itemsPerPage);
        if (scroll < maxScroll) {
            scroll = Math.min(maxScroll, scroll + scrollStep);
            updateScroll();
        }
    }

    /**
     * Scroll to a specific item index
     */
    public void scrollToItem(int index) {
        if (index < 0 || index >= allItems.size()) return;

        scroll = Math.max(0, Math.min(index, allItems.size() - itemsPerPage));
        updateScroll();
    }

    /**
     * Get current scroll position
     */
    public int getScrollPosition() {
        return scroll;
    }

    /**
     * Get max scroll position
     */
    public int getMaxScroll() {
        return Math.max(0, allItems.size() - itemsPerPage);
    }

    @Override
    public void open(Player player) {
        updateScroll();
        super.open(player);
    }

    public void updateScroll() {
        // Clear content slots
        for (int slot : contentSlots) {
            inventory.setItem(slot, null);
            items.remove(slot);
        }

        int endIndex = Math.min(scroll + itemsPerPage, allItems.size());

        for (int i = scroll; i < endIndex; i++) {
            int slotIndex = i - scroll;
            if (slotIndex < contentSlots.length) {
                setItem(contentSlots[slotIndex], allItems.get(i));
            }
        }

        // Update scroll controls
        updateScrollControls();

        // Update scroll bar
        updateScrollBar();
    }

    private void updateScrollControls() {
        if (scrollUpSlot != -1 && scrollUpItem != null) {
            if (scroll > 0) {
                setItem(scrollUpSlot, scrollUpItem);
            } else {
                inventory.setItem(scrollUpSlot, null);
                items.remove(scrollUpSlot);
            }
        }

        if (scrollDownSlot != -1 && scrollDownItem != null) {
            int maxScroll = Math.max(0, allItems.size() - itemsPerPage);
            if (scroll < maxScroll) {
                setItem(scrollDownSlot, scrollDownItem);
            } else {
                inventory.setItem(scrollDownSlot, null);
                items.remove(scrollDownSlot);
            }
        }
    }

    private void updateScrollBar() {
        if (scrollBarSlots == null || scrollBarSlots.length == 0) return;
        if (scrollBarItem == null) return;

        // Clear scroll bar
        for (int slot : scrollBarSlots) {
            inventory.setItem(slot, null);
            items.remove(slot);
        }

        int maxScroll = Math.max(0, allItems.size() - itemsPerPage);
        if (maxScroll == 0) return;

        // Calculate scroll bar position
        float scrollPercent = (float) scroll / maxScroll;
        int barPosition = Math.round(scrollPercent * (scrollBarSlots.length - 1));

        // Set active scroll bar indicator
        if (barPosition >= 0 && barPosition < scrollBarSlots.length) {
            GuiItem activeBar = new GuiItem(new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .setDisplayName("&aScroll: " + (int)(scrollPercent * 100) + "%"));
            setItem(scrollBarSlots[barPosition], activeBar);
        }

        // Fill rest with inactive bars
        for (int i = 0; i < scrollBarSlots.length; i++) {
            if (i != barPosition) {
                setItem(scrollBarSlots[i], scrollBarItem);
            }
        }
    }
}

