
package com.a2937.cleverbot;

import com.a2937.cleverbot.service.ai.BotMakerService;
import com.avairebot.config.EnvironmentOverride;
import com.avairebot.plugin.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotMakerMain extends JavaPlugin {

    public static final Logger LOGGER = LoggerFactory.getLogger(BotMakerMain.class);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        EnvironmentOverride.overrideWithPrefix("CLEVERBOT", getConfig());
        getAvaire().getIntelligenceManager().registerService(new BotMakerService(this));
    }

    public String getApiKey() {
        return this.getConfig().getString("keys.apiKey", "invalid");
    }
}
