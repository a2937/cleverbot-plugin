
package com.a2937.cleverbot;

import com.a2937.cleverbot.service.ai.BotMakerService;
import com.avairebot.plugin.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotMakerMain extends JavaPlugin {

    public static final Logger LOGGER = LoggerFactory.getLogger(BotMakerMain.class);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getAvaire().getIntelligenceManager().registerService(new BotMakerService(this));
    }
}
