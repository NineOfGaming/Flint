package dev.dfonline.flint.feature.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.dfonline.flint.Flint;
import dev.dfonline.flint.FlintAPI;
import dev.dfonline.flint.actiondump.ActionDump;
import dev.dfonline.flint.actiondump.ActionDumpFormat;
import dev.dfonline.flint.actiondump.gson.ComponentGson;
import dev.dfonline.flint.feature.trait.CommandFeature;
import dev.dfonline.flint.hypercube.Plot;
import dev.dfonline.flint.util.DebugReportUtil;
import dev.dfonline.flint.util.FlintSound;
import dev.dfonline.flint.util.message.MessageUtil;
import dev.dfonline.flint.util.message.impl.CompoundMessage;
import dev.dfonline.flint.util.message.impl.SoundMessage;
import dev.dfonline.flint.util.message.impl.prefix.DebugMessage;
import dev.dfonline.flint.util.message.impl.prefix.ErrorMessage;
import dev.dfonline.flint.util.message.impl.prefix.InfoMessage;
import dev.dfonline.flint.util.message.impl.prefix.SuccessMessage;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.minecraft.command.CommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.sound.SoundEvents;

import java.io.IOException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class FlintCommandFeature implements CommandFeature {

    private static final Gson ACTION_DUMP_GSON = new GsonBuilder()
            .registerTypeAdapter(Component.class, new ComponentGson())
            .setPrettyPrinting()
            .create();
    private static final DateTimeFormatter ACTION_DUMP_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss z");

    private static final SuggestionProvider<FabricClientCommandSource> ONLINE_PLAYERS = (context, builder) ->
            CommandSource.suggestMatching(context.getSource().getPlayerNames(), builder);

    @Override
    public String commandName() {
        return "flint";
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> createCommand(LiteralArgumentBuilder<FabricClientCommandSource> cmd, CommandRegistryAccess registryAccess) {
        return cmd.executes(context -> {
            Flint.getUser().sendMessage(new CompoundMessage(
                    new SuccessMessage("flint.command.flint"),
                    new SoundMessage(FlintSound.builder()
                            .setSound(SoundEvents.ENTITY_VILLAGER_YES)
                            .setPitch(2.0F)
                            .build()
                    )
            ));
            return 1;
        }).then(literal("mode")
                .executes(context -> {
                    Plot plot = Flint.getUser().getPlot();

                    String plotString = "null";
                    if (plot != null) {
                        plotString = plot.toReadableString();
                    }
                    String nodeString = "null";
                    if (Flint.getUser().getNode() != null) {
                        nodeString = Flint.getUser().getNode().getName();
                    }

                    Flint.getUser().sendMessage(new CompoundMessage(new InfoMessage("flint.command.flint.mode", Component.text(Flint.getUser().getMode().getName()), Component.text(plotString), Component.text(nodeString))));
                    return 1;
                })
        ).then(literal("clear_queue")
                .executes(context -> {
                    if (CommandSenderFeature.queueSize() > 0) {
                        CommandSenderFeature.clearQueue();
                        Flint.getUser().sendMessage(new SuccessMessage("flint.command.flint.clear_queue.success"));
                    } else {
                        Flint.getUser().sendMessage(new ErrorMessage("flint.command.flint.clear_queue.empty"));
                    }
                    return 1;
                })
        ).then(literal("debug")
                .executes(context -> {
                    toggleDebugging();
                    return 1;
                })
                .then(literal("enable")
                        .executes(context -> {
                            setDebugging(true);
                            return 1;
                        })
                )
                .then(literal("disable")
                        .executes(context -> {
                            setDebugging(false);
                            return 1;
                        })
                )
                .then(literal("clear_profile_cache")
                        .executes(context -> {
                            clearPlayerProfileCache();
                            return 1;
                        })
                )
                .then(literal("confirm_location")
                        .executes(context -> {
                            sendConfirmLocationWithLocateStatus();
                            return 1;
                        })
                        .then(literal("enable")
                                .executes(context -> {
                                    setUserConfirmLocationWithLocate(true);
                                    return 1;
                                })
                        )
                        .then(literal("disable")
                                .executes(context -> {
                                    setUserConfirmLocationWithLocate(false);
                                    return 1;
                                })
                        )
                )
                .then(literal("user_profile")
                        .executes(context -> {
                            sendUserProfileWithWhoisStatus();
                            return 1;
                        })
                        .then(literal("enable")
                                .executes(context -> {
                                    setUserFetchUserProfileWithWhois(true);
                                    return 1;
                                })
                        )
                        .then(literal("disable")
                                .executes(context -> {
                                    setUserFetchUserProfileWithWhois(false);
                                    return 1;
                                })
                        )
                )
                .then(literal("plot_owner_profile")
                        .executes(context -> {
                            sendPlotOwnerProfileWithWhoisStatus();
                            return 1;
                        })
                        .then(literal("enable")
                                .executes(context -> {
                                    setUserFetchPlotOwnerProfileWithWhois(true);
                                    return 1;
                                })
                        )
                        .then(literal("disable")
                                .executes(context -> {
                                    setUserFetchPlotOwnerProfileWithWhois(false);
                                    return 1;
                                })
                        )
                )
        ).then(literal("test")
                .then(literal("locate")
                        .executes(context -> {
                            debugLocate(Flint.getUser().getPlayer().getGameProfile().name());
                            return 1;
                        })
                        .then(argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .executes(context -> {
                                    debugLocate(StringArgumentType.getString(context, "player"));
                                    return 1;
                                })
                        )
                )
                .then(literal("whois")
                        .executes(context -> {
                            debugWhois(Flint.getUser().getPlayer().getGameProfile().name());
                            return 1;
                        })
                        .then(argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .executes(context -> {
                                    debugWhois(StringArgumentType.getString(context, "player"));
                                    return 1;
                                })
                        )
                )
                .then(createActionDumpTestCommand())
        ).then(literal("action_dump")
                .executes(context -> {
                    GetActionDumpFeature.getActionDump(false);
                    return 1;
                })
                .then(literal("force")
                        .executes(context -> {
                            GetActionDumpFeature.getActionDump(true);
                            return 1;
                        })
                )
        );
    }

    private static void toggleDebugging() {
        setDebugging(!FlintAPI.isDebugging());
    }

    private static void setDebugging(boolean debugging) {
        FlintAPI.setDebugging(debugging);
        Flint.getUser().sendMessage(new SuccessMessage(debugging ? "flint.command.flint.debug.enabled" : "flint.command.flint.debug.disabled"));
    }

    private static void clearPlayerProfileCache() {
        FlintAPI.clearPlayerProfileCache();
        Flint.getUser().sendMessage(new SuccessMessage("flint.command.flint.debug.profile_cache.cleared"));
    }

    private static void sendConfirmLocationWithLocateStatus() {
        boolean userEnabled = FlintAPI.isUserConfirmLocationWithLocate();
        boolean modRequired = FlintAPI.isConfirmLocationWithLocateRequired();
        boolean ownerProfileEnabled = FlintAPI.shouldFetchPlotOwnerProfileWithWhois();

        if (modRequired) {
            Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.confirm_location.status.enabled.required"));
            return;
        }

        if (userEnabled) {
            Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.confirm_location.status.enabled.user"));
            return;
        }

        if (ownerProfileEnabled) {
            Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.confirm_location.status.enabled.plot_owner_profile"));
            return;
        }

        Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.confirm_location.status.disabled"));
    }

    private static void setUserConfirmLocationWithLocate(boolean userConfirmLocationWithLocate) {
        FlintAPI.setUserConfirmLocationWithLocate(userConfirmLocationWithLocate);

        if (userConfirmLocationWithLocate) {
            ModeTrackerFeature.confirmCurrentLocation();
            Flint.getUser().sendMessage(new SuccessMessage("flint.command.flint.confirm_location.enabled"));
            return;
        }

        if (FlintAPI.shouldConfirmLocationWithLocate()) {
            sendConfirmLocationWithLocateStatus();
            return;
        }

        Flint.getUser().sendMessage(new SuccessMessage("flint.command.flint.confirm_location.disabled"));
    }

    private static void sendUserProfileWithWhoisStatus() {
        if (FlintAPI.isFetchUserProfileWithWhoisRequired()) {
            Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.user_profile.status.enabled.required"));
            return;
        }

        if (FlintAPI.isUserFetchUserProfileWithWhois()) {
            Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.user_profile.status.enabled.user"));
            return;
        }

        Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.user_profile.status.disabled"));
    }

    private static void setUserFetchUserProfileWithWhois(boolean userFetchUserProfileWithWhois) {
        FlintAPI.setUserFetchUserProfileWithWhois(userFetchUserProfileWithWhois);

        if (userFetchUserProfileWithWhois) {
            ModeTrackerFeature.ensureCurrentUserProfile();
            Flint.getUser().sendMessage(new SuccessMessage("flint.command.flint.user_profile.enabled"));
            return;
        }

        if (FlintAPI.shouldFetchUserProfileWithWhois()) {
            Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.user_profile.required"));
            return;
        }

        Flint.getUser().sendMessage(new SuccessMessage("flint.command.flint.user_profile.disabled"));
    }

    private static void sendPlotOwnerProfileWithWhoisStatus() {
        if (FlintAPI.isFetchPlotOwnerProfileWithWhoisRequired()) {
            Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.plot_owner_profile.status.enabled.required"));
            return;
        }

        if (FlintAPI.isUserFetchPlotOwnerProfileWithWhois()) {
            Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.plot_owner_profile.status.enabled.user"));
            return;
        }

        Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.plot_owner_profile.status.disabled"));
    }

    private static void setUserFetchPlotOwnerProfileWithWhois(boolean userFetchPlotOwnerProfileWithWhois) {
        FlintAPI.setUserFetchPlotOwnerProfileWithWhois(userFetchPlotOwnerProfileWithWhois);

        if (userFetchPlotOwnerProfileWithWhois) {
            ModeTrackerFeature.requestCurrentPlotOwnerProfile();
            Flint.getUser().sendMessage(new SuccessMessage("flint.command.flint.plot_owner_profile.enabled"));
            return;
        }

        if (FlintAPI.shouldFetchPlotOwnerProfileWithWhois()) {
            Flint.getUser().sendMessage(new InfoMessage("flint.command.flint.plot_owner_profile.required"));
            return;
        }

        Flint.getUser().sendMessage(new SuccessMessage("flint.command.flint.plot_owner_profile.disabled"));
    }

    private static void debugLocate(String player) {
        LocateFeature.requestLocate(player).thenAccept(locate -> MessageUtil.sendOnClientThread(new DebugMessage(DebugReportUtil.formatLocateResult(locate)))).exceptionally(throwable -> {
            MessageUtil.sendOnClientThread(new ErrorMessage("flint.command.flint.test.locate.fail", Component.text(throwable.getMessage())));
            return null;
        });
    }

    private static void debugWhois(String player) {
        WhoisFeature.requestWhois(player).thenAccept(profile -> MessageUtil.sendOnClientThread(new DebugMessage(DebugReportUtil.formatProfileResult(profile)))).exceptionally(throwable -> {
            MessageUtil.sendOnClientThread(new ErrorMessage("flint.command.flint.test.whois.fail", Component.text(throwable.getMessage())));
            return null;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> createActionDumpTestCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> command = literal("action_dump")
                .executes(context -> sendActionDumpSummary(false))
                .then(literal("reload")
                        .executes(context -> sendActionDumpSummary(true))
                );

        command.then(createActionDumpEntryCommand("codeblock", "codeblocks", ActionDump::codeblocks));
        command.then(createActionDumpEntryCommand("action", "actions", ActionDump::actions));
        command.then(createActionDumpEntryCommand("game_value_category", "game value categories", ActionDump::gameValueCategories));
        command.then(createActionDumpEntryCommand("game_value", "game values", ActionDump::gameValues));
        command.then(createActionDumpEntryCommand("particle_category", "particle categories", ActionDump::particleCategories));
        command.then(createActionDumpEntryCommand("particle", "particles", ActionDump::particles));
        command.then(createActionDumpEntryCommand("sound_category", "sound categories", ActionDump::soundCategories));
        command.then(createActionDumpEntryCommand("sound", "sounds", ActionDump::sounds));
        command.then(createActionDumpEntryCommand("potion", "potions", ActionDump::potions));
        command.then(createActionDumpEntryCommand("cosmetic", "cosmetics", ActionDump::cosmetics));
        command.then(createActionDumpEntryCommand("shop", "shops", ActionDump::shops));
        return command;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> createActionDumpEntryCommand(
            String commandName,
            String entryName,
            Function<ActionDump, Object[]> entries
    ) {
        return literal(commandName)
                .then(argument("index", IntegerArgumentType.integer(0))
                        .executes(context -> sendActionDumpEntry(
                                entryName,
                                IntegerArgumentType.getInteger(context, "index"),
                                entries
                        ))
                );
    }

    private static int sendActionDumpSummary(boolean reload) {
        try {
            ActionDump actionDump = reload ? ActionDump.reload() : ActionDump.get();
            ActionDumpFileMetadata metadata = actionDumpFileMetadata();
            Flint.getUser().sendMessage(new DebugMessage(DebugReportUtil.formatActionDumpSummary(
                    actionDump,
                    metadata.fileSize(),
                    metadata.generated()
            )));
            return 1;
        } catch (RuntimeException exception) {
            return sendActionDumpError(exception);
        }
    }

    private static ActionDumpFileMetadata actionDumpFileMetadata() {
        try {
            var path = ActionDumpFormat.MINI_MESSAGE.getFile().getPath();
            long bytes = Files.size(path);
            String modified = Files.getLastModifiedTime(path)
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .format(ACTION_DUMP_TIME_FORMAT);
            return new ActionDumpFileMetadata(readableFileSize(bytes), modified);
        } catch (IOException exception) {
            return new ActionDumpFileMetadata("unknown", "unknown");
        }
    }

    private static String readableFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        }

        double kibibytes = bytes / 1024.0;
        if (kibibytes < 1024) {
            return String.format(Locale.ROOT, "%.2f KiB (%,d bytes)", kibibytes, bytes);
        }

        return String.format(Locale.ROOT, "%.2f MiB (%,d bytes)", kibibytes / 1024.0, bytes);
    }

    private static int sendActionDumpEntry(String entryName, int index, Function<ActionDump, Object[]> entries) {
        try {
            Object[] actionDumpEntries = entries.apply(ActionDump.get());
            if (actionDumpEntries == null || actionDumpEntries.length == 0) {
                Flint.getUser().sendMessage(new ErrorMessage(
                        "flint.command.flint.test.action_dump.empty",
                        Component.text(entryName)
                ));
                return 0;
            }

            if (index >= actionDumpEntries.length) {
                Flint.getUser().sendMessage(new ErrorMessage(
                        "flint.command.flint.test.action_dump.out_of_bounds",
                        Component.text(entryName),
                        Component.text(index),
                        Component.text(actionDumpEntries.length - 1)
                ));
                return 0;
            }

            Flint.getUser().sendMessage(new DebugMessage(DebugReportUtil.formatActionDumpEntry(
                    entryName,
                    index,
                    ACTION_DUMP_GSON.toJson(actionDumpEntries[index])
            )));
            return 1;
        } catch (RuntimeException exception) {
            return sendActionDumpError(exception);
        }
    }

    private static int sendActionDumpError(RuntimeException exception) {
        Flint.getUser().sendMessage(new ErrorMessage(
                "flint.command.flint.test.action_dump.fail",
                Component.text(readable(exception.getMessage()))
        ));
        return 0;
    }

    private static String readable(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private record ActionDumpFileMetadata(String fileSize, String generated) {
    }

}
