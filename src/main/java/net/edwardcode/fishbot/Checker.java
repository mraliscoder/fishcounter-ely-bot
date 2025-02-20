package net.edwardcode.fishbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class Checker extends TimerTask {
    public static String failedMessageId = "";
    private static final Logger LOG = LogManager.getLogger(Checker.class);

    @Override
    public void run() {
        if (Main.API == null) return;

        int fishNum = 0;
        ArrayList<Message> messages = new ArrayList<>();
        Main.API.getGuildById(Main.config.guildId)
                .getTextChannelById(Main.config.channelId)
                .getIterableHistory()
                .stream()
                .forEachOrdered(messages::add);

        for (Message msg : messages) {
            if (msg.getTimeCreated().isBefore(OffsetDateTime.ofInstant(Instant.ofEpochSecond(Main.config.startLogAfter), ZoneId.systemDefault())))
                return;

            int fishNumCurrent = Utils.validateMessage(msg.getContentRaw());
            if (fishNumCurrent == -1) {
                alertInvalid(msg, true);
                return;
            }
            if (fishNum == 0) {
                fishNum = fishNumCurrent;
            } else if (fishNumCurrent != fishNum) {
                alertInvalid(msg, false);
                return;
            }

            System.out.println("Fish #" + fishNum + ": " + msg.getContentRaw());
            fishNum--;
        }
    }

    private void alertInvalid(Message message, boolean format) {
        if (message.getId().equals(failedMessageId)) return; // Already notified about this
        failedMessageId = message.getId();
        if (format) {
            Main.API.getGuildById(Main.config.guildId).getTextChannelById(Main.config.auditChannelId)
                    .sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("Сообщение с неверным форматом в хронологии!")
                            .setDescription("При очередной проверке выявлена ошибка в хронологии: формат сообщения #" + message.getId() + " не соответствует требуемому")
                            .setAuthor(message.getAuthor().getAsTag())
                            .addField("Номер рыбы", String.valueOf(Utils.validateMessage(message.getContentRaw())), true)
                            .addField("Автор", "<@" + message.getAuthor().getId() + ">", true)
                            .addField("Сообщение", message.getContentRaw(), false)
                            .setColor(Color.RED)
                            .build())
                    .queue();
        } else {
            int fishNum = Utils.validateMessage(message.getContentRaw());
            if (fishNum == -1) {
                Main.API.getGuildById(Main.config.guildId).getTextChannelById(Main.config.auditChannelId)
                        .sendMessageEmbeds(new EmbedBuilder()
                                .setTitle("Сообщение с неверной рыбой в хронологии!")
                                .setDescription("При очередной проверке выявлена ошибка в хронологии: номер рыбы #" + message.getId() + " не определен")
                                .setAuthor(message.getAuthor().getAsTag())
                                .addField("Номер рыбы", String.valueOf(Utils.validateMessage(message.getContentRaw())), true)
                                .addField("Автор", "<@" + message.getAuthor().getId() + ">", true)
                                .addField("Сообщение", message.getContentRaw(), false)
                                .setColor(Color.RED)
                                .build())
                        .queue();
            } else {
                fishNum = fishNum + 1;
                try(Connection sql = Main.database.getConn()) {
                    PreparedStatement getMessage = sql.prepareStatement("SELECT * FROM fishes WHERE fishnum=? AND valid='1'");
                    getMessage.setInt(1, fishNum);
                    ResultSet rs = getMessage.executeQuery();
                    if (!rs.next()) {
                        Main.API.getGuildById(Main.config.guildId).getTextChannelById(Main.config.auditChannelId)
                                .sendMessageEmbeds(new EmbedBuilder()
                                        .setTitle("Рыба потеряна!")
                                        .setDescription("При очередной проверке выявлена ошибка в хронологии: рыба рядом #" + message.getId() + " была потеряна и не найдена в т.ч. в базе данных")
                                        .setAuthor(message.getAuthor().getAsTag())
                                        .addField("Номер рыбы", String.valueOf(Utils.validateMessage(message.getContentRaw())), true)
                                        .addField("Автор", "<@" + message.getAuthor().getId() + ">", true)
                                        .addField("Сообщение", message.getContentRaw(), false)
                                        .setColor(Color.RED)
                                        .build())
                                .queue();
                    } else {
                        Main.API.getGuildById(Main.config.guildId).getTextChannelById(Main.config.auditChannelId)
                                .sendMessageEmbeds(new EmbedBuilder()
                                        .setTitle("Рыба потеряна!")
                                        .setDescription("При очередной проверке выявлена ошибка в хронологии: рыба #" + rs.getString("id") + " потеряна")
                                        .setAuthor(rs.getString("author").split(" ", 2)[1])
                                        .addField("Номер рыбы", String.valueOf(rs.getInt("fishnum")), true)
                                        .addField("Автор", "<@" + rs.getString("author").split(" ", 2)[0] + ">", true)
                                        .addField("Сообщение", rs.getString("text"), false)
                                        .setColor(Color.RED)
                                        .build())
                                .queue();
                    }
                } catch (Exception e){
                    LOG.error("SQL Failure: ", e);
                }
            }
        }
        System.out.println("Order failure! " + message.getContentRaw());
    }
}
