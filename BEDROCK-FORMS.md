# Bedrock Forms

KarmancosUI can translate regular inventory menus into Bedrock `SimpleForm` menus through Geyser/Floodgate.
The integration is optional: if neither API is installed, KarmancosUI still starts and opens the normal Java inventory.

## Quick Use

```java
import p.karmancos.gui.*;
import p.karmancos.gui.bedrock.BedrockFormOptions;

BaseGui gui = new BaseGui(3, "&bMenu") {};

gui.setItem(0, new GuiItem(
    new ItemBuilder(Material.DIAMOND)
        .setDisplayName("&bDiamond")
        .setLore("&7Available in Java and Bedrock")
        .build()
).onClick(event -> {
    event.getWhoClicked().sendMessage("Java inventory click");
}).onBedrockClick(player -> {
    player.sendMessage("Bedrock form click");
}));

gui.openAdaptive(player);
```

## Translation Options

```java
BedrockFormOptions options = BedrockFormOptions.defaults()
    .setContent("Choose an option")
    .setIncludeLore(true)
    .setIncludeSlotNumbers(false)
    .setFallbackToJavaInventory(true)
    .setTranslator((text, locale) -> translations.getOrDefault(text, text));

gui.openAdaptive(player, options);
```

## API

- `open(player)` always opens the classic inventory.
- `openAdaptive(player)` sends a Bedrock form when possible, otherwise opens the classic inventory.
- `tryOpenBedrockForm(player)` only attempts the Bedrock form and returns whether it was sent.
- `GuiItem#onBedrockClick(player -> ...)` registers an action that can run from a form.

Bedrock forms do not create Bukkit `InventoryClickEvent` instances. Existing Java inventory callbacks still work for Java players, but Bedrock-specific actions should be registered with `onBedrockClick`.

`PagedGui` prepares the current page before translation, and its previous/next navigation buttons reopen the updated Bedrock form automatically.
