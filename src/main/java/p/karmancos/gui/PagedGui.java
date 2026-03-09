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
 * PagedGui with fluent builder, structure support, and lazy loading.
 * <p>
 * Usage example:
 * <pre>
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
 * </pre>
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
    private int maxPage = -1;

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

    public void addContent(GuiItem item) {
        allItems.add(item);
        recalcMaxPage();
    }

    public void setContent(List<GuiItem> items) {
        allItems.clear();
        allItems.addAll(items);
        recalcMaxPage();
    }

    public void clearContent() {
        allItems.clear();
        maxPage = -1;
        page = 0;
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
        if (contentSlots.length == 0) {
            maxPage = -1;
        } else {
            maxPage = (int) Math.ceil((double) allItems.size() / contentSlots.length) - 1;
        }
    }

    // ─── Navigation ─────────────────────────────────────────────

    public void setNavigation(int prevSlot, GuiItem prevItem, int nextSlot, GuiItem nextItem) {
        this.previousPageSlot = prevSlot;
        this.previousPageItem = prevItem;
        this.nextPageSlot = nextSlot;
        this.nextPageItem = nextItem;
    }

    public void setPageInfo(int slot, GuiItem infoItem) {
        this.pageInfoSlot = slot;
        this.pageInfoItem = infoItem;
    }

    public void setNavigationDefault() {
        int size = inventory.getSize();
        this.previousPageSlot = size - 9;
        this.nextPageSlot = size - 1;

        this.previousPageItem = new GuiItem(new ItemBuilder(Material.ARROW)
                .setDisplayName("&aPágina anterior"), event -> {
            if (page > 0) {
                page--;
                updatePage();
            }
        });

        this.nextPageItem = new GuiItem(new ItemBuilder(Material.ARROW)
                .setDisplayName("&aPágina siguiente"), event -> {
            if (page < maxPage) {
                page++;
                updatePage();
            }
        });

        this.pageInfoSlot = size - 5;
        this.pageInfoItem = new GuiItem(player -> {
            int totalPages = Math.max(1, maxPage + 1);
            return new ItemBuilder(Material.PAPER)
                    .setDisplayName("&ePágina " + (page + 1) + " / " + totalPages)
                    .addLoreLines(
                            "&7Items: &f" + allItems.size(),
                            "&7En página: &f" + Math.min(contentSlots.length,
                                    Math.max(0, allItems.size() - (page * contentSlots.length)))
                    ).build();
        });
    }

    /**
     * Go to a specific page.
     */
    public void goToPage(int page) {
        if (page < 0) page = 0;
        if (page > maxPage && maxPage >= 0) page = maxPage;
        this.page = page;
        updatePage();
    }

    public void nextPage() {
        if (page < maxPage) {
            page++;
            updatePage();
        }
    }

    public void previousPage() {
        if (page > 0) {
            page--;
            updatePage();
        }
    }

    public int getCurrentPage() {
        return page;
    }

    public int getTotalPages() {
        return Math.max(1, maxPage + 1);
    }

    public boolean hasNextPage() {
        return page < maxPage;
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    // ─── Open / Update ──────────────────────────────────────────

    @Override
    public void open(Player player) {
        updatePage();
        super.open(player);
    }

    public void updatePage() {
        // Clear content slots
        for (int slot : contentSlots) {
            inventory.setItem(slot, null);
            items.remove(slot);
        }

        int startIndex = page * contentSlots.length;
        int endIndex = Math.min(startIndex + contentSlots.length, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex < contentSlots.length) {
                setItem(contentSlots[slotIndex], allItems.get(i));
            }
        }

        updateNavigationButtons();

        if (pageInfoSlot != -1 && pageInfoItem != null) {
            setItem(pageInfoSlot, pageInfoItem);
        }
    }

    private void updateNavigationButtons() {
        if (previousPageSlot != -1 && previousPageItem != null) {
            if (page > 0) {
                setItem(previousPageSlot, previousPageItem);
            } else {
                inventory.setItem(previousPageSlot, null);
                items.remove(previousPageSlot);
            }
        }

        if (nextPageSlot != -1 && nextPageItem != null) {
            if (page < maxPage) {
                setItem(nextPageSlot, nextPageItem);
            } else {
                inventory.setItem(nextPageSlot, null);
                items.remove(nextPageSlot);
            }
        }
    }

    // ─── Search ─────────────────────────────────────────────────

    /**
     * Search items by display name.
     */
    public List<GuiItem> searchItems(String query) {
        List<GuiItem> results = new ArrayList<>();
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
     * Search and show results as new content.
     */
    public void searchAndShow(String query) {
        List<GuiItem> results = searchItems(query);
        if (!results.isEmpty()) {
            setContent(results);
            page = 0;
            updatePage();
        }
    }

    // ─── Fluent Builder ─────────────────────────────────────────

    /**
     * Create a new PagedGui builder.
     */
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
        private List<GuiItem> content = new ArrayList<>();
        private GuiItem prevPageItem = null;
        private GuiItem nextPageItem = null;
        private GuiItem pageInfoItem = null;
        private boolean autoNavigation = false;

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
         * Default markers: '.' = content, '#' = filler, '<' = prev page, '>' = next page, 'P' = page info
         */
        public Builder setStructure(String... structure) {
            this.structure = structure;
            this.rows = structure.length;
            return this;
        }

        /**
         * Add an ingredient (char → GuiItem mapping for the structure).
         */
        public Builder addIngredient(char key, GuiItem item) {
            this.ingredients.put(key, item);
            return this;
        }

        /**
         * Convenience: add ingredient from ItemBuilder.
         */
        public Builder addIngredient(char key, ItemBuilder builder) {
            return addIngredient(key, new GuiItem(builder));
        }

        /**
         * Convenience: add ingredient from ItemStack.
         */
        public Builder addIngredient(char key, ItemStack itemStack) {
            return addIngredient(key, new GuiItem(itemStack));
        }

        /**
         * Set the content marker character (default '.').
         */
        public Builder setContentMarker(char marker) {
            this.contentMarker = marker;
            return this;
        }

        /**
         * Set the previous page marker character (default '<').
         */
        public Builder setPrevPageMarker(char marker) {
            this.prevPageMarker = marker;
            return this;
        }

        /**
         * Set the next page marker character (default '>').
         */
        public Builder setNextPageMarker(char marker) {
            this.nextPageMarker = marker;
            return this;
        }

        /**
         * Set the page info marker character (default 'P').
         */
        public Builder setPageInfoMarker(char marker) {
            this.pageInfoMarker = marker;
            return this;
        }

        public Builder setContent(List<GuiItem> content) {
            this.content = content != null ? content : new ArrayList<>();
            return this;
        }

        public Builder setPreviousPageItem(GuiItem item) {
            this.prevPageItem = item;
            return this;
        }

        public Builder setNextPageItem(GuiItem item) {
            this.nextPageItem = item;
            return this;
        }

        public Builder setPageInfoItem(GuiItem item) {
            this.pageInfoItem = item;
            return this;
        }

        /**
         * Enable automatic navigation buttons.
         */
        public Builder autoNavigation() {
            this.autoNavigation = true;
            return this;
        }

        public PagedGui build() {
            PagedGui gui;
            if (titleComponent != null) {
                gui = new PagedGui(rows, titleComponent);
            } else {
                gui = new PagedGui(rows, title);
            }

            if (structure != null) {
                // Process structure to find content slots and navigation slots
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

                // Set up navigation
                final int finalPrevSlot = prevSlot;
                final int finalNextSlot = nextSlot;

                if (prevSlot != -1) {
                    GuiItem prev = prevPageItem != null ? prevPageItem :
                            new GuiItem(new ItemBuilder(Material.ARROW)
                                    .setDisplayName("&aPágina anterior"), event -> {
                                gui.previousPage();
                            });
                    gui.previousPageSlot = finalPrevSlot;
                    gui.previousPageItem = prev;
                }

                if (nextSlot != -1) {
                    GuiItem next = nextPageItem != null ? nextPageItem :
                            new GuiItem(new ItemBuilder(Material.ARROW)
                                    .setDisplayName("&aPágina siguiente"), event -> {
                                gui.nextPage();
                            });
                    gui.nextPageSlot = finalNextSlot;
                    gui.nextPageItem = next;
                }

                if (infoSlot != -1) {
                    GuiItem info = pageInfoItem != null ? pageInfoItem :
                            new GuiItem(player -> new ItemBuilder(Material.PAPER)
                                    .setDisplayName("&ePágina " + (gui.page + 1) + " / " + gui.getTotalPages())
                                    .addLoreLines("&7Items: &f" + gui.allItems.size())
                                    .build());
                    gui.pageInfoSlot = infoSlot;
                    gui.pageInfoItem = info;
                }
            } else if (autoNavigation) {
                gui.setNavigationDefault();
            }

            gui.setContent(content);

            return gui;
        }
    }
}
