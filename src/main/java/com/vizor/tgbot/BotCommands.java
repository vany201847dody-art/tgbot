package com.vizor.tgbot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class BotCommands {

    private BotCommands() {
    }

    public static String execute(String text, long userId, boolean admin) {
        if (!text.startsWith("/")) {
            return null;
        }
        String[] parts = text.split("\\s+", 3);
        String cmd = parts[0].toLowerCase().replaceFirst("^/", "");
        String args = parts.length > 1 ? parts[1] : "";
        String args2 = parts.length > 2 ? parts[2] : "";

        MinecraftClient client = MinecraftClient.getInstance();

        switch (cmd) {
            case "help":
                return helpText();
            case "status":
                return statusTextSync(client);
            case "say":
                if (args.isEmpty()) return "❌ /say <текст>";
                final String sayMsg = args + (args2.isEmpty() ? "" : " " + args2);
                client.execute(() -> {
                    if (client.player != null) client.player.sendChatMessage(sayMsg);
                });
                return "✅ Сообщение отправлено: " + sayMsg;
            case "stop":
                client.execute(() -> MovementController.stop(client));
                return "✅ Остановлено";
            case "move":
                if (args.isEmpty()) return "❌ /move forward|back|left|right|stop";
                if (args.equalsIgnoreCase("stop")) {
                    client.execute(() -> MovementController.stop(client));
                    return "✅ Движение остановлено";
                }
                if (BotSettings.mode == 1 && !admin) {
                    long used = MovementController.getTimeUsedMs();
                    if (used >= 120_000) {
                        return "⏳ Лимит движения исчерпан (2 мин).";
                    }
                    long left = (120_000 - used) / 1000;
                    boolean ok = MovementController.startMove(args, userId, admin);
                    if (!ok) return "❌ Направление: forward|back|left|right";
                    return "✅ Двигаюсь: " + args + " (осталось " + left + " сек)";
                }
                boolean ok2 = MovementController.startMove(args, userId, admin);
                return ok2 ? "✅ Двигаюсь: " + args : "❌ Направление: forward|back|left|right";
            case "look":
                if (args.isEmpty() || args2.isEmpty()) return "❌ /look <yaw> <pitch>";
                try {
                    float y = Float.parseFloat(args);
                    float p = Float.parseFloat(args2);
                    final float fy = y, fp = p;
                    client.execute(() -> MovementController.look(client, fy, fp));
                    return "✅ Смотрю: yaw=" + y + " pitch=" + p;
                } catch (NumberFormatException e) {
                    return "❌ Числа: /look 90.0 -15.0";
                }
            case "jump":
                client.execute(() -> {
                    if (client.player != null) {
                        client.options.keyJump.setPressed(true);
                        new Thread(() -> {
                            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                            MinecraftClient.getInstance().options.keyJump.setPressed(false);
                        }, "jump-release").start();
                    }
                });
                return "✅ Прыжок";
            case "tp":
                if (BotSettings.mode != 2) return "❌ ТП запрещено в серверном режиме";
                if (args.isEmpty()) return "❌ /tp <x> <y> <z>";
                try {
                    String[] coords = args.split("\\s+");
                    double x = Double.parseDouble(coords[0]);
                    double y = Double.parseDouble(coords[1]);
                    double z = Double.parseDouble(coords[2]);
                    final String cmdStr = "tp @s " + x + " " + y + " " + z;
                    client.execute(() -> {
                        if (client.player != null) client.player.sendChatMessage(cmdStr);
                    });
                    return "✅ ТП → " + x + " " + y + " " + z;
                } catch (Exception e) {
                    return "❌ /tp 100 64 -200";
                }
            case "give":
                if (BotSettings.mode != 2) return "❌ Give запрещено в серверном режиме";
                if (args.isEmpty()) return "❌ /give <item> [count]";
                try {
                    int count = args2.isEmpty() ? 1 : Integer.parseInt(args2);
                    if (count < 1) count = 1;
                    if (count > 64) count = 64;
                    final String giveCmd = "give @s " + args + " " + count;
                    client.execute(() -> {
                        if (client.player != null) client.player.sendChatMessage(giveCmd);
                    });
                    return "✅ Выдано: " + args + " x" + count;
                } catch (Exception e) {
                    return "❌ /give diamond 5";
                }
            case "gamemode":
                if (BotSettings.mode != 2) return "❌ Гейммод запрещён в серверном режиме";
                final String gm = args.isEmpty() ? "creative" : args;
                client.execute(() -> {
                    if (client.player != null) client.player.sendChatMessage("gamemode " + gm);
                });
                return "✅ Гейммод → " + gm;
            case "heal":
                if (BotSettings.mode != 2) return "❌ Heal запрещён";
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.setHealth(20f);
                    }
                });
                return "✅ Здоровье восстановлено";
            case "feed":
                if (BotSettings.mode != 2) return "❌ Feed запрещён";
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.getHungerManager().setFoodLevel(20);
                    }
                });
                return "✅ Голод восстановлен";
            case "ban":
            case "kick":
            case "kill":
            case "op":
            case "deop":
                return "🚫 ЗАПРЕЩЕНО! Эта команда заблокирована.";
            default:
                return "❓ Неизвестная команда: /" + cmd + "\nНапиши /help";
        }
    }

    private static String statusTextSync(MinecraftClient client) {
        CountDownLatch latch = new CountDownLatch(1);
        final String[] result = new String[]{ "⚠️ Игрок не загружен" };
        client.execute(() -> {
            ClientPlayerEntity p = client.player;
            if (p == null) {
                latch.countDown();
                return;
            }
            String dim = client.world != null ? client.world.getRegistryKey().getValue().getPath() : "?";
            result[0] = String.format(java.util.Locale.ROOT,
                    "📊 <b>Статус:</b>\n"
                    + "❤️ HP: %.0f/20\n"
                    + "🍖 Голод: %d/20\n"
                    + "📍 %.1f %.1f %.1f\n"
                    + "🌍 %s\n"
                    + "🎮 Режим: %s",
                    p.getHealth(), p.getHungerManager().getFoodLevel(),
                    p.getX(), p.getY(), p.getZ(), dim,
                    BotSettings.mode == 1 ? "Сервер" : "Мир");
            latch.countDown();
        });
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return "⚠️ Таймаут получения статуса";
        }
        return result[0];
    }

    private static String helpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 <b>Команды бота:</b>\n\n");
        sb.append("📊 /status — состояние игрока\n");
        sb.append("💬 /say <текст> — написать в чат\n");
        sb.append("🏃 /move forward|back|left|right|stop\n");
        sb.append("👀 /look <yaw> <pitch>\n");
        sb.append("🦘 /jump\n");
        sb.append("⏹ /stop — остановить всё\n");
        if (BotSettings.mode == 2) {
            sb.append("\n🔓 <b>Дополнительно (мир):</b>\n");
            sb.append("📍 /tp <x> <y> <z>\n");
            sb.append("🎁 /give <предмет> [кол-во]\n");
            sb.append("🎮 /gamemode survival|creative|adventure\n");
            sb.append("💚 /heal — восстановить HP\n");
            sb.append("🍖 /feed — восстановить голод\n");
        } else {
            sb.append("\n🔒 Режим сервера: movement ограничен 2 мин\n");
        }
        return sb.toString();
    }
}
