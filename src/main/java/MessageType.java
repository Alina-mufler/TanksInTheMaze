public enum MessageType {
    DISCONNECTED_FROM_LOBBY_ERROR(-3),
    CONNECT_TO_LOBBY_ERROR(-2),
    CREATE_LOBBY_ERROR(-1),
    CREATE_LOBBY(0),
    SUCCESSFUL_CREATED_LOBBY(1),
    CONNECT_TO_LOBBY(2),
    SUCCESSFUL_CONNECTED_TO_LOBBY(3),
    DISCONNECT_FROM_LOBBY(4),
    SUCCESSFUL_DISCONNECTED_FROM_LOBBY(5);

    private int code;
    MessageType(int code){
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

