# KarmancosUI Library

Una librería avanzada para crear interfaces gráficas (GUIs) en plugins de Minecraft usando Paper/Spigot.

## Características

- ✅ **BaseGui** - Clase base para crear GUIs personalizadas
- ✅ **PagedGui** - GUIs con sistema de paginación
- ✅ **ScrollGui** - GUIs con desplazamiento
- ✅ **AnimatedGui** - GUIs con animaciones frame-by-frame
- ✅ **TabGui** - GUIs con pestañas
- ✅ **VirtualInventory** - Inventarios virtuales
- ✅ **GuiBuilder** - Constructor fluido para crear GUIs fácilmente
- ✅ **Auto-update system** - Sistema de actualización automática de elementos
- ✅ **Event handling** - Manejo de eventos de clic, drag, apertura y cierre

## Instalación

### Opción 1: Usando Maven

Agrega lo siguiente a tu `pom.xml`:

```xml
<dependency>
    <groupId>p.karmancos</groupId>
    <artifactId>karmancosui</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Opción 2: Copiar JAR manualmente

1. Descarga `karmancosui-1.0.0.jar` de la carpeta `target`
2. Agrega el JAR a la carpeta `libs` de tu proyecto
3. Configura tu IDE para que reconozca la librería

## Uso Rápido

### Crear un GUI básico

```java
import p.karmancos.gui.*;
import org.bukkit.entity.Player;

public class MiGui extends BaseGui {
    public MiGui() {
        super(3, "Mi Primer GUI");
    }
    
    public static void abrir(Player player) {
        MiGui gui = new MiGui();
        
        // Agregar items
        gui.setItem(0, new GuiItem(
            new ItemBuilder(Material.DIAMOND).setDisplayName("&bDiamante").build(),
            event -> {
                player.sendMessage("¡Hiciste clic en el diamante!");
            }
        ));
        
        gui.open(player);
    }
}
```

### Usar GuiBuilder

```java
BaseGui gui = new GuiBuilder()
    .setRows(6)
    .setTitle("Mi GUI")
    .setPaged(true)
    .build();

gui.setItem(0, new GuiItem(
    new ItemBuilder(Material.GOLD_BLOCK).setDisplayName("&6Oro").build(),
    event -> handleClick(event)
));

gui.open(player);
```

### Crear un GUI con paginación

```java
List<GuiItem> items = new ArrayList<>();
// ... agregar items

PagedGui gui = PagedGui.builder()
    .setRows(6)
    .setTitle("Items Paginados")
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
        new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("&7").build()
    ))
    .addIngredient('<', new GuiItem(
        new ItemBuilder(Material.ARROW).setDisplayName("&7Anterior").build()
    ))
    .addIngredient('>', new GuiItem(
        new ItemBuilder(Material.ARROW).setDisplayName("&7Siguiente").build()
    ))
    .build();

gui.open(player);
```

### Crear un GUI animado

```java
AnimatedGui gui = new AnimatedGui(plugin, 3, "GUI Animado");

// Frame 0
List<GuiItem> frame0 = new ArrayList<>();
frame0.add(new GuiItem(new ItemBuilder(Material.DIAMOND).setDisplayName("&bFrame 1").build()));
gui.addFrame(0, frame0, 20); // 20 ticks de duración

// Frame 1
List<GuiItem> frame1 = new ArrayList<>();
frame1.add(new GuiItem(new ItemBuilder(Material.GOLD_BLOCK).setDisplayName("&6Frame 2").build()));
gui.addFrame(1, frame1, 20);

gui.setLoop(true);
gui.setOnAnimationComplete(() -> {
    System.out.println("Animación completada");
});

gui.open(player);
```

## Componentes Principales

### GuiItem
Representa un item en el GUI con soporte para eventos de clic.

```java
GuiItem item = new GuiItem(itemStack, event -> {
    // Manejar clic
    event.setCancelled(true);
    event.getWhoClicked().sendMessage("¡Click!");
});
```

### ItemBuilder
Constructor fluido para crear ItemStacks fácilmente.

```java
ItemStack item = new ItemBuilder(Material.DIAMOND)
    .setDisplayName("&bMi Diamante")
    .setLore("&7Descripción")
    .setAmount(1)
    .addEnchantment(Enchantment.UNBREAKING, 3)
    .build();
```

### Actualizaciones Automáticas

```java
BaseGui gui = ...;

// Habilitar actualización automática cada 20 ticks
gui.setAutoUpdate(true, 20L);

// Establecer prioridad de actualización
gui.setUpdatePriority(UpdatePriority.HIGH);

// Actualizar un slot específico
gui.updateSlot(5);

// Actualizar para un jugador específico
gui.updateSlot(5, player);
```

## API Completa

### BaseGui
- `setItem(slot, GuiItem)`
- `setItem(row, col, GuiItem)`
- `removeItem(slot)`
- `getItem(slot)`
- `updateSlot(slot)`
- `updateForPlayer(player)`
- `setAutoUpdate(boolean, intervalTicks)`
- `open(player)`
- `close(player)`
- `fill(GuiItem)`
- `fillBorder(GuiItem)`
- `fillRow(row, GuiItem)`
- `fillColumn(col, GuiItem)`
- `fillArea(startRow, startCol, endRow, endCol, GuiItem)`
- `setCloseAction(Consumer)`
- `setOpenAction(Consumer)`
- `setOutsideClickAction(Consumer)`
- `setPlayerInventoryClickAction(Consumer)`
- `setDragAction(Consumer)`
- `setPreventClose(boolean)`
- `setAllowPlayerInventoryClick(boolean)`
- `setAllowDrag(boolean)`

## Requisitos

- **Java 21+**
- **Paper/Spigot 1.21.4+**

## Estructura de Archivos Generados

```
target/
├── karmancosui-1.0.0.jar           # JAR compilado (usar en plugins)
├── karmancosui-1.0.0-sources.jar   # Código fuente
└── karmancosui-1.0.0-javadoc.jar   # Documentación
```

## Próximos Pasos

Para usar esta librería en tus plugins:

1. **Maven**: Agrega la dependencia al `pom.xml`
2. **Gradle**: Agrega a `build.gradle`
3. **Manual**: Copia el JAR a la carpeta `libs`

## Licencia

MIT License

## Soporte

Para reportar problemas o sugerencias, contacta con el desarrollador.

