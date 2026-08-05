package dev.dfonline.flint.feature.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.common.collect.Maps;
import dev.dfonline.flint.Flint;
import dev.dfonline.flint.actiondump.ActionDumpFormat;
import dev.dfonline.flint.feature.trait.ChatListeningFeature;
import dev.dfonline.flint.feature.trait.PacketListeningFeature;
import dev.dfonline.flint.hypercube.Node;
import dev.dfonline.flint.hypercube.PlayerLocation;
import dev.dfonline.flint.hypercube.ServerPatch;
import dev.dfonline.flint.hypercube.ServerPatches;
import dev.dfonline.flint.util.ComponentUtil;
import dev.dfonline.flint.util.file.FileUtil;
import dev.dfonline.flint.util.message.impl.prefix.ErrorMessage;
import dev.dfonline.flint.util.message.impl.prefix.SuccessMessage;
import dev.dfonline.flint.util.result.ReplacementEventResult;
import net.kyori.adventure.text.Component;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GetActionDumpFeature implements ChatListeningFeature, PacketListeningFeature {

    private static final int MS_IN_SEC = 1000;
    private static final Gson ACTION_DUMP_GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static boolean isPreparingActionDump = false;
    private static boolean isGettingActionDump = false;
    private static int lines;
    private static int length;
    private static long startTime;
    private static @Nullable Node actionDumpNode;
    private static Map<ActionDumpFormat, StringBuilder> actionDumpProgression;

    public static void getActionDump(boolean allowNonObtainableActionDumpNodes) {
        if (isPreparingActionDump || isGettingActionDump) {
            return;
        }

        isPreparingActionDump = true;
        resolveActionDumpNode(allowNonObtainableActionDumpNodes)
                .thenAccept(node -> Flint.getClient().execute(() -> {
                    isPreparingActionDump = false;
                    startActionDump(allowNonObtainableActionDumpNodes, node);
                }))
                .exceptionally(throwable -> {
                    Flint.getClient().execute(() -> {
                        isPreparingActionDump = false;
                        Flint.getUser().sendMessage(new ErrorMessage("flint.command.flint.action_dump.fail.node"));
                    });
                    return null;
                });
    }

    private static CompletableFuture<Node> resolveActionDumpNode(boolean allowNonObtainableActionDumpNodes) {
        Node node = Flint.getUser().getNode();

        if (Flint.getUser().isLocationConfirmed() && node != null) {
            return CompletableFuture.completedFuture(node);
        }

        if (Flint.getClient().player == null) {
            return CompletableFuture.completedFuture(node);
        }

        String playerName = Flint.getClient().player.getGameProfile().name();
        return LocateFeature.requestLocate(playerName).thenApply(PlayerLocation::node);
    }

    private static void startActionDump(boolean allowNonObtainableActionDumpNodes, Node node) {
        if (isGettingActionDump) {
            return;
        }

        if (node == null || (!allowNonObtainableActionDumpNodes && !node.isActionDumpObtainable())) {
            Flint.getUser().sendMessage(new ErrorMessage("flint.command.flint.action_dump.fail.node"));
            return;
        }

        isGettingActionDump = true;
        actionDumpNode = node;
        ClientPlayNetworkHandler networkHandler = Flint.getClient().getNetworkHandler();

        if (networkHandler == null) {
            isGettingActionDump = false;
            actionDumpNode = null;
            return;
        }

        actionDumpProgression = Maps.newHashMap();

        for(var format : ActionDumpFormat.values()) {
            actionDumpProgression.put(format, new StringBuilder());
        }
        lines = 0;
        length = 0;
        startTime = System.currentTimeMillis();
        networkHandler.sendChatCommand("dumpactioninfo");
    }

    @Override
    public ReplacementEventResult<Text> onChatMessage(Text text, boolean actionbar) {
        if (actionDumpProgression == null || !isGettingActionDump) {
            return ReplacementEventResult.pass();
        }

        if (text.getString().startsWith("Error: ")) {
            isGettingActionDump = false;
            actionDumpNode = null;
            Flint.getUser().sendMessage(new ErrorMessage("flint.command.flint.action_dump.fail.start"));
            return ReplacementEventResult.cancel();
        }

        for(var format : ActionDumpFormat.values()) {
            ComponentUtil.textToString(text, actionDumpProgression.get(format), format);
            actionDumpProgression.get(format).append("\n");
        }
        String content = text.getString();
        lines += 1;
        length += content.length();
        Flint.getUser().sendMessage(new SuccessMessage("flint.command.flint.action_dump.progress", true, Component.text((float) (System.currentTimeMillis() - startTime) / MS_IN_SEC), Component.text(lines), Component.text(length)));

        if (text.getString().equals("}")) {
            isGettingActionDump = false;
            try {
                for(var format : ActionDumpFormat.values()) {
                    var capturedData = actionDumpProgression.get(format);
                    FileUtil.writeFile(format.getFile().getPath(), addVersion(capturedData.toString(), actionDumpNode));
                }
                Flint.getUser().sendMessage(new SuccessMessage("flint.command.flint.action_dump.success", Component.text((float) (System.currentTimeMillis() - startTime) / MS_IN_SEC), Component.text(lines), Component.text(length)));
            } catch (IOException | JsonParseException | IllegalStateException e) {
                Flint.getUser().sendMessage(new ErrorMessage("flint.command.flint.action_dump.fail.write"));
                return ReplacementEventResult.cancel();
            } finally {
                // Let the garbage collector do its job.
                actionDumpProgression = null;
                actionDumpNode = null;
            }
        }

        return ReplacementEventResult.cancel();
    }

    private static String addVersion(String capturedData, @Nullable Node node) {
        JsonObject actionDump = JsonParser.parseString(capturedData).getAsJsonObject();
        ServerPatch version = ServerPatches.currentForNode(node);
        actionDump.add("version", version == null ? JsonNull.INSTANCE : new JsonPrimitive(version.value()));
        return ACTION_DUMP_GSON.toJson(actionDump);
    }

}
