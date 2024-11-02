package net.edwardcode.fishbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DiscordEventListener extends ListenerAdapter {
    private static final Logger LOG = LogManager.getLogger(DiscordEventListener.class);

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (!event.getGuild().getId().equals(Main.config.guildId)
                || !event.getChannel().getId().equals(Main.config.channelId))
            return;

        String updatedMessageId = event.getMessageId();
        try(Connection sql = Main.database.getConn()) {
            PreparedStatement getMessage = sql.prepareStatement("SELECT * FROM fishes WHERE id=?");
            getMessage.setString(1, updatedMessageId);
            ResultSet rs = getMessage.executeQuery();
            if (!rs.next())
                return;

            if (rs.getString("text").equals(event.getMessage().getContentRaw()))
                return;

            alertMessageEdited(
                    rs.getString("id"),
                    rs.getInt("valid") == 1,
                    rs.getInt("fishnum"),
                    rs.getString("author"),
                    event.getMessage().getContentRaw(),
                    rs.getString("text")
            );

            PreparedStatement updateMessage = sql.prepareStatement("UPDATE fishes SET text=? WHERE id=?");
            updateMessage.setString(1, event.getMessage().getContentRaw());
            updateMessage.setString(2, updatedMessageId);
            updateMessage.executeUpdate();
        } catch (Exception e) {
            LOG.error("SQL Failure: ", e);
        }

//        event.getMessage().getContentRaw()
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (!event.getGuild().getId().equals(Main.config.guildId)
                || !event.getChannel().getId().equals(Main.config.channelId))
            return;

        String deletedMessageId = event.getMessageId();
        try(Connection sql = Main.database.getConn()) {
            PreparedStatement getMessage = sql.prepareStatement("SELECT * FROM fishes WHERE id=?");
            getMessage.setString(1, deletedMessageId);
            ResultSet rs = getMessage.executeQuery();
            if (!rs.next())
                return;

            alertMessageDeleted(
                    rs.getString("id"),
                    rs.getInt("valid") == 1,
                    rs.getInt("fishnum"),
                    rs.getString("author"),
                    rs.getString("text")
            );

            PreparedStatement updateMessage = sql.prepareStatement("UPDATE fishes SET deleted='1' WHERE id=?");
            updateMessage.setString(1, deletedMessageId);
            updateMessage.executeUpdate();
        } catch (Exception e) {
            LOG.error("SQL Failure: ", e);
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getGuild().getId().equals(Main.config.guildId)
            || !event.getChannel().getId().equals(Main.config.channelId))
            return;

        // 1. Check message format
        int fishNumber = Utils.validateMessage(event.getMessage().getContentRaw());
        if (fishNumber < 0) {
            logFish(
                    event.getMessageId(),
                    false,
                    fishNumber,
                    event.getAuthor(),
                    event.getMessage().getContentRaw()
            );
            alertInvalidFish(event, "неверный формат сообщения");
            return;
        }
        // 2. Check last fish number
        try(Connection sql = Main.database.getConn()) {
            PreparedStatement getLastFishNum = sql.prepareStatement("SELECT * FROM fishes WHERE valid='1' AND deleted='0' ORDER BY fishnum DESC LIMIT 1");
            ResultSet rs = getLastFishNum.executeQuery();
            if (rs.next()) {
                int lastFish = rs.getInt("fishnum");
                if (lastFish + 1 != fishNumber || rs.getString("author").split(" ", 2)[0].equals(event.getAuthor().getId())) {
                    if (lastFish + 1 != fishNumber) {
                        alertInvalidFish(event, "нарушен рыбный порядок");
                    } else {
                        alertInvalidFish(event, "предыдущую рыбу отправил этот же пользователь");
                    }
                    logFish(
                            event.getMessageId(),
                            false,
                            fishNumber,
                            event.getAuthor(),
                            event.getMessage().getContentRaw()
                    );
                    return;
                }
            }
        } catch (Exception e) {
            LOG.error("SQL Failure: ", e);
        }

        logFish(
                event.getMessageId(),
                true,
                fishNumber,
                event.getAuthor(),
                event.getMessage().getContentRaw()
        );
    }

    private void alertMessageEdited(String id, boolean valid, int fishNum, String author, String text, String oldText) {
        Main.API.getGuildById(Main.config.guildId).getTextChannelById(Main.config.auditChannelId)
                        .sendMessageEmbeds(new EmbedBuilder()
                                .setTitle("Сообщение отредактировано")
                                .setDescription("Сообщение #" + id + " отредактировано")
                                .setAuthor(author.split(" ", 2)[1])
                                .addField("Сообщение было", valid ? "правильным" : "неправильным", true)
                                .addField("Номер рыбы", String.valueOf(fishNum), true)
                                .addField("Автор", "<@" + author.split(" ", 2)[0] + ">", true)
                                .addField("Старое сообщение", oldText, false)
                                .addField("Новое сообщение", text, false)
                                .setColor(Color.RED)
                                .build())
                .queue();
        System.out.println("Message edited: " + id + ", valid: " + valid + ", fishNum: " + fishNum + ", author: " + author.split(" ", 2)[1] + ", text: " + text + ", oldText: " + oldText);
    }
    private void alertMessageDeleted(String id, boolean valid, int fishNum, String author, String text) {
        Main.API.getGuildById(Main.config.guildId).getTextChannelById(Main.config.auditChannelId)
                .sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("Сообщение удалено")
                        .setDescription("Сообщение #" + id + " удалено")
                        .setAuthor(author.split(" ", 2)[1])
                        .addField("Сообщение было", valid ? "правильным" : "неправильным", true)
                        .addField("Номер рыбы", String.valueOf(fishNum), true)
                        .addField("Автор", "<@" + author.split(" ", 2)[0] + ">", true)
                        .addField("Сообщение", text, false)
                        .setColor(valid ? Color.RED : Color.ORANGE)
                        .build())
                .queue();
        System.out.println("Message deleted: " + id + ", valid: " + valid + ", fishNum: " + fishNum + ", author: " + author.split(" ", 2)[1] + ", text: " + text);
    }
    private void alertInvalidFish(MessageReceivedEvent e, String whatHappened) {
        Main.API.getGuildById(Main.config.guildId).getTextChannelById(Main.config.auditChannelId)
                .sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("Неправильное сообщение")
                        .setDescription("В сообщении #" + e.getMessageId() + " содержится ошибка: " + whatHappened)
                        .setAuthor(e.getAuthor().getAsTag())
                        .addField("Автор", "<@" + e.getAuthor().getId() + ">", true)
                        .addField("Сообщение", e.getMessage().getContentRaw(), false)
                        .setColor(Color.RED)
                        .build())
                .queue();

        System.out.println("Invalid message from " + e.getAuthor().getAsTag() + ": " + e.getMessage().getContentRaw() + ", " + whatHappened);
    }

    private void logFish(String id, boolean valid, int fishNum, User author, String text) {
        try(Connection sql = Main.database.getConn()) {
            PreparedStatement insertData = sql.prepareStatement("INSERT INTO fishes(id, sent, valid, fishnum, author, text) VALUES(?, ?, ?, ?, ?, ?)");
            insertData.setString(1, id);
            insertData.setLong(2, System.currentTimeMillis() / 1000);
            insertData.setInt(3, valid ? 1 : 0);
            insertData.setInt(4, fishNum);
            insertData.setString(5, author.getId() + " " + author.getAsTag());
            insertData.setString(6, text);
            insertData.executeUpdate();
        } catch (SQLException e) {
            LOG.error("SQL Failure: ", e);
        }
    }
}
