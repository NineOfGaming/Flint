package dev.dfonline.flint.util.message;

import dev.dfonline.flint.Flint;

public final class MessageUtil {

    private MessageUtil() {
    }

    public static void sendOnClientThread(Message message) {
        Flint.getClient().execute(() -> {
            if (Flint.getClient().player != null) {
                Flint.getUser().sendMessage(message);
            }
        });
    }

}
