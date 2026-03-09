# KarmancosUI 📦

> Librería avanzada para crear GUIs profesionales en plugins de Minecraft

[![Build Status](https://github.com/Spinnin34/KarmancosUI/workflows/Maven%20Build/badge.svg)](https://github.com/Spinnin34/KarmancosUI/actions)
[![Java Version](https://img.shields.io/badge/java-21+-blue.svg)](https://www.java.com/)
[![Paper Version](https://img.shields.io/badge/paper-1.21.4+-red.svg)](https://papermc.io/)
[![Maven Central](https://img.shields.io/badge/maven%20central-v1.0.0-brightgreen)](https://github.com/Spinnin34/KarmancosUI/releases)

## 🎯 Características

- ✨ **BaseGui** - GUI base con sistema de items
- 📄 **PagedGui** - Paginación automática de contenido
- 📜 **ScrollGui** - Desplazamiento horizontal/vertical
- 🎬 **AnimatedGui** - Animaciones frame-by-frame
- 📑 **TabGui** - Sistema de pestañas
- 🔄 **Auto-Update** - Actualización automática de items
- 🎯 **EventHandling** - Manejo completo de eventos
- 🧩 **ItemBuilder** - Constructor fluido para items
- 📦 **Thread-Safe** - Operaciones concurrentes seguras

## 🚀 Inicio Rápido

### 1. Agregar Dependencia

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Spinnin34</groupId>
        <artifactId>KarmancosUI</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

### 2. Crear tu Primer GUI

```java
import p.karmancos.gui.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MiGui {
    public static void abrir(Player player) {
        // Crear GUI
        BaseGui gui = new BaseGui(3, "&bMi GUI") {};
        
        // Crear y agregar item
        GuiItem item = new GuiItem(
            new ItemBuilder(Material.DIAMOND).setDisplayName("&bDiamante").build(),
            event -> player.sendMessage("¡Hiciste clic!")
        );
        
        gui.setItem(0, item);
        gui.open(player);
    }
}
```

### 3. Usar en tu Plugin

```java
@Command(name = "gui")
public void openGui(Player player) {
    MiGui.abrir(player);
}
```

## 📚 Documentación

| Archivo | Descripción |
|---------|------------|
| [QUICKSTART.md](QUICKSTART.md) | Guía de inicio en 5 minutos |
| [README.md](README.md) | Documentación completa |
| [EXAMPLES.java](EXAMPLES.java) | 7 ejemplos de código |
| [PUBLISH.md](PUBLISH.md) | Cómo publicar y distribuir |
| [STATUS.md](STATUS.md) | Estado actual del proyecto |
| [CHANGELOG.md](CHANGELOG.md) | Historial de cambios |

## 📖 Ejemplos

### GUI Básico
```java
BaseGui gui = new BaseGui(6, "Mi GUI");
gui.setItem(0, new GuiItem(itemStack, event -> {...}));
gui.open(player);
```

### GUI con Paginación
```java
PagedGui gui = PagedGui.builder()
    .setRows(6)
    .setTitle("Items Paginados")
    .setContent(items)
    .build();
gui.open(player);
```

### GUI Animado
```java
AnimatedGui gui = new AnimatedGui(plugin, 3, "Animado");
gui.addFrame(0, frames1, 20);
gui.addFrame(1, frames2, 20);
gui.setLoop(true);
gui.open(player);
```

### Llenar Áreas
```java
GuiItem border = new GuiItem(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).build());

gui.fillBorder(border);           // Borde completo
gui.fillRow(0, border);           // Primera fila
gui.fillColumn(4, border);        // Columna central
gui.fillArea(1, 1, 3, 8, border); // Área personalizada
```

## 🎓 API Completa

### BaseGui
```java
// Items
gui.setItem(slot, item)
gui.setItem(row, col, item)
gui.removeItem(slot)
gui.getItem(slot)

// Layout
gui.fill(item)
gui.fillBorder(item)
gui.fillRow(row, item)
gui.fillColumn(col, item)
gui.fillArea(startRow, startCol, endRow, endCol, item)

// Actualización
gui.updateSlot(slot)
gui.setAutoUpdate(true, 20L)
gui.updateForPlayer(player)

// Eventos
gui.setCloseAction(consumer)
gui.setOpenAction(consumer)
gui.setOutsideClickAction(consumer)
gui.setPlayerInventoryClickAction(consumer)
gui.setDragAction(consumer)

// Control
gui.open(player)
gui.close(player)
gui.setPreventClose(boolean)
gui.setAllowPlayerInventoryClick(boolean)
gui.setAllowDrag(boolean)
```

## ⚙️ Requisitos

- **Java 21+**
- **Paper/Spigot 1.21.4+**
- **Maven 3.6+** (opcional, para compilar)

## 🔧 Compilar Localmente

```bash
git clone https://github.com/Spinnin34/KarmancosUI.git
cd KarmancosUI
mvn clean package
```

Los JARs generados estarán en `target/`:
- `karmancosui-1.0.0.jar` - JAR compilado
- `karmancosui-1.0.0-sources.jar` - Código fuente
- `karmancosui-1.0.0-javadoc.jar` - Documentación

## 📦 Distribución

### Con JitPack (Recomendado)
La librería se publica automáticamente en [JitPack](https://jitpack.io) desde GitHub.

### Con Maven Local
```bash
mvn install:install-file \
  -Dfile=target/karmancosui-1.0.0.jar \
  -DgroupId=p.karmancos \
  -DartifactId=karmancosui \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

## 🐛 Reportar Problemas

Si encuentras un bug o tienes una sugerencia:
1. Abre un [Issue](https://github.com/Spinnin34/KarmancosUI/issues)
2. Describe el problema con detalles
3. Incluye versión de Java y Paper

## 📄 Licencia

MIT License - Ver LICENSE para más detalles

## 👨‍💻 Autor

Creado por **Spinnin34**

## 🙏 Contribuciones

¡Las contribuciones son bienvenidas! Por favor:
1. Fork el repositorio
2. Crea una rama (git checkout -b feature/mi-feature)
3. Commit los cambios (git commit -m 'Add mi-feature')
4. Push a la rama (git push origin feature/mi-feature)
5. Abre un Pull Request

## 📞 Soporte

- 📖 **Documentación**: Ver [README.md](README.md)
- 🚀 **Inicio Rápido**: Ver [QUICKSTART.md](QUICKSTART.md)
- 💡 **Ejemplos**: Ver [EXAMPLES.java](EXAMPLES.java)
- 📋 **Estado**: Ver [STATUS.md](STATUS.md)

---

**Versión:** 1.0.0  
**Última Actualización:** 9 de Marzo de 2026  
**Estado:** ✅ Completada y Funcional

Hecho con ❤️ para la comunidad de Minecraft

