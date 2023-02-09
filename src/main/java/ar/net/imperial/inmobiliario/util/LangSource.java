package ar.net.imperial.inmobiliario.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.ResourceBundle;

public class LangSource {
    private final MiniMessage miniMessageMessage = MiniMessage.miniMessage();
    private final ResourceBundle resourceBundle;

    public LangSource(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    /**
     * Get a string from the resource bundle. It includes the prefix.
     *
     * @param key  The key of the string.
     * @param args The arguments to format the string.
     * @return The formatted string including prefix.
     */
    public String getStr(@PropertyKey(resourceBundle = "messages") String key, Object... args) {
        String prefix = resourceBundle.getString("PREFIX") + "<reset>";
        return String.format(prefix + " " + resourceBundle.getString(key), args);
    }

    @NotNull public String getStrNoPrefix(@PropertyKey(resourceBundle = "messages") String key, Object... args) {
        return String.format(resourceBundle.getString(key), args);
    }

    /**
     * Get a string from the resource bundle. To be used in Item Name or Lore.
     * It does not include the prefix. It does not allow Italic.
     *
     * @param key  The key of the string.
     * @param args The arguments to format the string.
     * @return The formatted string.
     */
    public Component getForItem(@PropertyKey(resourceBundle = "messages") String key, Object... args) {
        String str = "<!italic>" + String.format(resourceBundle.getString(key), args) + "</!italic>";
        Component component = miniMessageMessage.deserialize(str);
        return Component.empty().color(NamedTextColor.WHITE).append(component);
    }

}
