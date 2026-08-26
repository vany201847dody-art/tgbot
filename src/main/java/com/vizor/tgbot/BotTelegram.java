package com.vizor.tgbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class BotTelegram {

    private static Thread pollThread;
    private static long lastUpdateId = 0;
    private static volatile boolean running = false;

    private BotTelegram() {
    }

    public static boolean isRunning() {
        return running && pollThread != null && pollThread.isAlive();
    }

    public static void start() {
        if (running) {
            return;
        }
        if (BotSettings.botToken.isEmpty()) {
            return;
        }
        running = true;
        pollThread = new Thread(BotTelegram::pollLoop, "TG-Bot-Poller");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    public static void stop() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
            pollThread = null;
        }
    }

    public static void restart() {
        stop();
        start();
    }

    public static void sendMessage(String text) {
        if (BotSettings.botToken.isEmpty() || BotSettings.chatId == 0) {
            return;
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("chat_id", BotSettings.chatId);
            body.addProperty("text", text);
            body.addProperty("parse_mode", "HTML");
            httpPost("sendMessage", body.toString());
        } catch (Exception e) {
            System.err.println("[tgbot] Ошибка отправки: " + e);
        }
    }

    private static void pollLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                if (BotSettings.botToken.isEmpty()) {
                    Thread.sleep(5000);
                    continue;
                }
                String url = "getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=30";
                String resp = httpGet(url);
                if (resp == null) {
                    Thread.sleep(5000);
                    continue;
                }
                JsonObject json = new com.google.gson.JsonParser().parse(resp).getAsJsonObject();
                if (!json.get("ok").getAsBoolean()) {
                    Thread.sleep(5000);
                    continue;
                }
                JsonArray results = json.getAsJsonArray("result");
                for (int i = 0; i < results.size(); i++) {
                    JsonObject update = results.get(i).getAsJsonObject();
                    lastUpdateId = Math.max(lastUpdateId, update.get("update_id").getAsLong());
                    if (update.has("message") && update.getAsJsonObject("message").has("text")) {
                        JsonObject msg = update.getAsJsonObject("message");
                        long fromId = msg.getAsJsonObject("from").get("id").getAsLong();
                        long chatId = msg.getAsJsonObject("chat").get("id").getAsLong();
                        String text = msg.get("text").getAsString().trim();

                        // автоопределение chat_id если не задан
                        if (BotSettings.chatId == 0) {
                            BotSettings.chatId = chatId;
                            BotSettings.save();
                            sendMessage("✅ Подключено!\nChat ID: " + chatId);
                        }

                        // обрабатываем только из нашего чата
                        if (chatId != BotSettings.chatId) {
                            continue;
                        }

                        // выполняем команду в клиентском потоке
                        String finalFromName = msg.getAsJsonObject("from").has("username")
                                ? msg.getAsJsonObject("from").get("username").getAsString()
                                : String.valueOf(fromId);
                        boolean admin = BotSettings.isAdmin(fromId);
                        String finalText2 = text;
                        boolean finalAdmin2 = admin;
                        long finalUserId2 = fromId;
                        String response = BotCommands.execute(finalText2, finalUserId2, finalAdmin2);
                        if (response != null && !response.isEmpty()) {
                            sendMessage(response);
                        }
                    }
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println("[tgbot] Poll error: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
        running = false;
    }

    private static String httpGet(String path) throws Exception {
        URL url = new URL("https://api.telegram.org/bot" + BotSettings.botToken + "/" + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(45000);
        conn.setRequestProperty("User-Agent", "Minecraft-TGBot/1.0");
        int code = conn.getResponseCode();
        if (code != 200) {
            conn.disconnect();
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();
        return sb.toString();
    }

    private static String httpPost(String method, String jsonBody) throws Exception {
        URL url = new URL("https://api.telegram.org/bot" + BotSettings.botToken + "/" + method);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("User-Agent", "Minecraft-TGBot/1.0");
        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();
        int code = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();
        return sb.toString();
    }
}
