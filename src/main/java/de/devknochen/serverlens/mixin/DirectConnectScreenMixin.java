/*
 * Copyright 2026 DevKnochen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.devknochen.serverlens.mixin;

import de.devknochen.serverlens.Main;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(DirectConnectScreen.class)
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public abstract class DirectConnectScreenMixin extends Screen {

    @Unique
    private static final Logger SERVERLENS$LOGGER = LoggerFactory.getLogger(DirectConnectScreenMixin.class);
    @Unique
    private static final String SERVERLENS$MOD_ID = "serverlens";
    @Unique
    private static final ExecutorService SERVERLENS$PING_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ServerLens Server Pinger");
        thread.setDaemon(true);
        return thread;
    });
    @Unique
    private static final String SERVERLENS$SERVER_LIST_TEXTURE_PATH = "gui/serverlist/";
    @Unique
    private static final String SERVERLENS$DIRECT_CONNECT_ICON_ID = "serverlens_directconnectmotd";
    @Unique
    private static final String SERVERLENS$PINGING_TEXT = "multiplayer.status.pinging";
    @Unique
    private static final String SERVERLENS$CANNOT_CONNECT_TEXT = "multiplayer.status.cannot_connect";
    @Unique
    private static final String SERVERLENS$PLAYER_COUNT_SEPARATOR = "/";

    @Unique
    private static final int SERVERLENS$STATE_INITIAL = 0;
    @Unique
    private static final int SERVERLENS$STATE_PINGING = 1;
    @Unique
    private static final int SERVERLENS$STATE_SUCCESSFUL = 2;
    @Unique
    private static final int SERVERLENS$STATE_UNREACHABLE = 3;
    @Unique
    private static final int SERVERLENS$STATE_INCOMPATIBLE = 4;

    @Unique
    private static final Identifier[] SERVERLENS$PING_TEXTURES = {
            serverlens$texture("ping_1"),
            serverlens$texture("ping_2"),
            serverlens$texture("ping_3"),
            serverlens$texture("ping_4"),
            serverlens$texture("ping_5")
    };
    @Unique
    private static final Identifier[] SERVERLENS$PINGING_TEXTURES = {
            serverlens$texture("pinging_1"),
            serverlens$texture("pinging_2"),
            serverlens$texture("pinging_3"),
            serverlens$texture("pinging_4"),
            serverlens$texture("pinging_5")
    };
    @Unique
    private static final Identifier SERVERLENS$UNREACHABLE = serverlens$texture("unreachable");
    @Unique
    private static final Identifier SERVERLENS$INCOMPATIBLE = serverlens$texture("incompatible");
    @Unique
    private static final Identifier SERVERLENS$DEFAULT_ICON = serverlens$texture("default_icon");

    @Unique
    private static final int SERVERLENS$MAX_ROW_WIDTH = 305;
    @Unique
    private static final int SERVERLENS$MIN_ROW_WIDTH = 200;
    @Unique
    private static final int SERVERLENS$SIDE_PADDING = 20;
    @Unique
    private static final int SERVERLENS$FALLBACK_PREVIEW_Y_OFFSET = 30;
    @Unique
    private static final int SERVERLENS$PREVIEW_TOP_PADDING = 8;
    @Unique
    private static final int SERVERLENS$PREVIEW_BOTTOM_PADDING = 4;
    @Unique
    private static final int SERVERLENS$JOIN_BUTTON_Y_OFFSET = 108;
    @Unique
    private static final int SERVERLENS$ICON_SIZE = 32;
    @Unique
    private static final int SERVERLENS$MIN_ICON_SIZE = 8;
    @Unique
    private static final int SERVERLENS$TEXT_ICON_GAP = 3;
    @Unique
    private static final int SERVERLENS$TEXT_WIDTH_PADDING = 2;
    @Unique
    private static final int SERVERLENS$MOTD_Y_OFFSET = 10;
    @Unique
    private static final int SERVERLENS$MAX_MOTD_LINES = 2;
    @Unique
    private static final int SERVERLENS$PING_X_OFFSET = 15;
    @Unique
    private static final int SERVERLENS$PING_WIDTH = 10;
    @Unique
    private static final int SERVERLENS$PING_HEIGHT = 8;
    @Unique
    private static final int SERVERLENS$PLAYER_COUNT_GAP = 5;
    @Unique
    private static final int SERVERLENS$RIGHT_TEXT_GAP = 8;
    @Unique
    private static final int SERVERLENS$PING_ANIMATION_INTERVAL_MS = 100;
    @Unique
    private static final int SERVERLENS$PING_ANIMATION_FRAME_COUNT = 8;
    @Unique
    private static final int SERVERLENS$PING_ANIMATION_PEAK_FRAME = 4;
    @Unique
    private static final long SERVERLENS$PING_DEBOUNCE_MS = 500L;
    @Unique
    private static final long SERVERLENS$PING_TIMEOUT_MS = 10_000L;
    @Unique
    private static final long SERVERLENS$SERVER_DATA_GRACE_MS = 1_000L;
    @Unique
    private static final int SERVERLENS$MIN_PING_BARS = 1;
    @Unique
    private static final int SERVERLENS$POOR_PING_BARS = 1;
    @Unique
    private static final int SERVERLENS$SLOW_PING_BARS = 2;
    @Unique
    private static final int SERVERLENS$OK_PING_BARS = 3;
    @Unique
    private static final int SERVERLENS$GOOD_PING_BARS = 4;
    @Unique
    private static final int SERVERLENS$EXCELLENT_PING_BARS = 5;
    @Unique
    private static final int SERVERLENS$GOOD_PING_MS = 150;
    @Unique
    private static final int SERVERLENS$OK_PING_MS = 300;
    @Unique
    private static final int SERVERLENS$SLOW_PING_MS = 600;
    @Unique
    private static final int SERVERLENS$BAD_PING_MS = 1_000;
    @Unique
    private static final int SERVERLENS$COLOR_WHITE = 0xFFFFFFFF;
    @Unique
    private static final int SERVERLENS$COLOR_MOTD = 0xFF808080;
    @Unique
    private static final int SERVERLENS$COLOR_PLAYER_COUNT = 0xFFAAAAAA;
    @Unique
    private static final int SERVERLENS$COLOR_PLAYER_COUNT_SLASH = 0xFF555555;
    @Unique
    private static final int SERVERLENS$COLOR_INCOMPATIBLE_VERSION = 0xFFFF5555;
    @Unique
    private static final float SERVERLENS$TEXTURE_U = 0.0F;
    @Unique
    private static final float SERVERLENS$TEXTURE_V = 0.0F;
    @Unique
    private static final long SERVERLENS$UNKNOWN_PING = -1L;
    @Unique
    private static final long SERVERLENS$MIN_KNOWN_PING = 0L;
    @Unique
    private static final boolean SERVERLENS$TEXT_SHADOW = true;

    @Shadow
    private TextFieldWidget addressField;

    @Unique
    private String serverlens$lastAddress = "";
    @Unique
    private String serverlens$serverName = "";
    @Unique
    private Text serverlens$motdText = Text.empty();
    @Unique
    private String serverlens$playerCount = "";
    @Unique
    private String serverlens$versionText = "";
    @Unique
    private long serverlens$pingValue = SERVERLENS$UNKNOWN_PING;
    @Unique
    private int serverlens$serverState = SERVERLENS$STATE_INITIAL;
    @Unique
    private Identifier serverlens$serverIcon;
    @Unique
    private byte[] serverlens$lastFavicon;
    @Unique
    private final MultiplayerServerListPinger serverlens$pinger = new MultiplayerServerListPinger();
    @Unique
    private ServerInfo serverlens$currentServer;
    @Unique
    private String serverlens$pendingAddress = "";
    @Unique
    private long serverlens$pendingPingAt;
    @Unique
    private long serverlens$activePingStartedAt;
    @Unique
    private long serverlens$serverDataAt;

    protected DirectConnectScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private void serverlens$updateFavicon(byte[] faviconBytes) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) {
            client.execute(() -> serverlens$updateFavicon(faviconBytes));
            return;
        }

        if (faviconBytes == null) {
            serverlens$clearServerIcon();
            serverlens$lastFavicon = null;
            return;
        }

        if (serverlens$lastFavicon != null && Arrays.equals(faviconBytes, serverlens$lastFavicon)) {
            return;
        }
        serverlens$lastFavicon = Arrays.copyOf(faviconBytes, faviconBytes.length);

        try {
            serverlens$clearServerIcon();

            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> SERVERLENS$DIRECT_CONNECT_ICON_ID, NativeImage.read(faviconBytes));
            serverlens$serverIcon = Identifier.of(SERVERLENS$MOD_ID, SERVERLENS$DIRECT_CONNECT_ICON_ID);
            client.getTextureManager().registerTexture(serverlens$serverIcon, texture);
        } catch (IOException | RuntimeException e) {
            serverlens$clearServerIcon();
            if (Main.isDebugMode()) {
                SERVERLENS$LOGGER.debug("Failed to upload server favicon", e);
            }
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void serverlens$addPreviewRenderer(CallbackInfo ci) {
        this.addDrawable(this::serverlens$renderExtras);
    }

    @Unique
    private void serverlens$renderExtras(DrawContext graphics, int mouseX, int mouseY, float delta) {
        serverlens$updateAddressPreview();
        serverlens$tickServerPing();

        int rowWidth = MathHelper.clamp(this.width - SERVERLENS$SIDE_PADDING * 2, SERVERLENS$MIN_ROW_WIDTH, SERVERLENS$MAX_ROW_WIDTH);
        int baseX = (this.width - rowWidth) / 2;
        int editorBottom = addressField != null ? addressField.getY() + addressField.getHeight() : this.height / 2 + SERVERLENS$FALLBACK_PREVIEW_Y_OFFSET;
        int buttonTop = this.height / 4 + SERVERLENS$JOIN_BUTTON_Y_OFFSET;
        int availableHeight = buttonTop - editorBottom - SERVERLENS$PREVIEW_TOP_PADDING - SERVERLENS$PREVIEW_BOTTOM_PADDING;
        if (availableHeight < this.textRenderer.fontHeight) {
            return;
        }

        int baseY = editorBottom + SERVERLENS$PREVIEW_TOP_PADDING;
        int iconSize = MathHelper.clamp(availableHeight, SERVERLENS$MIN_ICON_SIZE, SERVERLENS$ICON_SIZE);
        int textX = baseX + iconSize + SERVERLENS$TEXT_ICON_GAP;
        int textWidth = rowWidth - (textX - baseX) - SERVERLENS$TEXT_WIDTH_PADDING;
        int nameWidth = serverlens$getNameWidth(baseX, rowWidth, textX);

        serverlens$renderServerIcon(graphics, baseX, baseY, iconSize);
        serverlens$renderServerText(graphics, textX, baseY, nameWidth, textWidth, availableHeight);
        serverlens$renderPing(graphics, baseX, rowWidth, baseY);
    }

    @Unique
    private void serverlens$updateAddressPreview() {
        if (addressField == null) {
            return;
        }

        String address = addressField.getText();
        if (address.isBlank()) {
            if (!serverlens$lastAddress.isEmpty()) {
                serverlens$lastAddress = "";
                serverlens$serverName = "";
                serverlens$motdText = Text.empty();
                serverlens$playerCount = "";
                serverlens$versionText = "";
                serverlens$pingValue = SERVERLENS$UNKNOWN_PING;
                serverlens$serverState = SERVERLENS$STATE_INITIAL;
                serverlens$updateFavicon(null);
                serverlens$pendingAddress = "";
                serverlens$currentServer = null;
                serverlens$cancelPings();
            }
            return;
        }
        if (address.equals(serverlens$lastAddress)) {
            return;
        }

        serverlens$lastAddress = address;
        serverlens$serverName = address;
        serverlens$motdText = Text.translatable(SERVERLENS$PINGING_TEXT);
        serverlens$playerCount = "";
        serverlens$versionText = "";
        serverlens$pingValue = SERVERLENS$UNKNOWN_PING;
        serverlens$serverState = SERVERLENS$STATE_PINGING;
        serverlens$updateFavicon(null);
        serverlens$pendingAddress = address;
        serverlens$pendingPingAt = System.currentTimeMillis() + SERVERLENS$PING_DEBOUNCE_MS;
        serverlens$currentServer = null;
    }

    @Unique
    private void serverlens$tickServerPing() {
        long now = System.currentTimeMillis();
        if (!serverlens$pendingAddress.isEmpty() && now >= serverlens$pendingPingAt) {
            serverlens$startServerPing(serverlens$pendingAddress);
            serverlens$pendingAddress = "";
        }

        try {
            serverlens$pinger.tick();
        } catch (Exception e) {
            if (Main.isDebugMode()) {
                SERVERLENS$LOGGER.debug("Server ping failed", e);
            }
            serverlens$markUnreachable();
            serverlens$cancelPings();
        }

        if (serverlens$currentServer == null) {
            return;
        }

        if (serverlens$hasServerResponse(serverlens$currentServer)) {
            if (serverlens$serverDataAt == 0L) {
                serverlens$serverDataAt = now;
            }
            serverlens$applyServerInfo(serverlens$currentServer, false);

            if (serverlens$currentServer.ping >= SERVERLENS$MIN_KNOWN_PING || now - serverlens$serverDataAt >= SERVERLENS$SERVER_DATA_GRACE_MS) {
                serverlens$applyServerInfo(serverlens$currentServer, true);
                serverlens$currentServer = null;
            }
            return;
        }

        if (now - serverlens$activePingStartedAt >= SERVERLENS$PING_TIMEOUT_MS) {
            serverlens$serverName = serverlens$lastAddress;
            serverlens$motdText = Text.translatable(SERVERLENS$CANNOT_CONNECT_TEXT);
            serverlens$playerCount = "";
            serverlens$versionText = "";
            serverlens$pingValue = SERVERLENS$UNKNOWN_PING;
            serverlens$serverState = SERVERLENS$STATE_UNREACHABLE;
            serverlens$currentServer = null;
            serverlens$cancelPings();
        }
    }

    @Unique
    private void serverlens$startServerPing(String address) {
        if (!serverlens$shouldPingAddress(address)) {
            serverlens$markUnreachable();
            return;
        }

        serverlens$cancelPings();

        ServerInfo server = new ServerInfo(address, address, ServerInfo.ServerType.OTHER);
        server.setStatus(ServerInfo.Status.PINGING);
        serverlens$currentServer = server;
        serverlens$activePingStartedAt = System.currentTimeMillis();
        serverlens$serverDataAt = 0L;

        SERVERLENS$PING_EXECUTOR.execute(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                serverlens$pinger.add(server, () -> {
                }, () -> {
                    server.setStatus(server.protocolVersion == SharedConstants.getProtocolVersion()
                            ? ServerInfo.Status.SUCCESSFUL
                            : ServerInfo.Status.INCOMPATIBLE);
                }, NetworkingBackend.remote(client.options.shouldUseNativeTransport()));
            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() -> {
                    if (server == serverlens$currentServer && address.equals(serverlens$lastAddress)) {
                        serverlens$markUnreachable();
                    }
                });
            }
        });
    }

    @Unique
    private void serverlens$markUnreachable() {
        if (serverlens$currentServer == null && serverlens$lastAddress.isEmpty()) {
            return;
        }

        serverlens$serverName = serverlens$lastAddress;
        serverlens$motdText = Text.translatable(SERVERLENS$CANNOT_CONNECT_TEXT);
        serverlens$playerCount = "";
        serverlens$versionText = "";
        serverlens$pingValue = SERVERLENS$UNKNOWN_PING;
        serverlens$serverState = SERVERLENS$STATE_UNREACHABLE;
        serverlens$currentServer = null;
    }

    @Unique
    private void serverlens$cancelPings() {
        try {
            serverlens$pinger.cancel();
        } catch (Exception e) {
            if (Main.isDebugMode()) {
                SERVERLENS$LOGGER.debug("Failed to cancel server ping", e);
            }
        }
    }

    @Unique
    private static boolean serverlens$shouldPingAddress(String address) {
        String host = address.trim();
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            return end > 1;
        }
        int colon = host.lastIndexOf(':');
        if (colon > 0 && host.indexOf(':') == colon) {
            host = host.substring(0, colon);
        }

        if (host.equalsIgnoreCase("localhost") || host.matches("\\d{1,3}(\\.\\d{1,3}){3}") || host.indexOf(':') >= 0) {
            return true;
        }
        if (!host.contains(".") || host.startsWith(".") || host.endsWith(".")) {
            return false;
        }

        String[] labels = host.split("\\.");
        if (labels.length < 2 || labels[labels.length - 1].length() < 2) {
            return false;
        }
        for (String label : labels) {
            if (label.isEmpty() || label.startsWith("-") || label.endsWith("-") || !label.matches("[A-Za-z0-9-]+")) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private void serverlens$applyServerInfo(ServerInfo serverInfo, boolean finalState) {
        if (!serverInfo.address.equals(serverlens$lastAddress)) {
            return;
        }

        serverlens$serverName = Objects.requireNonNullElse(serverInfo.name, serverInfo.address);
        if (serverlens$hasMotd(serverInfo.label)) {
            serverlens$motdText = serverInfo.label;
        }

        ServerMetadata.Players players = serverInfo.players;
        serverlens$playerCount = players != null ? players.online() + SERVERLENS$PLAYER_COUNT_SEPARATOR + players.max() : "";
        serverlens$versionText = Objects.requireNonNullElse(serverInfo.version, Text.empty()).getString();
        serverlens$pingValue = serverInfo.ping >= SERVERLENS$MIN_KNOWN_PING ? serverInfo.ping : SERVERLENS$UNKNOWN_PING;
        serverlens$updateFavicon(serverInfo.getFavicon());

        if (serverlens$isIncompatible(serverInfo)) {
            serverlens$serverState = SERVERLENS$STATE_INCOMPATIBLE;
            return;
        }

        if (!finalState && serverInfo.ping < SERVERLENS$MIN_KNOWN_PING) {
            serverlens$serverState = SERVERLENS$STATE_PINGING;
            return;
        }

        serverlens$pingValue = serverInfo.ping >= SERVERLENS$MIN_KNOWN_PING ? serverInfo.ping : 0L;
        serverlens$serverState = SERVERLENS$STATE_SUCCESSFUL;
    }

    @Unique
    private static boolean serverlens$isIncompatible(ServerInfo serverInfo) {
        return serverInfo.protocolVersion > 0 && serverInfo.protocolVersion != SharedConstants.getProtocolVersion();
    }

    @Unique
    private static boolean serverlens$hasServerResponse(ServerInfo serverInfo) {
        return serverInfo.players != null || serverInfo.getFavicon() != null || serverInfo.getStatus() == ServerInfo.Status.SUCCESSFUL || serverInfo.getStatus() == ServerInfo.Status.INCOMPATIBLE || serverlens$hasMotd(serverInfo.label);
    }

    @Unique
    private static boolean serverlens$hasMotd(Text label) {
        if (label == null || serverlens$isTranslatable(label, SERVERLENS$PINGING_TEXT) || serverlens$isTranslatable(label, SERVERLENS$CANNOT_CONNECT_TEXT)) {
            return false;
        }
        return !label.getString().isBlank();
    }

    @Unique
    private static boolean serverlens$isTranslatable(Text text, String key) {
        return text.getContent() instanceof TranslatableTextContent content && content.getKey().equals(key);
    }

    @Unique
    private void serverlens$renderServerIcon(DrawContext graphics, int baseX, int baseY, int iconSize) {
        Identifier icon = Objects.requireNonNullElse(serverlens$serverIcon, SERVERLENS$DEFAULT_ICON);
        graphics.drawTexture(RenderPipelines.GUI_TEXTURED, icon, baseX, baseY, SERVERLENS$TEXTURE_U, SERVERLENS$TEXTURE_V, iconSize, iconSize, iconSize, iconSize);
    }

    @Unique
    private void serverlens$renderServerText(DrawContext graphics, int textX, int baseY, int nameWidth, int textWidth, int availableHeight) {
        TextRenderer font = this.textRenderer;
        graphics.drawText(font, Text.literal(font.trimToWidth(serverlens$serverName, nameWidth)), textX, baseY + 1, SERVERLENS$COLOR_WHITE, SERVERLENS$TEXT_SHADOW);

        if (!serverlens$motdText.getString().isEmpty()) {
            int motdY = baseY + SERVERLENS$MOTD_Y_OFFSET;
            int motdWidth = Math.max(textWidth, font.fontHeight);
            List<OrderedText> lines = font.wrapLines(serverlens$motdText, motdWidth);
            int maxLines = MathHelper.clamp((availableHeight - SERVERLENS$MOTD_Y_OFFSET) / font.fontHeight, 0, SERVERLENS$MAX_MOTD_LINES);
            for (int lineIndex = 0; lineIndex < maxLines && lineIndex < lines.size(); lineIndex++) {
                OrderedText line = lines.get(lineIndex);
                graphics.drawText(font, line, textX, motdY, SERVERLENS$COLOR_MOTD, SERVERLENS$TEXT_SHADOW);
                motdY += font.fontHeight;
            }
        }
    }

    @Unique
    private int serverlens$getNameWidth(int baseX, int rowWidth, int textX) {
        int pingX = baseX + rowWidth - SERVERLENS$PING_X_OFFSET;
        int rightLimit = pingX - SERVERLENS$RIGHT_TEXT_GAP - serverlens$getPlayerCountWidth();
        return MathHelper.clamp(rightLimit - textX, this.textRenderer.fontHeight, rowWidth);
    }

    @Unique
    private int serverlens$getPlayerCountWidth() {
        if (serverlens$serverState == SERVERLENS$STATE_INCOMPATIBLE && !serverlens$versionText.isBlank()) {
            return this.textRenderer.getWidth(serverlens$versionText) + SERVERLENS$PLAYER_COUNT_GAP;
        }
        if (serverlens$serverState != SERVERLENS$STATE_SUCCESSFUL || serverlens$playerCount.isBlank() || !serverlens$playerCount.contains(SERVERLENS$PLAYER_COUNT_SEPARATOR)) {
            return 0;
        }

        String[] parts = serverlens$playerCount.split(SERVERLENS$PLAYER_COUNT_SEPARATOR, 2);
        if (parts.length < 2) {
            return 0;
        }

        TextRenderer font = this.textRenderer;
        return font.getWidth(parts[0]) + font.getWidth(SERVERLENS$PLAYER_COUNT_SEPARATOR) + font.getWidth(parts[1]) + SERVERLENS$PLAYER_COUNT_GAP;
    }

    @Unique
    private void serverlens$renderPing(DrawContext graphics, int baseX, int rowWidth, int baseY) {
        Identifier pingTexture = serverlens$getPingTexture();
        int pingX = baseX + rowWidth - SERVERLENS$PING_X_OFFSET;
        graphics.drawTexture(RenderPipelines.GUI_TEXTURED, pingTexture, pingX, baseY, SERVERLENS$TEXTURE_U, SERVERLENS$TEXTURE_V, SERVERLENS$PING_WIDTH, SERVERLENS$PING_HEIGHT, SERVERLENS$PING_WIDTH, SERVERLENS$PING_HEIGHT);

        serverlens$renderPlayerCount(graphics, pingX, baseY);
    }

    @Unique
    private void serverlens$renderPlayerCount(DrawContext graphics, int pingX, int baseY) {
        if (serverlens$serverState == SERVERLENS$STATE_INCOMPATIBLE && !serverlens$versionText.isBlank()) {
            int versionWidth = this.textRenderer.getWidth(serverlens$versionText);
            graphics.drawText(this.textRenderer, Text.literal(serverlens$versionText), pingX - versionWidth - SERVERLENS$PLAYER_COUNT_GAP, baseY, SERVERLENS$COLOR_INCOMPATIBLE_VERSION, SERVERLENS$TEXT_SHADOW);
            return;
        }

        if (serverlens$serverState == SERVERLENS$STATE_SUCCESSFUL && !serverlens$playerCount.isBlank() && serverlens$playerCount.contains(SERVERLENS$PLAYER_COUNT_SEPARATOR)) {
            String[] parts = serverlens$playerCount.split(SERVERLENS$PLAYER_COUNT_SEPARATOR, 2);
            String players = parts[0];
            String maxPlayers = parts[1];

            TextRenderer font = this.textRenderer;
            int playersWidth = font.getWidth(players);
            int slashWidth = font.getWidth(SERVERLENS$PLAYER_COUNT_SEPARATOR);
            int maxPlayersWidth = font.getWidth(maxPlayers);
            int playerTextX = pingX - (playersWidth + slashWidth + maxPlayersWidth) - SERVERLENS$PLAYER_COUNT_GAP;

            graphics.drawText(font, Text.literal(players), playerTextX, baseY, SERVERLENS$COLOR_PLAYER_COUNT, SERVERLENS$TEXT_SHADOW);
            graphics.drawText(font, Text.literal(SERVERLENS$PLAYER_COUNT_SEPARATOR), playerTextX + playersWidth, baseY, SERVERLENS$COLOR_PLAYER_COUNT_SLASH, SERVERLENS$TEXT_SHADOW);
            graphics.drawText(font, Text.literal(maxPlayers), playerTextX + playersWidth + slashWidth, baseY, SERVERLENS$COLOR_PLAYER_COUNT, SERVERLENS$TEXT_SHADOW);
        }
    }

    @Unique
    private Identifier serverlens$getPingTexture() {
        if (serverlens$serverState == SERVERLENS$STATE_UNREACHABLE) {
            return SERVERLENS$UNREACHABLE;
        }
        if (serverlens$serverState == SERVERLENS$STATE_INCOMPATIBLE) {
            return SERVERLENS$INCOMPATIBLE;
        }
        if (serverlens$serverState == SERVERLENS$STATE_PINGING || serverlens$pingValue < SERVERLENS$MIN_KNOWN_PING) {
            long tick = System.currentTimeMillis() / SERVERLENS$PING_ANIMATION_INTERVAL_MS;
            int frame = (int) (tick % SERVERLENS$PING_ANIMATION_FRAME_COUNT);
            if (frame > SERVERLENS$PING_ANIMATION_PEAK_FRAME) {
                frame = SERVERLENS$PING_ANIMATION_FRAME_COUNT - frame;
            }
            return SERVERLENS$PINGING_TEXTURES[frame];
        }
        if (serverlens$pingValue < SERVERLENS$GOOD_PING_MS) {
            return serverlens$pingTexture(SERVERLENS$EXCELLENT_PING_BARS);
        }
        if (serverlens$pingValue < SERVERLENS$OK_PING_MS) {
            return serverlens$pingTexture(SERVERLENS$GOOD_PING_BARS);
        }
        if (serverlens$pingValue < SERVERLENS$SLOW_PING_MS) {
            return serverlens$pingTexture(SERVERLENS$OK_PING_BARS);
        }
        if (serverlens$pingValue < SERVERLENS$BAD_PING_MS) {
            return serverlens$pingTexture(SERVERLENS$SLOW_PING_BARS);
        }
        return serverlens$pingTexture(SERVERLENS$POOR_PING_BARS);
    }

    @Unique
    private static Identifier serverlens$texture(String name) {
        return Identifier.of(SERVERLENS$MOD_ID, SERVERLENS$SERVER_LIST_TEXTURE_PATH + name + ".png");
    }

    @Unique
    private static Identifier serverlens$pingTexture(int bars) {
        return SERVERLENS$PING_TEXTURES[bars - SERVERLENS$MIN_PING_BARS];
    }

    @Unique
    private void serverlens$clearServerIcon() {
        if (serverlens$serverIcon != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(serverlens$serverIcon);
            serverlens$serverIcon = null;
        }
    }
}
