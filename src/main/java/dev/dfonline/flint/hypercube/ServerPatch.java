package dev.dfonline.flint.hypercube;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public record ServerPatch(String value) implements Comparable<ServerPatch> {

    public static final String REGEX = "\\d+\\.\\d+(?:\\.\\d+)?";

    private static final Pattern PATCH_PATTERN = Pattern.compile("^" + REGEX + "(?:-[a-z0-9]+)?$");

    public ServerPatch {
        value = value.trim();

        if (!PATCH_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid server patch: " + value);
        }
    }

    public static ServerPatch parse(String value) {
        return new ServerPatch(value);
    }

    public static @Nullable ServerPatch tryParse(@Nullable String value) {
        if (value == null) {
            return null;
        }

        try {
            return parse(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public boolean isAtLeast(String minimumPatch) {
        return this.compareTo(parse(minimumPatch)) >= 0;
    }

    public ServerPatch forNode(@Nullable Node node) {
        String basePatch = this.baseValue();
        String nodePatch = node == null || node.isMain() || node == Node.PRIVATE || node == Node.EVENT
                ? basePatch
                : basePatch + "-" + node.getId();

        return nodePatch.equals(this.value) ? this : new ServerPatch(nodePatch);
    }

    public String baseValue() {
        return this.value.split("-", 2)[0];
    }

    @Override
    public int compareTo(@NotNull ServerPatch other) {
        return compareNumbers(this.baseValue().split("\\."), other.baseValue().split("\\."));
    }

    private static int compareNumbers(String[] left, String[] right) {
        int maxLength = Math.max(left.length, right.length);

        for (int index = 0; index < maxLength; index++) {
            String leftPart = index < left.length ? left[index] : "0";
            String rightPart = index < right.length ? right[index] : "0";
            int comparison = compareNumberPart(leftPart, rightPart);

            if (comparison != 0) {
                return comparison;
            }
        }

        return 0;
    }

    private static int compareNumberPart(String left, String right) {
        String normalizedLeft = trimLeadingZeroes(left);
        String normalizedRight = trimLeadingZeroes(right);
        int lengthComparison = Integer.compare(normalizedLeft.length(), normalizedRight.length());

        if (lengthComparison != 0) {
            return lengthComparison;
        }

        return normalizedLeft.compareTo(normalizedRight);
    }

    private static String trimLeadingZeroes(String number) {
        String normalized = number.replaceFirst("^0+", "");
        return normalized.isEmpty() ? "0" : normalized;
    }

}
