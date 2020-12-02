import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Server {

    private static Server server;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    private ByteBuffer buffer;
    private Properties properties;

    private HashMap<String, LinkedList<SocketChannel>> lobbies;

    public void init() throws Exception {
        if (server == null) throw new Exception("Сервер уже был создан");
        server = new Server();
    }

    private Server() throws Exception {

        properties = new Properties();

        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();

        serverSocketChannel.bind(new InetSocketAddress(properties.getProperty("host"), Integer.parseInt(properties.getProperty("port"))));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        buffer = ByteBuffer.allocate(Integer.parseInt(properties.getProperty("bufferSize")));

        lobbies = new HashMap<>();
        server.start();
    }

    private void start() throws Exception {
        selector.select();
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iter = selectedKeys.iterator();
        while (iter.hasNext()) {

            SelectionKey key = iter.next();

            if (key.isAcceptable()) {
                registerPlayer(selector, serverSocketChannel);
            }

            if (key.isWritable()) {
                readData(buffer, key);
            }

            if (key.isReadable()) {
                writeData(buffer, key);
            }
            iter.remove();
        }
    }

    private void writeData(ByteBuffer buffer, SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();

        client.write(buffer);

    }

    private void readData(ByteBuffer buffer, SelectionKey key) throws IOException {

        buffer.clear();

        SocketChannel client = (SocketChannel) key.channel();
        int read = client.read(buffer);
        buffer.limit(read);
        //буфер -
        int messageType = buffer.get();
        if (messageType == MessageType.CONNECT_TO_LOBBY.getCode()) {
            connectToLobby(key, buffer);
        }
        if (messageType == MessageType.CREATE_LOBBY.getCode()) {
            createLobby(key);
        }
        if (messageType == MessageType.DISCONNECT_FROM_LOBBY.getCode()) {
            disconnectFromLobby(key, buffer);
        }


    }

    private void disconnectFromLobby(SelectionKey key, ByteBuffer buffer) throws IOException {
        try {
            byte[] playerToken = new byte[12];
            buffer.get(playerToken);
            String token = new String(playerToken);

            boolean success = lobbies.get(token).remove((SocketChannel) key.channel());
            if (success) {
                writeData(ByteBuffer.wrap(BigInteger.valueOf(MessageType.SUCCESSFUL_DISCONNECTED_FROM_LOBBY.getCode()).toByteArray()), key);
            } else {
                writeData(ByteBuffer.wrap(BigInteger.valueOf(MessageType.DISCONNECTED_FROM_LOBBY_ERROR.getCode()).toByteArray()), key);
            }
        } catch (Exception e) {
            writeData(ByteBuffer.wrap(BigInteger.valueOf(MessageType.DISCONNECTED_FROM_LOBBY_ERROR.getCode()).toByteArray()), key);
        }
    }

    private void registerPlayer(Selector selector, ServerSocketChannel serverSocketChannel) throws IOException {
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    private void connectToLobby(SelectionKey key, ByteBuffer buffer) throws IOException {
        try {
            SocketChannel client = (SocketChannel) key.channel();

            byte[] tokenChars = new byte[12];
            buffer.get(tokenChars);
            String token = new String(tokenChars);

            if (lobbies.containsKey(token) && lobbies.get(token).size() < 4) {
                lobbies.get(token).add(client);
                writeData(ByteBuffer.wrap(BigInteger.valueOf(MessageType.SUCCESSFUL_CONNECTED_TO_LOBBY.getCode()).toByteArray()), key);
            } else {
                writeData(ByteBuffer.wrap(BigInteger.valueOf(MessageType.CONNECT_TO_LOBBY_ERROR.getCode()).toByteArray()), key);
            }

            buffer.clear();
        } catch (Exception e) {
            writeData(ByteBuffer.wrap(BigInteger.valueOf(MessageType.CONNECT_TO_LOBBY_ERROR.getCode()).toByteArray()), key);
        }
    }


    private void createLobby(SelectionKey key) throws IOException {
        try {

            String passSymbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            Random rnd = new Random();
            StringBuilder stringResult = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                stringResult.append(passSymbols.charAt(rnd.nextInt(passSymbols.length())));
            }
            byte[] token = stringResult.toString().getBytes();
            ByteBuffer send = ByteBuffer.allocate(token.length + 1);
            send.put((byte) MessageType.SUCCESSFUL_CREATED_LOBBY.getCode());
            send.put(token);
            writeData(send, key);
        } catch (Exception e) {
            writeData(ByteBuffer.wrap(BigInteger.valueOf(MessageType.CREATE_LOBBY_ERROR.getCode()).toByteArray()), key);
        }
    }

    public static void main(String[] args) {
        try {
            server.init();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

