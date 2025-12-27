package org.nmo.project_exfil.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import net.kyori.adventure.audience.Audience;
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
    @SuppressWarnings("rawtypes")
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
                    @SuppressWarnings("unchecked")
                    TranslationStore<MessageFormat> typedRegistry = (TranslationStore<MessageFormat>) this.registry;
                    typedRegistry.register(entry.getKey(), locale, new MessageFormat(entry.getValue(), locale));
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
        String raw = getRawString(key);
        if (raw == null) raw = key;
        
        Component parsed = miniMessage.deserialize(raw, tags);
        audience.sendMessage(parsed);
    }
    
    private String getRawString(String key) {
        java.text.MessageFormat format = (java.text.MessageFormat) registry.translate(key, Locale.CHINA);
        if (format != null) {
            return format.toPattern();
        }
        return null;
    }
}
