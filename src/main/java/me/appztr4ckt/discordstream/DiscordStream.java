package me.appztr4ckt.discordstream;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DiscordStream extends JavaPlugin implements Listener {

    private String webhookUrl;
    private String messageFormat;
    private String joinMessageFormat;
    private String leaveMessageFormat;
    private String mentionReplacement;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        if (config == null) {
            getLogger().severe("Die Konfigurationsdatei konnte nicht geladen werden.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        webhookUrl = config.getString("discord.webhook_url");
        if (webhookUrl == null) {
            getLogger().severe("Webhook-URL fehlt in der Konfiguration.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        messageFormat = config.getString("messages.format", "%player%: %message%");
        joinMessageFormat = config.getString("messages.join_message_format", "%player% hat den Server betreten.");
        leaveMessageFormat = config.getString("messages.leave_message_format", "%player% hat den Server verlassen.");
        mentionReplacement = config.getString("messages.mention_replacement", "**﹫**");

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        String playerName = event.getPlayer().getName();

        message = ChatColor.stripColor(message); // Farbcodes entfernen
        message = message.replace("@", mentionReplacement); // @-Zeichen ersetzen

        String formattedMessage = messageFormat
                .replace("%player%", playerName)
                .replace("%message%", message);

        sendWebhookMessage(formattedMessage);

        getLogger().info(ChatColor.stripColor(playerName + ": " + message));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        String formattedMessage = joinMessageFormat.replace("%player%", playerName);
        sendWebhookMessage(formattedMessage);
        getLogger().info(playerName + " hat den Server betreten.");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerName = event.getPlayer().getName();
        String formattedMessage = leaveMessageFormat.replace("%player%", playerName);
        sendWebhookMessage(formattedMessage);
        getLogger().info(playerName + " hat den Server verlassen.");
    }

    private void sendWebhookMessage(String message) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String payload = String.format("{\"content\": \"%s\"}", message);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                getLogger().warning("Fehler beim Senden der Nachricht an Discord. Antwortcode: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
