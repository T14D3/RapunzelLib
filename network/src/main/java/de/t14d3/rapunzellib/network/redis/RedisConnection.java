package de.t14d3.rapunzellib.network.redis;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class RedisConnection implements AutoCloseable {
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    private RedisConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    static RedisConnection connect(RedisPubSubConfig config, boolean subscribeConnection) throws IOException {
        Socket socket = config.ssl() ? SSLSocketFactory.getDefault().createSocket() : new Socket();
        socket.connect(new InetSocketAddress(config.host(), config.port()), config.connectTimeoutMillis());

        int timeout = subscribeConnection ? 0 : config.socketTimeoutMillis();
        socket.setSoTimeout(Math.max(timeout, 0));

        if (socket instanceof SSLSocket sslSocket) {
            sslSocket.startHandshake();
        }

        RedisConnection conn = new RedisConnection(socket);
        conn.authenticate(config);
        conn.setClientName(config);
        return conn;
    }

    boolean isOpen() {
        return !socket.isClosed() && socket.isConnected();
    }

    void subscribe(String channel) throws IOException {
        sendCommand("SUBSCRIBE", channel);
    }

    void publish(String channel, String payload) throws IOException {
        sendCommand("PUBLISH", channel, payload);
        readReply(); // integer reply
    }

    private void authenticate(RedisPubSubConfig config) throws IOException {
        String password = config.password();
        if (password == null) return;

        String username = config.username();
        if (username != null) {
            sendCommand("AUTH", username, password);
        } else {
            sendCommand("AUTH", password);
        }
        readReply(); // "OK"
    }

    private void setClientName(RedisPubSubConfig config) throws IOException {
        String name = config.clientName();
        if (name == null) return;
        sendCommand("CLIENT", "SETNAME", name);
        readReply(); // "OK"
    }

    void sendCommand(String... args) throws IOException {
        writeAscii('*');
        writeAscii(Integer.toString(args.length));
        writeCrlf();

        for (String arg : args) {
            byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
            writeAscii('$');
            writeAscii(Integer.toString(bytes.length));
            writeCrlf();
            out.write(bytes);
            writeCrlf();
        }
        out.flush();
    }

    Object readReply() throws IOException {
        int prefix = in.read();
        if (prefix == -1) throw new EOFException("Redis connection closed");

        return switch (prefix) {
            case '+' -> readLine();
            case '-' -> throw new IOException("Redis error: " + readLine());
            case ':' -> Long.parseLong(readLine());
            case '$' -> readBulkString();
            case '*' -> readArray();
            default -> throw new IOException("Unexpected RESP prefix: " + (char) prefix);
        };
    }

    private String readBulkString() throws IOException {
        int len = Integer.parseInt(readLine());
        if (len < 0) return null;

        byte[] bytes = in.readNBytes(len);
        if (bytes.length != len) throw new EOFException("Unexpected EOF in bulk string");
        expectCrlf();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private List<Object> readArray() throws IOException {
        int count = Integer.parseInt(readLine());
        if (count < 0) return null;

        ArrayList<Object> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(readReply());
        }
        return out;
    }

    private String readLine() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            int b = in.read();
            if (b == -1) throw new EOFException("Unexpected EOF while reading line");
            if (b == '\r') {
                int next = in.read();
                if (next == -1) throw new EOFException("Unexpected EOF while reading line ending");
                if (next != '\n') throw new IOException("Invalid RESP line ending");
                break;
            }
            baos.write(b);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private void expectCrlf() throws IOException {
        int r = in.read();
        int n = in.read();
        if (r != '\r' || n != '\n') throw new IOException("Invalid RESP bulk string termination");
    }

    private void writeAscii(char c) throws IOException {
        out.write((byte) c);
    }

    private void writeAscii(String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeCrlf() throws IOException {
        out.write('\r');
        out.write('\n');
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}

