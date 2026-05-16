package p.karmancos.gui.bedrock;

import org.bukkit.entity.Player;
import p.karmancos.gui.BaseGui;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Optional bridge to Geyser/Floodgate Cumulus forms.
 * <p>
 * This class intentionally uses reflection so KarmancosUI can start without
 * Geyser or Floodgate installed. If neither API is present, callers receive
 * false and should open the regular Java inventory.
 */
public final class BedrockFormService {

    private BedrockFormService() {
    }

    public static boolean isAvailable() {
        return classExists("org.geysermc.geyser.api.GeyserApi")
                || classExists("org.geysermc.floodgate.api.FloodgateApi");
    }

    public static boolean isBedrockPlayer(Player player) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        Boolean geyser = callBooleanApi("org.geysermc.geyser.api.GeyserApi", "api", "isBedrockPlayer", uuid);
        if (geyser != null) {
            return geyser;
        }
        Boolean floodgate = callBooleanApi("org.geysermc.floodgate.api.FloodgateApi", "getInstance", "isFloodgatePlayer", uuid);
        return floodgate != null && floodgate;
    }

    public static boolean open(BaseGui gui, Player player, BedrockFormOptions options) {
        if (!isBedrockPlayer(player)) {
            return false;
        }

        BedrockFormOptions safeOptions = options == null ? BedrockFormOptions.defaults() : options;
        BedrockMenuTranslator.TranslatedMenu menu = BedrockMenuTranslator.translate(gui, player, safeOptions);
        if (menu.buttons().isEmpty()) {
            return false;
        }

        Object form = buildSimpleForm(gui, player, safeOptions, menu);
        return form != null && sendForm(player.getUniqueId(), form);
    }

    private static Object buildSimpleForm(BaseGui gui, Player player, BedrockFormOptions options,
                                          BedrockMenuTranslator.TranslatedMenu menu) {
        try {
            Class<?> simpleFormClass = findClass(
                    "org.geysermc.cumulus.form.SimpleForm",
                    "org.geysermc.cumulus.SimpleForm"
            );
            Object builder = simpleFormClass.getMethod("builder").invoke(null);

            invokeBuilder(builder, "title", menu.title());
            invokeBuilder(builder, "content", menu.content());
            for (BedrockFormButton button : menu.buttons()) {
                invokeBuilder(builder, "button", button.text());
            }

            Object validHandler = consumerProxy(response -> {
                int clickedButtonId = readClickedButtonId(response);
                List<BedrockFormButton> buttons = menu.buttons();
                if (clickedButtonId < 0 || clickedButtonId >= buttons.size()) {
                    return;
                }

                BedrockFormButton button = buttons.get(clickedButtonId);
                boolean handled = button.item().handleBedrockClick(player);
                if (!handled && options.isFallbackToJavaInventory()) {
                    gui.open(player);
                }
            });

            if (!invokeCompatible(builder, "validResultHandler", validHandler)) {
                return null;
            }

            return builder.getClass().getMethod("build").invoke(builder);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static boolean sendForm(UUID uuid, Object form) {
        if (sendThroughApi("org.geysermc.geyser.api.GeyserApi", "api", uuid, form)) {
            return true;
        }
        return sendThroughApi("org.geysermc.floodgate.api.FloodgateApi", "getInstance", uuid, form);
    }

    private static boolean sendThroughApi(String className, String accessor, UUID uuid, Object form) {
        try {
            Class<?> apiClass = Class.forName(className);
            Object api = apiClass.getMethod(accessor).invoke(null);
            for (Method method : api.getClass().getMethods()) {
                if (!method.getName().equals("sendForm") || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes[0].isAssignableFrom(UUID.class) && parameterTypes[1].isInstance(form)) {
                    method.invoke(api, uuid, form);
                    return true;
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
        return false;
    }

    private static Boolean callBooleanApi(String className, String accessor, String methodName, UUID uuid) {
        try {
            Class<?> apiClass = Class.forName(className);
            Object api = apiClass.getMethod(accessor).invoke(null);
            Method method = api.getClass().getMethod(methodName, UUID.class);
            Object result = method.invoke(api, uuid);
            return result instanceof Boolean value ? value : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static Object consumerProxy(Consumer<Object> consumer) {
        return Proxy.newProxyInstance(
                BedrockFormService.class.getClassLoader(),
                new Class<?>[]{Consumer.class},
                (proxy, method, args) -> {
                    if ("accept".equals(method.getName()) && args != null && args.length == 1) {
                        consumer.accept(args[0]);
                    }
                    return null;
                }
        );
    }

    private static int readClickedButtonId(Object response) {
        for (String methodName : List.of("clickedButtonId", "getClickedButtonId", "buttonId", "getButtonId")) {
            try {
                Object value = response.getClass().getMethod(methodName).invoke(response);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return -1;
    }

    private static void invokeBuilder(Object builder, String methodName, String value) throws ReflectiveOperationException {
        builder.getClass().getMethod(methodName, String.class).invoke(builder, value);
    }

    private static boolean invokeCompatible(Object target, String methodName, Object argument) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0].isInstance(argument)) {
                method.invoke(target, argument);
                return true;
            }
        }
        return false;
    }

    private static Class<?> findClass(String... classNames) throws ClassNotFoundException {
        for (String className : classNames) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(String.join(", ", classNames));
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className, false, BedrockFormService.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
