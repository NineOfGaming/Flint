package dev.dfonline.flint.feature.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.dfonline.flint.Flint;
import dev.dfonline.flint.FlintAPI;
import dev.dfonline.flint.feature.trait.CommandFeature;
import dev.dfonline.flint.hypercube.PlayerBadge;
import dev.dfonline.flint.hypercube.PlayerLocation;
import dev.dfonline.flint.hypercube.PlayerProfile;
import dev.dfonline.flint.hypercube.Plot;
import dev.dfonline.flint.util.FlintSound;
import dev.dfonline.flint.util.PaletteColor;
import dev.dfonline.flint.util.message.Message;
import dev.dfonline.flint.util.message.impl.CompoundMessage;
import dev.dfonline.flint.util.message.impl.SoundMessage;
import dev.dfonline.flint.util.message.impl.prefix.DebugMessage;
import dev.dfonline.flint.util.message.impl.prefix.ErrorMessage;
import dev.dfonline.flint.util.message.impl.prefix.InfoMessage;
import dev.dfonline.flint.util.message.impl.prefix.SuccessMessage;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.sound.SoundEvents;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class FlintCommandFeature implements CommandFeature {

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
                            debugLocate(Flint.getUser().getPlayer().getNameForScoreboard());
                            return 1;
                        })
                        .then(argument("player", StringArgumentType.greedyString())
                                .executes(context -> {
                                    debugLocate(StringArgumentType.getString(context, "player"));
                                    return 1;
                                })
                        )
                )
                .then(literal("whois")
                        .executes(context -> {
                            debugWhois(Flint.getUser().getPlayer().getNameForScoreboard());
                            return 1;
                        })
                        .then(argument("player", StringArgumentType.greedyString())
                                .executes(context -> {
                                    debugWhois(StringArgumentType.getString(context, "player"));
                                    return 1;
                                })
                        )
                )
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
            ModeTrackerFeature.requestCurrentUserProfile();
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
        LocateFeature.requestLocate(player).thenAccept(locate -> sendMessageOnClientThread(new DebugMessage(formatLocateResult(locate)))).exceptionally(throwable -> {
            sendMessageOnClientThread(new ErrorMessage("flint.command.flint.test.locate.fail", Component.text(throwable.getMessage())));
            return null;
        });
    }

    private static void debugWhois(String player) {
        WhoisFeature.requestWhois(player).thenAccept(profile -> sendMessageOnClientThread(new DebugMessage(formatProfileResult(profile)))).exceptionally(throwable -> {
            sendMessageOnClientThread(new ErrorMessage("flint.command.flint.test.whois.fail", Component.text(throwable.getMessage())));
            return null;
        });
    }

    private static void sendMessageOnClientThread(Message message) {
        Flint.getClient().execute(() -> {
            if (Flint.getClient().player != null) {
                Flint.getUser().sendMessage(message);
            }
        });
    }

    private static Component formatLocateResult(PlayerLocation locate) {
        Component report = debugReport("Locate Test");
        report = appendValue(report, "Player", locate.player());
        report = appendValue(report, "Mode", locate.mode().getName());
        report = appendValue(report, "Mode Enum", locate.mode().name());
        report = appendValue(report, "Node", locate.node() == null ? null : locate.node().getName());
        report = appendValue(report, "Node Enum", locate.node() == null ? null : locate.node().name());
        report = appendValue(report, "Node Id", locate.nodeId());

        Plot plot = locate.plot();
        if (plot == null) {
            return appendValue(report, "Plot", "none");
        }

        report = appendSection(report, "Plot");
        report = appendValue(report, "Plot Id", plot.getId());
        report = appendValue(report, "Name", plot.getName().getString());
        report = appendValue(report, "Handle", plot.getHandle());
        report = appendValue(report, "Whitelisted", plot.isWhitelisted());
        report = appendValue(report, "Owner", plot.getOwner());
        report = appendValue(report, "Owner Profile Loaded", plot.getOwnerProfile() != null);
        report = appendValue(report, "Owner Ranks", plot.getOwnerRanks().toReadableString());
        report = appendValue(report, "Dev Origin", plot.getDevOrigin());
        report = appendValue(report, "Detected Size", plot.getDetectedSize());
        report = appendValue(report, "Assumed Size", plot.getSize());
        report = appendValue(report, "Has Underground", plot.hasUnderground());
        report = appendValue(report, "Code Bounds", plot.getCodeBoundsString());
        return report;
    }

    private static Component formatProfileResult(PlayerProfile profile) {
        Component report = debugReport("Whois Test");
        report = appendValue(report, "Username", profile.userName());
        report = appendValue(report, "Pronouns", profile.pronouns());
        report = appendValue(report, "Joined", profile.joined());
        report = appendValue(report, "About", profile.about());
        report = appendValue(report, "Ranks", profile.ranks().toReadableString());
        report = appendValue(report, "Primary Rank", profile.ranks().getPrimaryRankName());
        report = appendValue(report, "Rank Levels", profile.ranks().toLevelString());

        if (profile.badges().isEmpty()) {
            return appendValue(report, "Badges", "none");
        }

        report = appendSection(report, "Badges");
        for (int i = 0; i < profile.badges().size(); i++) {
            PlayerBadge badge = profile.badges().get(i);
            report = appendValue(report, "Badge " + (i + 1), "text " + readable(badge.text()) + ", name " + readable(badge.name()) + ", description " + readable(badge.description()));
        }
        return report;
    }

    private static Component debugReport(String title) {
        return Component.text(title, PaletteColor.PINK_LIGHT);
    }

    private static Component appendSection(Component report, String section) {
        return report.append(Component.newline())
                .append(Component.text(section + ":", PaletteColor.PINK_LIGHT));
    }

    private static Component appendValue(Component report, String key, Object value) {
        return report.append(Component.newline())
                .append(Component.text(key, PaletteColor.PINK_LIGHT))
                .append(Component.text(": ", PaletteColor.GRAY_DARK))
                .append(Component.text(readable(value), PaletteColor.WHITE));
    }

    private static String readable(Object value) {
        if (value == null) {
            return "unknown";
        }

        if (value instanceof String string && string.isBlank()) {
            return "none";
        }

        return value.toString();
    }

    private static String readable(String value) {
        return readable((Object) value);
    }

}
