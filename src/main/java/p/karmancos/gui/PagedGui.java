package p.karmancos.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * PagedGui with fluent builder, structure support, and lazy loading.
 * <p>
 * Usage example:
 * <pre>{@code
 * PagedGui gui = PagedGui.builder()
 *     .setRows(6)
 *     .setTitle("&eMy GUI")
 *     .setStructure(
 *         "# # # # # # # # #",
 *         "# . . . . . . . #",
 *         "# . . . . . . . #",
 *         "# . . . . . . . #",
 *         "# . . . . . . . #",
 *         "# # # < P > # # #"
 *     )
 *     .addIngredient('#', new GuiItem(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("&7")))
 *     .setContent(myItems)
 *     .build();
 * gui.open(player);
 * }</pre>
 * <p>
 * Navigation buttons are auto-wired when using structure markers {@code <}, {@code >} and {@code P}.
 * These markers are <b>optional</b>; omitting them simply disables those buttons.
 * Custom items can be provided via {@link Builder#setPreviousPageItem(GuiItem)},
 * {@link Builder#setNextPageItem(GuiItem)} and {@link Builder#setPageInfoItem(GuiItem)},
 * or their lightweight counterparts (name, lore, material).
 */
public class PagedGui extends BaseGui {

    private final List<GuiItem> allItems;
    private int page = 0;
    private int[] contentSlots;
    private GuiItem previousPageItem;
    private GuiItem nextPageItem;
    private GuiItem pageInfoItem;
    private int previousPageSlot = -1;
    private int nextPageSlot = -1;
    private int pageInfoSlot = -1;
    private int maxPage = 0;
    private boolean alwaysShowPrevButton = false;
    private boolean alwaysShowNextButton = false;

    // ─── Constructors ───────────────────────────────────────────

    public PagedGui(int rows, Component title) {
        super(rows, title);
        this.allItems = new ArrayList<>();
        initDefaultContentSlots();
    }

    public PagedGui(int rows, String title) {
        super(rows, title);
        this.allItems = new ArrayList<>();
        initDefaultContentSlots();
    }

    private void initDefaultContentSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < (rows - 1) * 9; i++) {
            slots.add(i);
        }
        this.contentSlots = slots.stream().mapToInt(i -> i).toArray();
    }

    // ─── Content management ─────────────────────────────────────

    /**
     * Add a single content item. Does NOT auto-refresh the page;
     * call {@link #updatePage()} afterwards if the GUI is already open.
     */
    public void addContent(GuiItem item) {
        allItems.add(item);
        recalcMaxPage();
    }

    /**
     * Replace all content items with the given list and reset to page 0.
     * If the GUI is already open (has viewers), the page is automatically refreshed.
     */
    public void setContent(List<GuiItem> items) {
        allItems.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        recalcMaxPage();
        // Always reset to page 0 when content changes to avoid stale page index
        page = 0;
        // Auto-refresh if the GUI is already being viewed
        if (!viewers.isEmpty()) {
            updatePage();
        }
    }

    /**
     * Clear all content and reset to page 0.
     * If the GUI is already open (has viewers), the page is automatically refreshed.
     */
    public void clearContent() {
        allItems.clear();
        recalcMaxPage();
        page = 0;
        if (!viewers.isEmpty()) {
            updatePage();
        }
    }

    public List<GuiItem> getContent() {
        return new ArrayList<>(allItems);
    }

    public int getContentSize() {
        return allItems.size();
    }

    public void setContentSlots(int... slots) {
        this.contentSlots = slots;
        recalcMaxPage();
    }

    private void recalcMaxPage() {
        if (contentSlots.length == 0 || allItems.isEmpty()) {
            maxPage = 0;
        } else {
            maxPage = (int) Math.ceil((double) allItems.size() / contentSlots.length) - 1;
        }
        // Clamp current page so it never exceeds the new max
        if (page > maxPage) {
            page = maxPage;
        }
    }

    // ─── Navigation ─────────────────────────────────────────────

    /**
     * Configure both navigation buttons at once.
     * Navigation actions are auto-wired through the slot+item setters.
     */
    public void setNavigation(int prevSlot, GuiItem prevItem, int nextSlot, GuiItem nextItem) {
        setPreviousPageItem(prevSlot, prevItem);
        setNextPageItem(nextSlot, nextItem);
    }

    /**
     * Set the previous page button item and slot.
     * The navigation action is automatically applied; any existing action on the item is preserved and called first.
     */
    public void setPreviousPageItem(int slot, GuiItem item) {
        this.previousPageSlot = slot;
        wireNavigationAction(item, true);
        this.previousPageItem = item;
    }

    /**
     * Set the next page button item and slot.
     * The navigation action is automatically applied; any existing action on the item is preserved and called first.
     */
    public void setNextPageItem(int slot, GuiItem item) {
        this.nextPageSlot = slot;
        wireNavigationAction(item, false);
        this.nextPageItem = item;
    }

    /**
     * Replace the previous page GuiItem (slot must be set separately or already configured).
     * Navigation action is auto-wired; any pre-existing action on the item is preserved and called first.
     */
    public void setPreviousPageItem(GuiItem item) {
        if (item == null) { this.previousPageItem = null; return; }
        wireNavigationAction(item, true);
        this.previousPageItem = item;
    }

    /**
     * Replace the next page GuiItem (slot must be set separately or already configured).
     * Navigation action is auto-wired; any pre-existing action on the item is preserved and called first.
     */
    public void setNextPageItem(GuiItem item) {
        if (item == null) { this.nextPageItem = null; return; }
        wireNavigationAction(item, false);
        this.nextPageItem = item;
    }

    /**
     * Wire the navigation action onto the given item.
     * @param item the GuiItem to wire
     * @param previous true = previousPage(), false = nextPage()
     */
    private void wireNavigationAction(GuiItem item, boolean previous) {
        if (item == null) return;
        Consumer<InventoryClickEvent> originalAction = item.getAction();
        if (previous) {
            item.onClick(event -> {
                if (originalAction != null) originalAction.accept(event);
                previousPage();
            });
        } else {
            item.onClick(event -> {
                if (originalAction != null) originalAction.accept(event);
                nextPage();
            });
        }
    }

    /**
     * Whether to always show the previous page button even on page 0.
     */
    public void setAlwaysShowPrevButton(boolean alwaysShow) {
        this.alwaysShowPrevButton = alwaysShow;
    }

    /**
     * Whether to always show the next page button even on the last page.
     */
    public void setAlwaysShowNextButton(boolean alwaysShow) {
        this.alwaysShowNextButton = alwaysShow;
    }

    /**
     * Set the page info item and slot.
     */
    public void setPageInfo(int slot, GuiItem infoItem) {
        this.pageInfoSlot = slot;
        this.pageInfoItem = infoItem;
    }

    /**
     * Automatically configure default navigation buttons using the last row.
     * Previous button at slot (size-9), next button at slot (size-1), info at slot (size-5).
     */
    public void setNavigationDefault() {
        int size = inventory.getSize();

        setPreviousPageItem(size - 9, new GuiItem(new ItemBuilder(Material.ARROW)
                .setDisplayName("&aPágina anterior")));

        setNextPageItem(size - 1, new GuiItem(new ItemBuilder(Material.ARROW)
                .setDisplayName("&aPágina siguiente")));

        this.pageInfoSlot = size - 5;
        this.pageInfoItem = new GuiItem(player -> {
            int totalPages = getTotalPages();
            return new ItemBuilder(Material.PAPER)
                    .setDisplayName("&ePágina " + (page + 1) + " / " + totalPages)
                    .addLoreLines(
                            "&7Items: &f" + allItems.size(),
                            "&7En página: &f" + getItemCountOnCurrentPage()
                    ).build();
        });
    }

    // ─── Page navigation ────────────────────────────────────────

    /**
     * Go to a specific page (0-based). Clamped to valid range.
     */
    public void goToPage(int targetPage) {
        int clamped = Math.max(0, Math.min(targetPage, maxPage));
        if (clamped == this.page) return; // no-op
        this.page = clamped;
        updatePage();
    }

    /**
     * Advance to the next page if available.
     */
    public void nextPage() {
        if (page < maxPage) {
            page++;
            updatePage();
        }
    }

    /**
     * Go back to the previous page if available.
     */
    public void previousPage() {
        if (page > 0) {
            page--;
            updatePage();
        }
    }

    public int getCurrentPage() {
        return page;
    }

    /**
     * Total number of pages (always &ge; 1).
     */
    public int getTotalPages() {
        return maxPage + 1;
    }

    public boolean hasNextPage() {
        return page < maxPage;
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    /**
     * Number of items displayed on the current page.
     */
    public int getItemCountOnCurrentPage() {
        int startIndex = page * contentSlots.length;
        return Math.max(0, Math.min(contentSlots.length, allItems.size() - startIndex));
    }

    // ─── Open / Update ──────────────────────────────────────────

    @Override
    public void open(Player player) {
        // Populate the inventory slots before super.open() which renders ItemProviders per-player
        updatePage();
        super.open(player);
    }

    /**
     * Re-render the current page: content slots, navigation buttons, and page info.
     */
    public void updatePage() {
        // ── 1. Clear content slots (respect InputSlots) ──────────────────
        for (int slot : contentSlots) {
            if (inputSlots.containsKey(slot)) continue;
            inventory.setItem(slot, null);
            items.remove(slot);
        }

        // ── 2. Place content items for the current page ──────────────────
        int startIndex = page * contentSlots.length;
        int endIndex = Math.min(startIndex + contentSlots.length, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            setItem(contentSlots[slotIndex], allItems.get(i));
        }

        // ── 3. Update navigation buttons ─────────────────────────────────
        updateNavigationButtons();

        // ── 4. Update page info item ─────────────────────────────────────
        updatePageInfoItem();
    }

    private void updateNavigationButtons() {
        // ── Previous button ──────────────────────────────────────────────
        if (previousPageSlot != -1) {
            if (previousPageItem != null && (page > 0 || alwaysShowPrevButton)) {
                setItem(previousPageSlot, previousPageItem);
            } else {
                inventory.setItem(previousPageSlot, null);
                items.remove(previousPageSlot);
            }
        }

        // ── Next button ──────────────────────────────────────────────────
        if (nextPageSlot != -1) {
            if (nextPageItem != null && (page < maxPage || alwaysShowNextButton)) {
                setItem(nextPageSlot, nextPageItem);
            } else {
                inventory.setItem(nextPageSlot, null);
                items.remove(nextPageSlot);
            }
        }
    }

    private void updatePageInfoItem() {
        if (pageInfoSlot != -1 && pageInfoItem != null) {
            setItem(pageInfoSlot, pageInfoItem);
        } else if (pageInfoSlot != -1) {
            // Slot configured but no item — clear it
            inventory.setItem(pageInfoSlot, null);
            items.remove(pageInfoSlot);
        }
    }

    // ─── Search ─────────────────────────────────────────────────

    /**
     * Search items by display name (case-insensitive).
     */
    public List<GuiItem> searchItems(String query) {
        List<GuiItem> results = new ArrayList<>();
        if (query == null || query.isEmpty()) return results;
        String lowerQuery = query.toLowerCase();

        for (GuiItem item : allItems) {
            try {
                ItemStack is = item.getItemStack(null);
                if (is != null && is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
                    @SuppressWarnings("deprecation")
                    String displayName = is.getItemMeta().getDisplayName().toLowerCase();
                    if (displayName.contains(lowerQuery)) {
                        results.add(item);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return results;
    }

    /**
     * Search and show results as new content, resetting to page 0.
     * Does nothing if no results are found.
     */
    public void searchAndShow(String query) {
        List<GuiItem> results = searchItems(query);
        if (!results.isEmpty()) {
            allItems.clear();
            allItems.addAll(results);
            recalcMaxPage();
            page = 0;
            updatePage();
        }
    }

    // ─── Fluent Builder ─────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int rows = 6;
        private String title = "&8Menu";
        private Component titleComponent = null;
        private String[] structure = null;
        private final Map<Character, GuiItem> ingredients = new HashMap<>();
        private char contentMarker = '.';
        private char prevPageMarker = '<';
        private char nextPageMarker = '>';
        private char pageInfoMarker = 'P';
        private List<GuiItem> content = null;

        // Custom nav items — null = use default arrow/paper
        private GuiItem prevPageItem = null;
        private GuiItem nextPageItem = null;
        private GuiItem pageInfoItem = null;

        // Lore customization for default nav items
        private List<String> prevPageLore = null;
        private List<String> nextPageLore = null;
        private List<String> pageInfoLore = null;

        // Display name customization for default nav items
        private String prevPageName = null;
        private String nextPageName = null;
        private String pageInfoName = null;

        // Material customization for default nav items
        private Material prevPageMaterial = Material.ARROW;
        private Material nextPageMaterial = Material.ARROW;
        private Material pageInfoMaterial = Material.PAPER;

        private boolean autoNavigation = false;
        private boolean alwaysShowPrevButton = false;
        private boolean alwaysShowNextButton = false;

        public Builder setRows(int rows) {
            this.rows = Math.max(1, Math.min(6, rows));
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            this.titleComponent = null;
            return this;
        }

        public Builder setTitle(Component title) {
            this.titleComponent = title;
            return this;
        }

        /**
         * Set the layout structure. Each string is one row (9 chars, spaces ignored).
         * Default markers:
         * <ul>
         *   <li>{@code .} = content slot</li>
         *   <li>{@code #} = filler (requires ingredient)</li>
         *   <li>{@code <} = previous page button (optional)</li>
         *   <li>{@code >} = next page button (optional)</li>
         *   <li>{@code P} = page info (optional)</li>
         * </ul>
         * Markers {@code <}, {@code >} and {@code P} are optional — omitting them simply disables those buttons.
         */
        public Builder setStructure(String... structure) {
            this.structure = structure;
            this.rows = structure.length;
            return this;
        }

        /** Add an ingredient (char to GuiItem mapping for the structure). */
        public Builder addIngredient(char key, GuiItem item) {
            this.ingredients.put(key, item);
            return this;
        }

        /** Convenience: add ingredient from ItemBuilder. */
        public Builder addIngredient(char key, ItemBuilder builder) {
            return addIngredient(key, new GuiItem(builder));
        }

        /** Convenience: add ingredient from ItemStack. */
        public Builder addIngredient(char key, ItemStack itemStack) {
            return addIngredient(key, new GuiItem(itemStack));
        }

        /** Set the content marker character (default '.'). */
        public Builder setContentMarker(char marker) {
            this.contentMarker = marker;
            return this;
        }

        /** Set the previous page marker character (default '&lt;'). */
        public Builder setPrevPageMarker(char marker) {
            this.prevPageMarker = marker;
            return this;
        }

        /** Set the next page marker character (default '&gt;'). */
        public Builder setNextPageMarker(char marker) {
            this.nextPageMarker = marker;
            return this;
        }

        /** Set the page info marker character (default 'P'). */
        public Builder setPageInfoMarker(char marker) {
            this.pageInfoMarker = marker;
            return this;
        }

        public Builder setContent(List<GuiItem> content) {
            this.content = content;
            return this;
        }

        /**
         * Provide a fully custom previous-page button.
         * The navigation action will still be auto-wired on top of any action the item already has.
         */
        public Builder setPreviousPageItem(GuiItem item) {
            this.prevPageItem = item;
            return this;
        }

        /**
         * Provide a fully custom next-page button.
         * The navigation action will still be auto-wired on top of any action the item already has.
         */
        public Builder setNextPageItem(GuiItem item) {
            this.nextPageItem = item;
            return this;
        }

        /**
         * Provide a fully custom page-info item.
         * Tip: use an {@link ItemProvider} lambda so the text updates dynamically per page.
         */
        public Builder setPageInfoItem(GuiItem item) {
            this.pageInfoItem = item;
            return this;
        }

        // ── Lightweight customization of the DEFAULT nav items ──────────────

        /**
         * Override the display name of the default previous-page button (ignored if a custom GuiItem is set).
         */
        public Builder setPrevPageName(String name) {
            this.prevPageName = name;
            return this;
        }

        /**
         * Override the display name of the default next-page button (ignored if a custom GuiItem is set).
         */
        public Builder setNextPageName(String name) {
            this.nextPageName = name;
            return this;
        }

        /**
         * Override the display name of the default page-info item (ignored if a custom GuiItem is set).
         * Supports placeholders: {@code %page%}, {@code %total%}, {@code %size%}.
         */
        public Builder setPageInfoName(String name) {
            this.pageInfoName = name;
            return this;
        }

        /**
         * Set lore lines for the default previous-page button (ignored if a custom GuiItem is set).
         */
        public Builder setPrevPageLore(String... lore) {
            this.prevPageLore = List.of(lore);
            return this;
        }

        /**
         * Set lore lines for the default next-page button (ignored if a custom GuiItem is set).
         */
        public Builder setNextPageLore(String... lore) {
            this.nextPageLore = List.of(lore);
            return this;
        }

        /**
         * Set lore lines for the default page-info item (ignored if a custom GuiItem is set).
         * Supports placeholders: {@code %page%}, {@code %total%}, {@code %size%}, {@code %perpage%}.
         */
        public Builder setPageInfoLore(String... lore) {
            this.pageInfoLore = List.of(lore);
            return this;
        }

        /**
         * Override the material of the default previous-page button (ignored if a custom GuiItem is set).
         */
        public Builder setPrevPageMaterial(Material material) {
            this.prevPageMaterial = material;
            return this;
        }

        /**
         * Override the material of the default next-page button (ignored if a custom GuiItem is set).
         */
        public Builder setNextPageMaterial(Material material) {
            this.nextPageMaterial = material;
            return this;
        }

        /**
         * Override the material of the default page-info item (ignored if a custom GuiItem is set).
         */
        public Builder setPageInfoMaterial(Material material) {
            this.pageInfoMaterial = material;
            return this;
        }

        // ────────────────────────────────────────────────────────────────────

        /**
         * Enable automatic navigation buttons (only applies when no structure is set).
         */
        public Builder autoNavigation() {
            this.autoNavigation = true;
            return this;
        }

        /**
         * Always render the previous-page button even when on page 0.
         */
        public Builder alwaysShowPrevButton() {
            this.alwaysShowPrevButton = true;
            return this;
        }

        /**
         * Always render the next-page button even when on the last page.
         */
        public Builder alwaysShowNextButton() {
            this.alwaysShowNextButton = true;
            return this;
        }

        public PagedGui build() {
            PagedGui gui;
            if (titleComponent != null) {
                gui = new PagedGui(rows, titleComponent);
            } else {
                gui = new PagedGui(rows, title);
            }

            gui.setAlwaysShowPrevButton(alwaysShowPrevButton);
            gui.setAlwaysShowNextButton(alwaysShowNextButton);

            if (structure != null) {
                List<Integer> contentSlotsList = new ArrayList<>();
                int prevSlot = -1;
                int nextSlot = -1;
                int infoSlot = -1;
                int slot = 0;

                for (String row : structure) {
                    String cleaned = row.replace(" ", "");
                    for (int i = 0; i < cleaned.length() && slot < gui.inventory.getSize(); i++) {
                        char c = cleaned.charAt(i);
                        if (c == contentMarker) {
                            contentSlotsList.add(slot);
                        } else if (c == prevPageMarker) {
                            prevSlot = slot;
                        } else if (c == nextPageMarker) {
                            nextSlot = slot;
                        } else if (c == pageInfoMarker) {
                            infoSlot = slot;
                        } else if (ingredients.containsKey(c)) {
                            gui.setItem(slot, ingredients.get(c));
                        }
                        slot++;
                    }
                }

                gui.setContentSlots(contentSlotsList.stream().mapToInt(Integer::intValue).toArray());

                // ── Previous page button ─────────────────────────────────────
                if (prevSlot != -1) {
                    GuiItem prev;
                    if (prevPageItem != null) {
                        prev = prevPageItem;
                    } else {
                        String name = prevPageName != null ? prevPageName : "&aPágina anterior";
                        ItemBuilder ib = new ItemBuilder(prevPageMaterial).setDisplayName(name);
                        if (prevPageLore != null && !prevPageLore.isEmpty()) {
                            ib.addLoreLines(prevPageLore.toArray(new String[0]));
                        }
                        prev = new GuiItem(ib.build());
                    }
                    gui.setPreviousPageItem(prevSlot, prev);
                }

                // ── Next page button ─────────────────────────────────────────
                if (nextSlot != -1) {
                    GuiItem next;
                    if (nextPageItem != null) {
                        next = nextPageItem;
                    } else {
                        String name = nextPageName != null ? nextPageName : "&aPágina siguiente";
                        ItemBuilder ib = new ItemBuilder(nextPageMaterial).setDisplayName(name);
                        if (nextPageLore != null && !nextPageLore.isEmpty()) {
                            ib.addLoreLines(nextPageLore.toArray(new String[0]));
                        }
                        next = new GuiItem(ib.build());
                    }
                    gui.setNextPageItem(nextSlot, next);
                }

                // ── Page info button ─────────────────────────────────────────
                if (infoSlot != -1) {
                    gui.pageInfoSlot = infoSlot;
                    gui.pageInfoItem = pageInfoItem != null ? pageInfoItem : buildDefaultPageInfoItem(gui);
                }

            } else if (autoNavigation) {
                gui.setNavigationDefault();
            }

            // Set content last — this calls recalcMaxPage() and resets page to 0
            if (content != null && !content.isEmpty()) {
                gui.allItems.addAll(content);
                gui.recalcMaxPage();
            }

            return gui;
        }

        /**
         * Build the default page-info GuiItem using an ItemProvider so it evaluates dynamically.
         */
        private GuiItem buildDefaultPageInfoItem(PagedGui gui) {
            final String nameTemplate = pageInfoName != null ? pageInfoName : "&ePágina %page% / %total%";
            final List<String> loreTemplate = pageInfoLore != null
                    ? pageInfoLore
                    : List.of("&7Items: &f%size%");
            final Material mat = pageInfoMaterial;

            return new GuiItem(player -> {
                int totalPages = gui.getTotalPages();
                int currentPage = gui.getCurrentPage() + 1;
                int totalItems = gui.getContentSize();
                int perPage = gui.contentSlots.length;

                String resolvedName = applyPlaceholders(nameTemplate, currentPage, totalPages, totalItems, perPage);
                ItemBuilder ib = new ItemBuilder(mat).setDisplayName(resolvedName);

                if (!loreTemplate.isEmpty()) {
                    String[] resolvedLore = new String[loreTemplate.size()];
                    for (int i = 0; i < loreTemplate.size(); i++) {
                        resolvedLore[i] = applyPlaceholders(loreTemplate.get(i), currentPage, totalPages, totalItems, perPage);
                    }
                    ib.addLoreLines(resolvedLore);
                }

                return ib.build();
            });
        }

        private static String applyPlaceholders(String text, int page, int total, int size, int perPage) {
            return text
                    .replace("%page%", String.valueOf(page))
                    .replace("%total%", String.valueOf(total))
                    .replace("%size%", String.valueOf(size))
                    .replace("%perpage%", String.valueOf(perPage));
        }
    }
}
