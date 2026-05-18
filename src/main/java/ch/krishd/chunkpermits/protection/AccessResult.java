package ch.krishd.chunkpermits.protection;

public record AccessResult(boolean allowed, String reason) {
    public static AccessResult allow() {
        return new AccessResult(true, "");
    }

    public static AccessResult deny(String reason) {
        return new AccessResult(false, reason);
    }
}