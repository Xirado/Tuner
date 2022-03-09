package at.xirado.tuner.interaction;

public enum CommandType {

    SLASH_COMMAND(1),
    USER_CONTEXT_MENU_COMMAND(2),
    MESSAGE_CONTEXT_MENU_COMMAND(3);

    private final int id;

    CommandType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static CommandType fromId(int id) {
        for (var type : values()) {
            if (type.getId() == id)
                return type;
        }
        return null;
    }
}
