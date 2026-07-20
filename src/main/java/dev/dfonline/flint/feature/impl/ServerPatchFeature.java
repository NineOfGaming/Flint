package dev.dfonline.flint.feature.impl;

import dev.dfonline.flint.Flint;
import dev.dfonline.flint.feature.trait.ChatListeningFeature;
import dev.dfonline.flint.feature.trait.ConnectionListeningFeature;
import dev.dfonline.flint.feature.trait.TickedFeature;
import dev.dfonline.flint.hypercube.Node;
import dev.dfonline.flint.hypercube.ServerPatch;
import dev.dfonline.flint.hypercube.ServerPatchTarget;
import dev.dfonline.flint.hypercube.ServerPatches;
import dev.dfonline.flint.util.result.ReplacementEventResult;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerPatchFeature implements ChatListeningFeature, ConnectionListeningFeature, TickedFeature {

    private static final Pattern CURRENT_PATCH_MESSAGE_PATTERN =
            Pattern.compile("^Current patch: (?<patch>" + ServerPatch.REGEX + ")\\. See the patch notes with /patch!$");

    private @Nullable String pendingPatch;
    private @Nullable Node nodeWhenPatchWasSeen;

    @Override
    public boolean alwaysOn() {
        return true;
    }

    @Override
    public void onJoin() {
        this.pendingPatch = null;
        this.nodeWhenPatchWasSeen = null;
    }

    @Override
    public void onDisconnect() {
        this.pendingPatch = null;
        this.nodeWhenPatchWasSeen = null;
        ServerPatches.clear();
    }

    @Override
    public void tick() {
        if (this.pendingPatch != null) {
            this.tryStorePatch();
        }
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

        this.pendingPatch = patch;
        this.nodeWhenPatchWasSeen = Flint.getUser().getNode();
        ServerPatches.setUnknown(patch);
        GetActionDumpFeature.refreshExistingActionDumpPatchMetadata();
        this.tryStorePatch();
    }

    private static @Nullable String extractCurrentPatch(String message) {
        Matcher matcher = CURRENT_PATCH_MESSAGE_PATTERN.matcher(message);
        return matcher.find() ? matcher.group("patch") : null;
    }

    private void tryStorePatch() {
        String patch = this.pendingPatch;
        if (patch == null) {
            return;
        }

        Node node = Flint.getUser().getNode();
        if (node == null) {
            return;
        }

        if (node == this.nodeWhenPatchWasSeen) {
            return;
        }

        ServerPatchTarget target = ServerPatchTarget.fromNode(node);
        if (target == null) {
            return;
        }

        ServerPatches.set(target, patch);
        GetActionDumpFeature.refreshExistingActionDumpPatchMetadata();
        this.pendingPatch = null;
        this.nodeWhenPatchWasSeen = null;
    }

}
