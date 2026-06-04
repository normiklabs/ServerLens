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

package de.devknochen.serverlens;

import de.devknochen.serverlens.logic.DirectConnectLogic;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.status.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final long PING_DEBOUNCE_MS = 500L;
    private static final String PINGER_THREAD_NAME = "ServerLens-Pinger";
    private static final String PLAYER_COUNT_SEPARATOR = "/";
    private static final boolean DEBUG_MODE = Boolean.getBoolean("serverlens.debug");
    private static final long MIN_KNOWN_PING = 0L;
    private static String lastAddress = "";
    private static long lastPingTime;

    @Override
    public void onInitializeClient() {
        LOGGER.info("ServerLens initialized");
    }

    public static void onAddressBarUpdate(String address) {
        if (address == null || address.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (address.equals(lastAddress) && now - lastPingTime < PING_DEBOUNCE_MS) {
            return;
        }

        lastAddress = address;
        lastPingTime = now;

        Thread.ofPlatform()
                .name(PINGER_THREAD_NAME)
                .start(() -> DirectConnectLogic.pingServer(address, Main::handlePingResult));
    }

    public static boolean isDebugMode() {
        return DEBUG_MODE;
    }

    private static void handlePingResult(ServerData serverInfo) {
        ServerStatus.Players players = serverInfo.players;
        String playerCount = players != null ? players.online() + PLAYER_COUNT_SEPARATOR + players.max() : "";

        if (DEBUG_MODE) {
            logPingResult(serverInfo, playerCount);
        }

        Minecraft client = Minecraft.getInstance();
        client.execute(() -> updateCurrentScreen(client, serverInfo, playerCount));
    }

    private static void updateCurrentScreen(Minecraft client, ServerData serverInfo, String playerCount) {
        Screen screen = client.screen;
        if (screen instanceof ServerDataUpdater updater) {
            ServerData.State state = normalizedState(serverInfo);
            updater.updateServerData(
                    serverInfo.name,
                    serverInfo.motd,
                    playerCount,
                    serverInfo.ping,
                    state
            );
            updater.updateFavicon(serverInfo.getIconBytes());
        }
    }

    private static ServerData.State normalizedState(ServerData serverInfo) {
        ServerData.State state = serverInfo.state();
        if (state == ServerData.State.PINGING && serverInfo.ping >= MIN_KNOWN_PING) {
            return ServerData.State.SUCCESSFUL;
        }
        return state;
    }

    private static void logPingResult(ServerData serverInfo, String playerCount) {
        LOGGER.debug(
                "Server ping result: motd={}, players={}, ping={}, version={}, state={}",
                serverInfo.motd.getString(),
                !playerCount.isEmpty() ? playerCount : "0/0",
                serverInfo.ping,
                serverInfo.version.getString(),
                serverInfo.state()
        );
    }
}
