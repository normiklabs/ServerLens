/*
 * Copyright 2026 The normik Authors
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

package org.normik.serverlens.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.normik.serverlens.Main;
import org.normik.serverlens.ServerDataUpdater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Mixin(DirectJoinServerScreen.class)
@Implements(@Interface(iface = ServerDataUpdater.class, prefix = "serverlensUpdater$"))
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public abstract class DirectConnectScreenMixin extends Screen {

    @Unique
    private static final Logger SERVERLENS$LOGGER = LoggerFactory.getLogger(DirectConnectScreenMixin.class);
    @Unique
    private static final String SERVERLENS$MOD_ID = "serverlens";
    @Unique
    private static final String SERVERLENS$SERVER_LIST_TEXTURE_PATH = "gui/serverlist/";
    @Unique
    private static final String SERVERLENS$DIRECT_CONNECT_ICON_ID = "directconnectmotd";
    @Unique
    private static final String SERVERLENS$ICON_ID_SEPARATOR = ":";
    @Unique
    private static final String SERVERLENS$PLAYER_COUNT_SEPARATOR = "/";

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
    private EditBox ipEdit;

    @Unique
    private String serverlens$lastAddress = "";
    @Unique
    private String serverlens$serverName = "";
    @Unique
    private Component serverlens$motdText = Component.empty();
    @Unique
    private String serverlens$playerCount = "";
    @Unique
    private long serverlens$pingValue = SERVERLENS$UNKNOWN_PING;
    @Unique
    private ServerData.State serverlens$serverState = ServerData.State.INITIAL;
    @Unique
    private FaviconTexture serverlens$serverIcon;
    @Unique
    private byte[] serverlens$lastFavicon;

    protected DirectConnectScreenMixin(Component title) {
        super(title);
    }

    public void serverlensUpdater$updateServerData(String name, Component motd, String players, long ping, ServerData.State state) {
        this.serverlens$serverName = name != null ? name : "";
        this.serverlens$motdText = motd != null ? motd : Component.empty();
        this.serverlens$playerCount = players != null ? players : "";
        this.serverlens$pingValue = ping;
        this.serverlens$serverState = state != null ? state : ServerData.State.INITIAL;
    }

    public void serverlensUpdater$updateFavicon(byte[] faviconBytes) {
        Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread()) {
            client.execute(() -> serverlensUpdater$updateFavicon(faviconBytes));
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
        byte[] valid = ServerData.validateIcon(faviconBytes);
        if (valid == null) {
            return;
        }
        serverlens$lastFavicon = Arrays.copyOf(faviconBytes, faviconBytes.length);

        try {
            serverlens$clearServerIcon();

            String idSource = SERVERLENS$DIRECT_CONNECT_ICON_ID + SERVERLENS$ICON_ID_SEPARATOR + serverlens$lastAddress;
            serverlens$serverIcon = FaviconTexture.forServer(client.getTextureManager(), idSource);
            serverlens$serverIcon.upload(NativeImage.read(valid));
        } catch (IOException | RuntimeException e) {
            serverlens$clearServerIcon();
            if (Main.isDebugMode()) {
                SERVERLENS$LOGGER.debug("Failed to upload server favicon", e);
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void serverlens$renderExtras(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        serverlens$updateAddressPreview();

        int rowWidth = Math.clamp(this.width - SERVERLENS$SIDE_PADDING * 2, SERVERLENS$MIN_ROW_WIDTH, SERVERLENS$MAX_ROW_WIDTH);
        int baseX = (this.width - rowWidth) / 2;
        int editorBottom = ipEdit != null ? ipEdit.getY() + ipEdit.getHeight() : this.height / 2 + SERVERLENS$FALLBACK_PREVIEW_Y_OFFSET;
        int buttonTop = this.height / 4 + SERVERLENS$JOIN_BUTTON_Y_OFFSET;
        int availableHeight = buttonTop - editorBottom - SERVERLENS$PREVIEW_TOP_PADDING - SERVERLENS$PREVIEW_BOTTOM_PADDING;
        if (availableHeight < this.font.lineHeight) {
            return;
        }

        int baseY = editorBottom + SERVERLENS$PREVIEW_TOP_PADDING;
        int iconSize = Math.min(availableHeight, SERVERLENS$ICON_SIZE);
        int textX = baseX + iconSize + SERVERLENS$TEXT_ICON_GAP;
        int textWidth = rowWidth - (textX - baseX) - SERVERLENS$TEXT_WIDTH_PADDING;
        int nameWidth = serverlens$getNameWidth(baseX, rowWidth, textX);

        serverlens$renderServerIcon(graphics, baseX, baseY, iconSize);
        serverlens$renderServerText(graphics, textX, baseY, nameWidth, textWidth, availableHeight);
        serverlens$renderPing(graphics, baseX, rowWidth, baseY);
    }

    @Unique
    private void serverlens$updateAddressPreview() {
        if (ipEdit == null) {
            return;
        }

        String address = ipEdit.getValue();
        if (address.isBlank()) {
            Main.onAddressBarUpdate(address);
            serverlens$lastAddress = "";
            return;
        }

        if (address.equals(serverlens$lastAddress)) {
            return;
        }

        serverlens$lastAddress = address;
        serverlens$serverName = address;
        serverlens$motdText = Component.translatable("multiplayer.status.pinging");
        serverlens$playerCount = "";
        serverlens$pingValue = SERVERLENS$UNKNOWN_PING;
        serverlens$serverState = ServerData.State.PINGING;
        serverlensUpdater$updateFavicon(null);
        Main.onAddressBarUpdate(address);
    }

    @Unique
    private void serverlens$renderServerIcon(GuiGraphicsExtractor graphics, int baseX, int baseY, int iconSize) {
        if (serverlens$serverIcon != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, serverlens$serverIcon.textureLocation(), baseX, baseY, SERVERLENS$TEXTURE_U, SERVERLENS$TEXTURE_V, iconSize, iconSize, iconSize, iconSize);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, SERVERLENS$DEFAULT_ICON, baseX, baseY, SERVERLENS$TEXTURE_U, SERVERLENS$TEXTURE_V, iconSize, iconSize, iconSize, iconSize);
        }
    }

    @Unique
    private void serverlens$renderServerText(GuiGraphicsExtractor graphics, int textX, int baseY, int nameWidth, int textWidth, int availableHeight) {
        Font font = this.font;
        graphics.text(font, Component.literal(font.plainSubstrByWidth(serverlens$serverName, nameWidth)), textX, baseY + 1, SERVERLENS$COLOR_WHITE, SERVERLENS$TEXT_SHADOW);

        if (!serverlens$motdText.getString().isEmpty()) {
            int motdY = baseY + SERVERLENS$MOTD_Y_OFFSET;
            int motdWidth = Math.max(textWidth, font.lineHeight);
            List<FormattedCharSequence> lines = font.split(serverlens$motdText, motdWidth);
            int maxLines = Math.clamp((availableHeight - SERVERLENS$MOTD_Y_OFFSET) / font.lineHeight, 0, SERVERLENS$MAX_MOTD_LINES);
            for (int lineIndex = 0; lineIndex < maxLines && lineIndex < lines.size(); lineIndex++) {
                FormattedCharSequence line = lines.get(lineIndex);
                graphics.text(font, line, textX, motdY, SERVERLENS$COLOR_MOTD, SERVERLENS$TEXT_SHADOW);
                motdY += font.lineHeight;
            }
        }
    }

    @Unique
    private int serverlens$getNameWidth(int baseX, int rowWidth, int textX) {
        int pingX = baseX + rowWidth - SERVERLENS$PING_X_OFFSET;
        int rightLimit = pingX - SERVERLENS$RIGHT_TEXT_GAP - serverlens$getPlayerCountWidth();
        return Math.clamp(rightLimit - textX, this.font.lineHeight, rowWidth);
    }

    @Unique
    private int serverlens$getPlayerCountWidth() {
        if (serverlens$serverState != ServerData.State.SUCCESSFUL || serverlens$playerCount.isBlank() || !serverlens$playerCount.contains(SERVERLENS$PLAYER_COUNT_SEPARATOR)) {
            return 0;
        }

        String[] parts = serverlens$playerCount.split(SERVERLENS$PLAYER_COUNT_SEPARATOR, 2);
        if (parts.length < 2) {
            return 0;
        }

        Font font = this.font;
        return font.width(parts[0]) + font.width(SERVERLENS$PLAYER_COUNT_SEPARATOR) + font.width(parts[1]) + SERVERLENS$PLAYER_COUNT_GAP;
    }

    @Unique
    private void serverlens$renderPing(GuiGraphicsExtractor graphics, int baseX, int rowWidth, int baseY) {
        Identifier pingTexture = serverlens$getPingTexture();
        int pingX = baseX + rowWidth - SERVERLENS$PING_X_OFFSET;
        graphics.blit(RenderPipelines.GUI_TEXTURED, pingTexture, pingX, baseY, SERVERLENS$TEXTURE_U, SERVERLENS$TEXTURE_V, SERVERLENS$PING_WIDTH, SERVERLENS$PING_HEIGHT, SERVERLENS$PING_WIDTH, SERVERLENS$PING_HEIGHT);

        serverlens$renderPlayerCount(graphics, pingX, baseY);
    }

    @Unique
    private void serverlens$renderPlayerCount(GuiGraphicsExtractor graphics, int pingX, int baseY) {
        if (serverlens$serverState == ServerData.State.SUCCESSFUL && !serverlens$playerCount.isBlank() && serverlens$playerCount.contains(SERVERLENS$PLAYER_COUNT_SEPARATOR)) {
            String[] parts = serverlens$playerCount.split(SERVERLENS$PLAYER_COUNT_SEPARATOR, 2);
            String players = parts[0];
            String maxPlayers = parts[1];

            Font font = this.font;
            int playersWidth = font.width(players);
            int slashWidth = font.width(SERVERLENS$PLAYER_COUNT_SEPARATOR);
            int maxPlayersWidth = font.width(maxPlayers);
            int playerTextX = pingX - (playersWidth + slashWidth + maxPlayersWidth) - SERVERLENS$PLAYER_COUNT_GAP;

            graphics.text(font, Component.literal(players), playerTextX, baseY, SERVERLENS$COLOR_PLAYER_COUNT, SERVERLENS$TEXT_SHADOW);
            graphics.text(font, Component.literal(SERVERLENS$PLAYER_COUNT_SEPARATOR), playerTextX + playersWidth, baseY, SERVERLENS$COLOR_PLAYER_COUNT_SLASH, SERVERLENS$TEXT_SHADOW);
            graphics.text(font, Component.literal(maxPlayers), playerTextX + playersWidth + slashWidth, baseY, SERVERLENS$COLOR_PLAYER_COUNT, SERVERLENS$TEXT_SHADOW);
        }
    }

    @Unique
    private Identifier serverlens$getPingTexture() {
        if (serverlens$serverState == ServerData.State.UNREACHABLE) {
            return SERVERLENS$UNREACHABLE;
        }
        if (serverlens$serverState == ServerData.State.INCOMPATIBLE) {
            return SERVERLENS$INCOMPATIBLE;
        }
        if (serverlens$serverState == ServerData.State.PINGING || serverlens$pingValue < SERVERLENS$MIN_KNOWN_PING) {
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
        return Identifier.fromNamespaceAndPath(SERVERLENS$MOD_ID, SERVERLENS$SERVER_LIST_TEXTURE_PATH + name + ".png");
    }

    @Unique
    private static Identifier serverlens$pingTexture(int bars) {
        return SERVERLENS$PING_TEXTURES[bars - SERVERLENS$MIN_PING_BARS];
    }

    @Unique
    private void serverlens$clearServerIcon() {
        if (serverlens$serverIcon != null) {
            serverlens$serverIcon.close();
            serverlens$serverIcon = null;
        }
    }
}
