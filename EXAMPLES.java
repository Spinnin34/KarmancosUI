package com.example.plugins;

import p.karmancos.gui.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Ejemplo de uso de la librería KarmancosUI
 */
public class GuiExamples {

    /**
     * Ejemplo 1: GUI básico simple
     */
    public static void openBasicGui(Player player) {
        BaseGui gui = new BaseGui(3, "GUI Básico") {
            // Clase anónima simple
        };

        // Crear un item con ItemBuilder
        ItemStack diamante = new ItemBuilder(Material.DIAMOND)
                .setDisplayName("&bDiamante")
                .setLore("&7Este es un diamante")
                .build();

        // Crear GuiItem con evento
        GuiItem item = new GuiItem(diamante, event -> {
            event.setCancelled(true);
            player.sendMessage("¡Hiciste clic en el diamante!");
        });

        // Colocar el item en el GUI
        gui.setItem(0, item);
        gui.open(player);
    }

    /**
     * Ejemplo 2: GUI con GuiBuilder
     */
    public static void openBuilderGui(Player player) {
        BaseGui gui = new GuiBuilder()
                .setRows(6)
                .setTitle("&6Mi GUI con Builder")
                .build();

        // Llenar bordes
        GuiItem border = new GuiItem(
                new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .setDisplayName("&7")
                        .build()
        );
        gui.fillBorder(border);

        // Agregar contenido
        for (int i = 10; i < 17; i++) {
            GuiItem item = new GuiItem(
                    new ItemBuilder(Material.GOLD_BLOCK)
                            .setDisplayName("&6Slot " + i)
                            .build(),
                    event -> {
                        event.setCancelled(true);
                        player.sendMessage("Hiciste clic en el slot: " + event.getSlot());
                    }
            );
            gui.setItem(i, item);
        }

        gui.open(player);
    }

    /**
     * Ejemplo 3: GUI con paginación
     */
    public static void openPagedGui(Player player) {
        // Crear items para paginar
        java.util.List<GuiItem> items = new java.util.ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            final int itemNumber = i;
            items.add(new GuiItem(
                    new ItemBuilder(Material.APPLE)
                            .setDisplayName("&cItem #" + i)
                            .setAmount(Math.min(64, i))
                            .build(),
                    event -> {
                        event.setCancelled(true);
                        player.sendMessage("Seleccionaste el item #" + itemNumber);
                    }
            ));
        }

        PagedGui gui = PagedGui.builder()
                .setRows(6)
                .setTitle("&bInventario Paginado")
                .setContent(items)
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# # < P > # # # #"
                )
                .addIngredient('#', new GuiItem(
                        new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                                .setDisplayName("&7")
                                .build()
                ))
                .addIngredient('<', new GuiItem(
                        new ItemBuilder(Material.ARROW)
                                .setDisplayName("&7Página Anterior")
                                .build()
                ))
                .addIngredient('>', new GuiItem(
                        new ItemBuilder(Material.ARROW)
                                .setDisplayName("&7Siguiente Página")
                                .build()
                ))
                .addIngredient('P', new GuiItem(
                        new ItemBuilder(Material.PAPER)
                                .setDisplayName("&6Información")
                                .build()
                ))
                .build();

        gui.open(player);
    }

    /**
     * Ejemplo 4: GUI animado
     */
    public static void openAnimatedGui(Player player, org.bukkit.plugin.Plugin plugin) {
        AnimatedGui gui = new AnimatedGui(plugin, 3, "&eGUI Animado");

        // Frame 1
        java.util.List<GuiItem> frame1 = new java.util.ArrayList<>();
        frame1.add(new GuiItem(
                new ItemBuilder(Material.DIAMOND)
                        .setDisplayName("&bFrame 1")
                        .build()
        ));
        gui.addFrame(0, frame1, 20); // 20 ticks

        // Frame 2
        java.util.List<GuiItem> frame2 = new java.util.ArrayList<>();
        frame2.add(new GuiItem(
                new ItemBuilder(Material.GOLD_BLOCK)
                        .setDisplayName("&6Frame 2")
                        .build()
        ));
        gui.addFrame(1, frame2, 20);

        // Frame 3
        java.util.List<GuiItem> frame3 = new java.util.ArrayList<>();
        frame3.add(new GuiItem(
                new ItemBuilder(Material.EMERALD_BLOCK)
                        .setDisplayName("&aFrame 3")
                        .build()
        ));
        gui.addFrame(2, frame3, 20);

        gui.setLoop(true);
        gui.setOnAnimationComplete(() -> {
            player.sendMessage("¡Animación completada!");
        });

        gui.open(player);
    }

    /**
     * Ejemplo 5: GUI con actualización automática
     */
    public static void openAutoUpdateGui(Player player, org.bukkit.plugin.Plugin plugin) {
        BaseGui gui = new BaseGui(6, "&6GUI Auto-Update") {
        };

        // Item que se actualiza dinámicamente
        gui.setItem(0, new GuiItem(
                new ItemBuilder(Material.REDSTONE)
                        .setDisplayName("&cItem Dinámico")
                        .build(),
                event -> {
                    event.setCancelled(true);
                    player.sendMessage("Contador: " + System.currentTimeMillis());
                }
        ));

        // Habilitar actualización automática cada 20 ticks (1 segundo)
        gui.setAutoUpdate(true, 20L);
        gui.setUpdatePriority(UpdatePriority.HIGH);

        gui.open(player);
    }

    /**
     * Ejemplo 6: GUI con proveedor de items
     */
    public static void openProviderGui(Player player) {
        BaseGui gui = new BaseGui(3, "&9GUI con Provider") {
        };

        // Usar un ItemProvider personalizado
        ItemProvider provider = new ItemProvider() {
            @Override
            public ItemStack getItem(Player player) {
                // Generar items dinámicamente
                return new ItemBuilder(Material.PLAYER_HEAD)
                        .setDisplayName("&9" + player.getName())
                        .setLore("&7UUID: " + player.getUniqueId())
                        .build();
            }
        };

        // Crear GuiItem desde el provider
        GuiItem dynamicItem = new GuiItem(
                provider.getItem(player),
                event -> {
                    event.setCancelled(true);
                    player.sendMessage("¡Item dinámico!");
                }
        );

        gui.setItem(13, dynamicItem);
        gui.open(player);
    }

    /**
     * Ejemplo 7: GUI con eventos personalizados
     */
    public static void openEventGui(Player player) {
        BaseGui gui = new BaseGui(3, "&dGUI con Eventos") {
        };

        // Configurar evento de apertura
        gui.setOpenAction(event -> {
            player.sendMessage("&a¡GUI Abierto!");
        });

        // Configurar evento de cierre
        gui.setCloseAction(event -> {
            player.sendMessage("&c¡GUI Cerrado!");
        });

        // Configurar evento de clic afuera
        gui.setOutsideClickAction(event -> {
            event.setCancelled(true);
            player.sendMessage("&e¡Intentaste clickear afuera!");
        });

        // Permitir clic en inventario personal
        gui.setAllowPlayerInventoryClick(true);

        // Permitir drag
        gui.setAllowDrag(false);

        gui.open(player);
    }
}

