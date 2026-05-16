package p.karmancos.gui.bedrock;

import p.karmancos.gui.GuiItem;

/**
 * One translated Bedrock form button mapped back to its original GUI item.
 */
public record BedrockFormButton(int slot, String text, GuiItem item) {
}
