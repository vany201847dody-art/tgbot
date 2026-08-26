package com.vizor.tgbot;

import net.minecraft.client.MinecraftClient;

public final class MovementController {

    private static final long MOVE_TIME_LIMIT_MS = 120_000; // 2 минуты

    private static volatile String moveDirection = null;
    private static volatile long moveStartTime = 0;
    private static volatile long moveTotalMs = 0;
    private static volatile boolean moveIsAdmin = false;

    private static volatile Float lookYaw = null;
    private static volatile Float lookPitch = null;

    private MovementController() {
    }

    public static boolean startMove(String direction, long userId, boolean admin) {
        String d = direction.toLowerCase().trim();
        switch (d) {
            case "forward":
            case "back":
            case "left":
            case "right":
                break;
            default:
                return false;
        }
        if (moveDirection != null && moveTotalMs > 0) {
            moveTotalMs += System.currentTimeMillis() - moveStartTime;
        }
        moveDirection = d;
        moveStartTime = System.currentTimeMillis();
        moveIsAdmin = admin;
        return true;
    }

    public static void look(MinecraftClient client, float yaw, float pitch) {
        lookYaw = yaw;
        lookPitch = Math.max(-90f, Math.min(90f, pitch));
    }

    public static void stop(MinecraftClient client) {
        moveDirection = null;
        moveTotalMs = 0;
        lookYaw = null;
        lookPitch = null;
        resetKeys(client);
    }

    public static long getTimeUsedMs() {
        return moveTotalMs;
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        // --- движение ---
        if (moveDirection != null) {
            if (!moveIsAdmin) {
                long elapsed = System.currentTimeMillis() - moveStartTime + moveTotalMs;
                if (elapsed >= MOVE_TIME_LIMIT_MS) {
                    stop(client);
                    if (client.player != null) {
                        client.player.sendMessage(
                                new net.minecraft.text.LiteralText("§c[ТГ-Бот] Лимит движения исчерпан (2 мин)"), true);
                    }
                    return;
                }
            }
            applyMoveKeys(client, moveDirection);
        } else {
            client.options.keyForward.setPressed(false);
            client.options.keyBack.setPressed(false);
            client.options.keyLeft.setPressed(false);
            client.options.keyRight.setPressed(false);
        }

        // --- обзор ---
        if (lookYaw != null) {
            client.player.yaw = lookYaw.floatValue();
            client.player.headYaw = lookYaw.floatValue();
            if (lookPitch != null) {
                client.player.pitch = lookPitch.floatValue();
            }
        }
    }

    private static void applyMoveKeys(MinecraftClient client, String dir) {
        client.options.keyForward.setPressed(dir.equals("forward"));
        client.options.keyBack.setPressed(dir.equals("back"));
        client.options.keyLeft.setPressed(dir.equals("left"));
        client.options.keyRight.setPressed(dir.equals("right"));
    }

    private static void resetKeys(MinecraftClient client) {
        client.options.keyForward.setPressed(false);
        client.options.keyBack.setPressed(false);
        client.options.keyLeft.setPressed(false);
        client.options.keyRight.setPressed(false);
        client.options.keyJump.setPressed(false);
    }
}
