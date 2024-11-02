package net.edwardcode.fishbot;

public class Config {
    public MySQL mysql;
    public String discordBotToken;
    public String guildId;
    public String channelId;
    public String auditChannelId;
    public long startLogAfter;

    public static class MySQL {
        public String host;
        public String name;
        public String user;
        public String pass;
    }
}
