package dev.dfonline.flint.hypercube;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Node {

    NODE_1("node1", "Node 1", false),
    NODE_2("node2", "Node 2", false),
    NODE_3("node3", "Node 3", false),
    NODE_4("node4", "Node 4", false),
    NODE_5("node5", "Node 5", false),
    NODE_6("node6", "Node 6", false),
    NODE_7("node7", "Node 7", false),
    EVENT("event", "Event", false),
    BETA("beta", "Node Beta", true),
    DEV("dev", "Dev", true),
    DEV_2("dev2", "Dev 2", true),
    DEV_3("dev3", "Dev 3", true),
    DEV_4("dev4", "Dev 4", true),
    LOCAL("local", "Local", true),
    PRIVATE("private", "Private Node", true),
    ALPHA_1("alpha1", "Alpha 1", true),
    ALPHA_2("alpha2", "Alpha 2", true);

    private static final Map<String, Node> ID_MAP = new HashMap<>();
    private static final Map<String, Node> NAME_MAP = new HashMap<>();
    private static final Pattern PRIVATE_NODE_NAME_PATTERN = Pattern.compile("^Private Node (?<number>\\d+)$");

    static {
        for (Node node : values()) {
            ID_MAP.put(node.id, node);
            NAME_MAP.put(node.name, node);
        }
    }

    private final String id;
    private final String name;
    private final boolean isActionDumpObtainable;

    Node(String id, String name, boolean isActionDumpObtainable) {
        this.id = id;
        this.name = name;
        this.isActionDumpObtainable = isActionDumpObtainable;
    }

    public static Node fromId(String serverId) {
        return ID_MAP.get(serverId);
    }

    public static Node fromName(String serverName) {
        if(serverName.startsWith("Private Node")) return PRIVATE;

        return NAME_MAP.get(serverName);
    }

    /**
     * Gets the specific ID from a private node's display name.
     *
     * <p>The general private node ID remains {@code private}; for example,
     * {@code Private Node 14} has the specific private node ID {@code private14}.</p>
     *
     * @param serverName The node name.
     * @return The specific private node ID, or {@code null} when the name is not
     * a numbered private node.
     */
    public static @Nullable String privateNodeIdFromName(String serverName) {
        Matcher matcher = PRIVATE_NODE_NAME_PATTERN.matcher(serverName);
        if (!matcher.matches()) {
            return null;
        }

        return PRIVATE.id + matcher.group("number");
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public boolean isActionDumpObtainable() {
        return this.isActionDumpObtainable;
    }

    public boolean isMain() {
        return switch (this) {
            case NODE_1, NODE_2, NODE_3, NODE_4, NODE_5, NODE_6, NODE_7 -> true;
            default -> false;
        };
    }

}
