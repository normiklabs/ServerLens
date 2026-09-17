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

package org.normik.serverlens.logic;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.EventLoopGroupHolder;

import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class DirectConnectLogic {

    private static final long PING_TICK_SLEEP_MS = 50L;
    private static final long PING_TICK_PAUSE_NANOS = TimeUnit.MILLISECONDS.toNanos(PING_TICK_SLEEP_MS);
    private static final long UNKNOWN_PING = -1L;
    private static final Runnable NO_OP = DirectConnectLogic::noop;

    private DirectConnectLogic() {
    }

    @FunctionalInterface
    public interface PingCallback {
        void onFinished(ServerData serverInfo);
    }

    public static void pingServer(String address, PingCallback callback) {
        ServerData serverInfo = new ServerData(address, address, ServerData.Type.OTHER);
        ServerStatusPinger pinger = new ServerStatusPinger();
        AtomicBoolean finished = new AtomicBoolean(false);

        serverInfo.ping = UNKNOWN_PING;
        serverInfo.setState(ServerData.State.PINGING);

        try {
            pinger.pingServer(serverInfo, NO_OP, () -> finish(callback, serverInfo, finished), EventLoopGroupHolder.remote(true));

            while (!finished.get()) {
                pinger.tick();
                LockSupport.parkNanos(PING_TICK_PAUSE_NANOS);
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (UnknownHostException ignored) {
            serverInfo.motd = Component.literal("Unknown host");
            serverInfo.status = Component.empty();
            serverInfo.setState(ServerData.State.UNREACHABLE);
            finish(callback, serverInfo, finished);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            serverInfo.motd = Component.literal("Ping cancelled");
            serverInfo.status = Component.empty();
            serverInfo.setState(ServerData.State.UNREACHABLE);
            finish(callback, serverInfo, finished);
        } catch (RuntimeException ignored) {
            serverInfo.motd = Component.literal("Cannot connect");
            serverInfo.status = Component.empty();
            serverInfo.setState(ServerData.State.UNREACHABLE);
            finish(callback, serverInfo, finished);
        } finally {
            pinger.removeAll();
        }
    }

    private static void finish(PingCallback callback, ServerData serverInfo, AtomicBoolean finished) {
        if (finished.compareAndSet(false, true)) {
            callback.onFinished(serverInfo);
        }
    }

    @SuppressWarnings("EmptyMethod")
    private static void noop() {
    }
}
