package dev.dfonline.flint.feature.impl;

import dev.dfonline.flint.Flint;
import dev.dfonline.flint.feature.trait.PacketListeningFeature;
import dev.dfonline.flint.feature.trait.TickedFeature;
import dev.dfonline.flint.util.RateLimiter;
import dev.dfonline.flint.util.result.EventResult;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;

import java.util.ArrayDeque;

/**
 * Queues up commands and sends them while avoiding getting kicked for spam.
 */
public final class CommandSenderFeature implements PacketListeningFeature, TickedFeature {

    // Vanilla Minecraft uses an increment of 20 and a threshold of 200.
    // We have a lower threshold for extra safety and to account for lag.
    private static final RateLimiter rateLimiter = new RateLimiter(20, 160);
    private static final ArrayDeque<String> priorityQueue = new ArrayDeque<>();
    private static final ArrayDeque<String> queue = new ArrayDeque<>();

    public static void queue(String command) {
        queue.add(normalizeCommand(command));
    }

    static void queueInternal(String command) {
        priorityQueue.add(normalizeCommand(command));
    }

    public static void clearQueue() {
        priorityQueue.clear();
        queue.clear();
    }

    public static int queueSize() {
        return priorityQueue.size() + queue.size();
    }

    public static boolean isRateLimited() {
        return rateLimiter.isRateLimited();
    }

    public static int rateLimitCount() {
        return rateLimiter.getCount();
    }

    public static int rateLimitThreshold() {
        return rateLimiter.getThreshold();
    }

    public static int rateLimitIncrementStep() {
        return rateLimiter.getIncrementStep();
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
            sendNextCommand(networkHandler);
        }
    }

    @Override
    public EventResult onSendPacket(Packet<?> packet) {
        if (packet instanceof CommandExecutionC2SPacket || packet instanceof ChatMessageC2SPacket) {
            rateLimiter.increment();
        }

        return EventResult.PASS;
    }

    private static void sendNextCommand(ClientPlayNetworkHandler networkHandler) {
        ArrayDeque<String> commandQueue = priorityQueue.isEmpty() ? queue : priorityQueue;
        String command = commandQueue.poll();
        if (command != null) {
            networkHandler.sendChatCommand(command);
        }
    }

    private static String normalizeCommand(String command) {
        String normalizedCommand = command.trim();
        if (normalizedCommand.startsWith("/")) {
            normalizedCommand = normalizedCommand.substring(1);
        }

        return normalizedCommand;
    }

}
