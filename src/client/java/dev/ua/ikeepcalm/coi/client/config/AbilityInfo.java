package dev.ua.ikeepcalm.coi.client.config;

public record AbilityInfo(String abilityId, String localizedName, String englishName, String category, boolean hasLeftClick) {

    private static final String LEFT_CLICK_MARKER = "#left_click";
    public static final String ACTION_EXECUTE = "execute";
    public static final String ACTION_LEFT_CLICK = "left_click";

    public static String formatStored(String abilityId, String displayName, String action) {
        String storedId = ACTION_LEFT_CLICK.equals(action) ? abilityId + LEFT_CLICK_MARKER : abilityId;
        return storedId + " - " + displayName;
    }

    /**
     * Returns the base ability ID without display-name or client-side action suffixes.
     */
    public static String extractId(String stored) {
        if (stored == null) return null;
        String id = stored.contains(" - ") ? stored.split(" - ")[0] : stored;
        return id.endsWith(LEFT_CLICK_MARKER) ? id.substring(0, id.length() - LEFT_CLICK_MARKER.length()) : id;
    }

    public static String extractAction(String stored) {
        if (stored == null) return ACTION_EXECUTE;
        String id = stored.contains(" - ") ? stored.split(" - ")[0] : stored;
        return id.endsWith(LEFT_CLICK_MARKER) ? ACTION_LEFT_CLICK : ACTION_EXECUTE;
    }

    /**
     * Returns the display-name suffix from the stored "id - localizedName" format.
     */
    public static String extractDisplayName(String stored) {
        if (stored == null) return null;
        if (stored.contains(" - ")) {
            String[] parts = stored.split(" - ", 2);
            return parts.length > 1 ? parts[1] : stored;
        }
        return stored;
    }
}
