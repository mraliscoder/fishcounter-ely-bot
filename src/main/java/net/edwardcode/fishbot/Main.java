package net.edwardcode.fishbot;

import com.google.gson.Gson;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Timer;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);
    public static Config config;
    public static String VERSION = "Development Version";
    public static Database database;
    public static JDA API;

    public static void main(String[] args) throws IOException, InterruptedException {
        LOG.info("Initializing Config...");
        Gson gson = new Gson();
        String configString = Files.readString(new File("config.json").toPath());
        config = gson.fromJson(configString, Config.class);

        LOG.info("Loading version information...");
        try {
            String v = Main.class.getPackage().getImplementationVersion();
            if (v != null) {
                VERSION = v;
            }
        } catch (Exception ignored) {}
        LOG.info("Working on version {}", VERSION);

        LOG.info("Connecting to database");
        database = new Database();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new Checker(), 10_000, 1_000 * 60 * 60);

        LOG.info("Starting JDA");
        API = JDABuilder
                .createDefault(config.discordBotToken)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new DiscordEventListener())
                .build()
                .awaitReady();
    }
}
