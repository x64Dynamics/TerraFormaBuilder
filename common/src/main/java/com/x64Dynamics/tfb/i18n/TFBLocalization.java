package com.x64Dynamics.tfb.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Platform-agnostic localization loader backed by assets/terraformabuilder/lang/en_us.json.
 */
public final class TFBLocalization {
    private static final Map<String, String> TRANSLATIONS = new HashMap<>();
    private static volatile boolean loaded = false;

    private TFBLocalization() { }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try (InputStream is = TFBLocalization.class.getResourceAsStream("/assets/terraformabuilder/lang/en_us.json")) {
            if (is != null) {
                Type type = new TypeToken<Map<String, String>>() { }.getType();
                Map<String, String> map = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), type);
                if (map != null) {
                    TRANSLATIONS.putAll(map);
                }
            }
        } catch (Exception ignored) { }
    }

    public static String tr(String key, Object... args) {
        ensureLoaded();
        String pattern = TRANSLATIONS.getOrDefault(key, key);
        if (args != null && args.length > 0) {
            try {
                return String.format(Locale.ROOT, pattern, args);
            } catch (IllegalFormatException ignored) {
                return pattern;
            }
        }
        return pattern;
    }

    public static boolean hasKey(String key) {
        ensureLoaded();
        return TRANSLATIONS.containsKey(key);
    }

    public static Set<String> allKeys() {
        ensureLoaded();
        return Collections.unmodifiableSet(TRANSLATIONS.keySet());
    }
}
