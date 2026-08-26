package com.vizor.tgbot;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

public class BotSettingsScreen extends Screen {

    private TextFieldWidget tokenField;
    private TextFieldWidget adminField;

    public BotSettingsScreen() {
        super(new TranslatableText("gui.tgbot.title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        // поле токена
        tokenField = new TextFieldWidget(this.textRenderer, cx - 130, 32, 260, 20,
                new LiteralText("Bot Token"));
        tokenField.setText(BotSettings.botToken);
        tokenField.setMaxLength(200);
        this.addButton(tokenField);

        // chat id
        String chatIdStr = BotSettings.chatId == 0 ? "не определён" : String.valueOf(BotSettings.chatId);
        this.addButton(new ButtonWidget(cx - 130, 58, 190, 20,
                new LiteralText("Chat ID: " + chatIdStr), b -> {}));
        this.addButton(new ButtonWidget(cx + 64, 58, 66, 20,
                new LiteralText("Определить"), b -> {
            if (!BotSettings.botToken.isEmpty()) {
                BotSettings.chatId = 0;
                BotSettings.save();
                BotTelegram.restart();
                this.init();
            }
        }));

        // режим
        final String modeText = BotSettings.mode == 1
                ? "Режим: 1 - Сервер (ограничен)"
                : "Режим: 2 - Мир (всё можно)";
        this.addButton(new ButtonWidget(cx - 130, 84, 260, 20,
                new LiteralText(modeText), b -> {
            BotSettings.mode = BotSettings.mode == 1 ? 2 : 1;
            BotSettings.save();
            this.init();
        }));

        // админы
        adminField = new TextFieldWidget(this.textRenderer, cx - 130, 110, 260, 20,
                new LiteralText("TG User IDs"));
        adminField.setText(BotSettings.adminIds.stream().map(String::valueOf).reduce((a,b) -> a+","+b).orElse(""));
        adminField.setMaxLength(500);
        this.addButton(adminField);

        // статус бота
        String status = BotTelegram.isRunning() ? "🟢 Работает" : "🔴 Остановлен";
        this.addButton(new ButtonWidget(cx - 130, 136, 260, 20,
                new LiteralText(status), b -> {}));

        // старт/стоп
        this.addButton(new ButtonWidget(cx - 130, 162, 128, 20,
                new LiteralText("▶ Старт"), b -> {
            BotSettings.botToken = tokenField.getText().trim();
            BotSettings.save();
            BotTelegram.start();
            this.init();
        }));
        this.addButton(new ButtonWidget(cx + 2, 162, 128, 20,
                new LiteralText("⏹ Стоп"), b -> {
            BotTelegram.stop();
            this.init();
        }));

        // готово
        this.addButton(new ButtonWidget(cx - 40, this.height - 30, 80, 20,
                new TranslatableText("gui.tgbot.done"), b -> {
            saveAdmins();
            BotSettings.botToken = tokenField.getText().trim();
            BotSettings.save();
            this.onClose();
        }));
    }

    private void saveAdmins() {
        BotSettings.adminIds.clear();
        String text = adminField.getText().trim();
        if (!text.isEmpty()) {
            for (String s : text.split("[,\\s]+")) {
                s = s.trim();
                if (!s.isEmpty()) {
                    try {
                        BotSettings.adminIds.add(Long.parseLong(s));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        BotSettings.save();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title.getString(), this.width / 2, 14, 0xFFFFFF);
        this.textRenderer.drawWithShadow(matrices, "Токен бота (от @BotFather):", this.width / 2 - 130, 22, 0xAAAAAA);
        this.textRenderer.drawWithShadow(matrices, "Telegram ID админов (через запятую):", this.width / 2 - 130, 100, 0xAAAAAA);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
