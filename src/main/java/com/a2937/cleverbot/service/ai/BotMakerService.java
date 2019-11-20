package com.a2937.cleverbot.service.ai;

import com.avairebot.AvaIre;
import com.avairebot.chat.ConsoleColor;
import com.avairebot.contracts.ai.IntelligenceService;
import com.avairebot.factories.MessageFactory;
import com.avairebot.handlers.DatabaseEventHolder;
import com.avairebot.plugin.JavaPlugin;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.core.entities.Message;
import okhttp3.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class BotMakerService implements IntelligenceService {
    public static final MediaType JSON
        = MediaType.parse("application/json; charset=utf-8");
    private static final Logger log = LoggerFactory.getLogger(BotMakerService.class);
    private static final String actionOutput = ConsoleColor.format(
        "%cyanExecuting Intelligence Action %cyan\" for:"
            + "\n\t\t%cyanUser:\t %author%"
            + "\n\t\t%cyanServer:\t %server%"
            + "\n\t\t%cyanChannel: %channel%"
            + "\n\t\t%cyanMessage: %reset%message%"
            + "\n\t\t%cyanResponse: %reset%response%"
    );
    private static final String propertyOutput = ConsoleColor.format(
        "%reset%s %cyan[%reset%s%cyan]"
    );
    private String apiKey;
    private Cache<String, String> conversationDictionary;
    private ExecutorService executor;

    private JavaPlugin plugin;

    public BotMakerService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void registerService(AvaIre avaIre) {
        apiKey = plugin.getConfig().getString("apiKey", "invalid");
        conversationDictionary = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();

        if (apiKey.equals("invalid"))
        {
            return;
        }
        executor = Executors.newFixedThreadPool(2);

    }

    @Override
    public void unregisterService(AvaIre avaIre) {

        if (executor != null)
        {
            executor.shutdownNow();
        }

    }

    @Override
    public void onMessage(Message message, DatabaseEventHolder databaseEventHolder) {
        executor.submit(() -> processRequest(message, databaseEventHolder));
    }

    private void processRequest(Message message, DatabaseEventHolder databaseEventHolder) {
        String[] split = message.getContentStripped().split(" ");
        String rawMessage = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
        Request request = generateRequestBody(rawMessage, message.getGuild().getId());
        OkHttpClient client = new OkHttpClient();
        try
        {
            Response response = client.newCall(request).execute();
            ProcessResponse(message, response);
        } catch (IOException e)
        {
            log.error("Error occurred processing AI response", e);
        }
    }

    private void ProcessResponse(Message message, Response response) throws IOException {
        if (!response.isSuccessful())
        {
            if (response.body() == null)
            {
                MessageFactory.makeError(message, response.message()).queue();
            }
            else
            {
                String json = response.body().string();
                String error = new JSONObject(json).getString("error");
                MessageFactory.makeError(message, error).queue();
            }
        }
        else
        {
            if (response.body() != null)
            {
                String json = response.body().string();
                JSONObject obj = new JSONObject(json);
                log.info(actionOutput
                    .replace("%author%", generateUsername(message))
                    .replace("%server%", generateServer(message))
                    .replace("%channel%", generateChannel(message))
                    .replace("%message%", message.getContentRaw())
                    .replace("%response%", response.message()));
                conversationDictionary.put(message.getGuild().getId(), obj.getString("cs"));
                MessageFactory.makeInfo(message, obj.get("output").toString()).queue();
            }
        }
    }

    private Request generateRequestBody(String message, String guildId) {
        HttpUrl.Builder urlBuilder
            = HttpUrl.parse("https://www.cleverbot.com/getreply").newBuilder();


        urlBuilder
            .addQueryParameter("key", apiKey)
            .addQueryParameter("input", message);

        if (conversationDictionary.getIfPresent(guildId) != null)
        {
            urlBuilder.addQueryParameter("cs", conversationDictionary.getIfPresent(guildId));
        }

        Request request = new Request.Builder()
            .url(urlBuilder.build().toString())
            .addHeader("Content-Type", "application/json")
            .get()
            .build();

        return request;
    }

    private String generateUsername(Message message) {
        return String.format(propertyOutput,
            message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator(),
            message.getAuthor().getId()
        );
    }

    private String generateServer(Message message) {
        if (!message.getChannelType().isGuild())
        {
            return ConsoleColor.GREEN + "PRIVATE";
        }

        return String.format(propertyOutput,
            message.getGuild().getName(),
            message.getGuild().getId()
        );
    }

    private CharSequence generateChannel(Message message) {
        if (!message.getChannelType().isGuild())
        {
            return ConsoleColor.GREEN + "PRIVATE";
        }

        return String.format(propertyOutput,
            message.getChannel().getName(),
            message.getChannel().getId()
        );
    }
}
