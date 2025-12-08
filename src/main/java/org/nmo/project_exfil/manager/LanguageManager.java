package org.nmo.project_exfil.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

public class LanguageManager {

    private final ProjectEXFILPlugin plugin;
    private static final Key KEY = Key.key("project_exfil", "main");
    private TranslationStore registry;
    private final MiniMessage miniMessage;

    public LanguageManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.setup();
    }

    private void setup() {
        this.registry = TranslationStore.messageFormat(KEY);
        // Load Chinese language from JSON
        loadLanguage(Locale.CHINA, "zh_CN.json");
        GlobalTranslator.translator().addSource(this.registry);
    }

    private void loadLanguage(Locale locale, String fileName) {
        try (InputStream in = plugin.getResource("languages/" + fileName)) {
            if (in == null) {
                plugin.getLogger().warning("Language file not found: " + fileName);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Map<String, String> map = new Gson().fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    this.registry.register(entry.getKey(), locale, new MessageFormat(entry.getValue(), locale));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load language: " + fileName);
            e.printStackTrace();
        }
    }

    public Component getMessage(String key, TagResolver... tags) {
        String raw = getRawString(key);
        if (raw == null) return Component.text(key);
        return miniMessage.deserialize(raw, tags);
    }
    
    public void send(Audience audience, String key, TagResolver... tags) {
        // Create a translatable component with the key
        // We use MiniMessage to parse the translation result if needed, but GlobalTranslator handles Component.translatable
        // However, to support MiniMessage tags INSIDE the properties file, we need a bit more logic if we want to parse them after translation.
        // But Adventure's GlobalTranslator returns a MessageFormat by default for properties.
        // If we want MiniMessage support in lang files, we usually use a custom translator or just parse the result.
        
        // Simple approach: Use MiniMessage to parse the raw string from bundle if we were doing it manually.
        // But since we registered to GlobalTranslator, Component.translatable(key) should work and return the text.
        // If the text contains MiniMessage tags, they won't be parsed automatically by vanilla client.
        // We need to parse them.
        
        // Better approach for MiniMessage + Lang:
        // 1. Get raw string from registry
        // 2. Parse with MiniMessage
        
        String raw = getRawString(key);
        if (raw == null) raw = key;
        
        Component parsed = miniMessage.deserialize(raw, tags);
        audience.sendMessage(parsed);
    }
    
    private String getRawString(String key) {
        // This is a helper to get the raw string from our registry for the default locale
        // In a real multi-lang setup, we would check player's locale.
        java.text.MessageFormat format = (java.text.MessageFormat) registry.translate(key, Locale.CHINA);
        if (format != null) {
            return format.toPattern();
        }
        return null;
    }
}
