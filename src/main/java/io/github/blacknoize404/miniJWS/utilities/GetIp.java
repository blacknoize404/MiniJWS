package io.github.blacknoize404.miniJWS.utilities;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class GetIp {

    public static String getIp() {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 12345);
            return socket.getLocalAddress().getHostAddress();
        } catch (UnknownHostException | SocketException e) {
            throw new RuntimeException(e);
        }
    }
}
