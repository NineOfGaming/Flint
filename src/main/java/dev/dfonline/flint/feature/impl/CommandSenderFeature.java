package dev.dfonline.flint.feature.impl;

import dev.dfonline.flint.Flint;
import dev.dfonline.flint.FlintAPI;
import dev.dfonline.flint.feature.trait.PacketListeningFeature;
import dev.dfonline.flint.feature.trait.TickedFeature;
import dev.dfonline.flint.hypercube.Mode;
import dev.dfonline.flint.util.RateLimiter;
import dev.dfonline.flint.util.result.EventResult;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;

import java.util.ArrayDeque;

/**
 * Queues up commands and plot commands and sends them while avoiding getting kicked for spam.
 */
public final class CommandSenderFeature implements PacketListeningFeature, TickedFeature {

    // Vanilla Minecraft uses an increment of 20 and a threshold of 200.
    // We have a lower threshold for extra safety and to account for lag.
    private static final RateLimiter rateLimiter = new RateLimiter(20, 160);
    private static final ArrayDeque<QueuedMessage> priorityQueue = new ArrayDeque<>();
    private static final ArrayDeque<QueuedMessage> queue = new ArrayDeque<>();

    public static void queue(String command) {
        queueCommand(command);
    }

    public static void queueCommand(String command) {
        queue.add(QueuedMessage.command(normalizeCommand(command)));
    }

    public static void queuePlotCommand(String command) {
        queue.add(QueuedMessage.plotCommand(normalizePlotCommand(command)));
    }

    static void queueInternalCommand(String command) {
        priorityQueue.add(QueuedMessage.command(normalizeCommand(command)));
    }

    public static void clearQueue() {
        priorityQueue.clear();
        queue.clear();
    }

    public static int queueSize() {
        return priorityQueue.size() + queue.size();
    }

    @Override
    public boolean alwaysOn() {
        return true;
    }

    @Override
    public void tick() {
        rateLimiter.tick();
        ClientPlayNetworkHandler networkHandler = Flint.getClient().getNetworkHandler();
        if (networkHandler != null && !rateLimiter.isRateLimited() && queueSize() > 0) {
            // No need to increment here, since our packet listener will do that for us.
            sendNextMessage(networkHandler);
        }
    }

    @Override
    public EventResult onSendPacket(Packet<?> packet) {
        if (packet instanceof CommandExecutionC2SPacket || packet instanceof ChatMessageC2SPacket) {
            rateLimiter.increment();
        }

        return EventResult.PASS;
    }

    private static void sendNextMessage(ClientPlayNetworkHandler networkHandler) {
        ArrayDeque<QueuedMessage> messageQueue = priorityQueue.isEmpty() ? queue : priorityQueue;
        QueuedMessage message = messageQueue.peek();

        if (message == null) {
            return;
        }

        SendResult result = message.send(networkHandler);
        if (result != SendResult.WAIT) {
            messageQueue.pop();
        }
    }

    private static String normalizeCommand(String command) {
        String normalizedCommand = command.trim();
        if (normalizedCommand.startsWith("/")) {
            normalizedCommand = normalizedCommand.substring(1);
        }

        return normalizedCommand;
    }

    private static String normalizePlotCommand(String command) {
        String normalizedCommand = command.trim();
        if (normalizedCommand.startsWith("@")) {
            normalizedCommand = normalizedCommand.substring(1);
        }

        return normalizedCommand;
    }

    private enum QueuedMessageType {
        COMMAND,
        PLOT_COMMAND
    }

    private enum SendResult {
        SENT,
        CANCELLED,
        WAIT
    }

    private enum PlotCommandState {
        SEND,
        CANCEL,
        WAIT
    }

    private record QueuedMessage(QueuedMessageType type, String content) {

        private static QueuedMessage command(String command) {
            return new QueuedMessage(QueuedMessageType.COMMAND, command);
        }

        private static QueuedMessage plotCommand(String command) {
            return new QueuedMessage(QueuedMessageType.PLOT_COMMAND, command);
        }

        private SendResult send(ClientPlayNetworkHandler networkHandler) {
            if (this.type == QueuedMessageType.PLOT_COMMAND) {
                PlotCommandState state = getPlotCommandState();
                if (state == PlotCommandState.WAIT) {
                    return SendResult.WAIT;
                }
                if (state == PlotCommandState.CANCEL) {
                    return SendResult.CANCELLED;
                }
            }

            switch (this.type) {
                case COMMAND -> networkHandler.sendChatCommand(this.content);
                case PLOT_COMMAND -> networkHandler.sendChatMessage("@" + this.content);
            }
            return SendResult.SENT;
        }
    }

    private static PlotCommandState getPlotCommandState() {
        if (!FlintAPI.shouldConfirmLocationWithLocate()) {
            return PlotCommandState.SEND;
        }

        Mode mode = Flint.getUser().getMode();
        if (mode == Mode.PLAY) {
            return PlotCommandState.SEND;
        }

        return PlotCommandState.CANCEL;
    }
}
