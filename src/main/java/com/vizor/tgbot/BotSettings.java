package com.vizor.tgbot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class BotSettings {

    public static String botToken = "";
    public static long chatId = 0;
    public static int mode = 2; // 1=сервер(ограничен), 2=мир(всё)
    public static Set<Long> adminIds = new HashSet<>();

    private static File file;
    private static final Gson GSON = new Gson();

    public static void load() {
        file = FabricLoader.getInstance().getGameDir().resolve("tgbot.json").toFile();
        if (!file.exists()) {
            save();
            return;
        }
        try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject root = new com.google.gson.JsonParser().parse(r).getAsJsonObject();
            botToken = root.has("token") ? root.get("token").getAsString() : "";
            chatId = root.has("chatId") ? root.get("chatId").getAsLong() : 0;
            mode = root.has("mode") ? root.get("mode").getAsInt() : 2;
            adminIds.clear();
            if (root.has("admins")) {
                for (com.google.gson.JsonElement e : root.getAsJsonArray("admins")) {
                    adminIds.add(e.getAsLong());
                }
            }
        } catch (Exception e) {
            System.err.println("[tgbot] Ошибка чтения конфига: " + e);
        }
    }

    public static synchronized void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("token", botToken);
            root.addProperty("chatId", chatId);
            root.addProperty("mode", mode);
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (Long id : adminIds) {
                arr.add(id);
            }
            root.add("admins", arr);
            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            System.err.println("[tgbot] Ошибка сохранения: " + e);
        }
    }

    public static boolean isConfigured() {
        return botToken != null && !botToken.isEmpty() && chatId != 0;
    }

    public static boolean isAdmin(long telegramUserId) {
        return adminIds.contains(telegramUserId);
    }
}
