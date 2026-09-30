package naturalexceptions.core.worldgen;

/** Opens the room writer's stair turning to tests in other packages. */
public final class RoomWriterAccess {

    private RoomWriterAccess() {}

    public static int stairs(char c, int rotation) {
        return RoomWriter.stairs(c, rotation);
    }

    public static int alcoveMeta(String mob) {
        return RoomWriter.alcoveMeta(mob);
    }
}
