package dev.dfonline.flint.feature.impl;

import dev.dfonline.flint.Flint;
import dev.dfonline.flint.feature.trait.ChatListeningFeature;
import dev.dfonline.flint.feature.trait.ConnectionListeningFeature;
import dev.dfonline.flint.hypercube.ServerPatch;
import dev.dfonline.flint.hypercube.ServerPatches;
import dev.dfonline.flint.util.result.ReplacementEventResult;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerPatchFeature implements ChatListeningFeature, ConnectionListeningFeature {

    private static final Pattern CURRENT_PATCH_MESSAGE_PATTERN =
            Pattern.compile("^Current patch: (?<patch>" + ServerPatch.REGEX + ")\\. See the patch notes with /patch!$");

    @Override
    public boolean alwaysOn() {
        return true;
    }

    @Override
    public void onDisconnect() {
        ServerPatches.clear();
    }

    @Override
    public ReplacementEventResult<Text> onChatMessage(Text text, boolean actionbar) {
        if (actionbar) {
            return ReplacementEventResult.pass();
        }

        String message = Formatting.strip(text.getString());

        if (message == null || message.isBlank()) {
            return ReplacementEventResult.pass();
        }

        Flint.getClient().execute(() -> this.processChatMessage(message));

        return ReplacementEventResult.pass();
    }

    private void processChatMessage(String message) {
        String patch = extractCurrentPatch(message);

        if (patch == null) {
            return;
        }

        ServerPatches.set(patch);
    }

    private static @Nullable String extractCurrentPatch(String message) {
        Matcher matcher = CURRENT_PATCH_MESSAGE_PATTERN.matcher(message);
        return matcher.find() ? matcher.group("patch") : null;
    }

}
