package multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Luồng lắng nghe tin nhắn UDP Unicast và Multicast.
 * Tự động join vào nhóm mặc định '*' (toàn mạng) và các nhóm người dùng chọn.
 */
public class MessageListener implements Runnable {
    private final int port;
    private final MulticastSocket socket;
    private final NetworkInterface netIf;
    private final Set<String> joinedGroups = new HashSet<>();
    private volatile boolean running = true;
    private MessageCallback callback;

    public void setMessageCallback(MessageCallback callback) {
        this.callback = callback;
    }

    public MessageListener(int port) throws IOException {
        this.port = port;
        this.netIf = MulticastConfig.getPreferredNetworkInterface();

        // Khởi tạo MulticastSocket và cho phép chia sẻ cổng (SO_REUSEADDR)
        this.socket = new MulticastSocket(port);
        this.socket.setReuseAddress(true);
        if (this.netIf != null) {
            try {
                this.socket.setNetworkInterface(this.netIf);
            } catch (Exception ignored) {}
        }

        // Mặc định join vào nhóm nhận tin nhắn toàn mạng (*)
        joinGroup(MulticastConfig.ALL_HOSTS_ADDRESS);
    }

    /**
     * Tham gia vào một nhóm Multicast theo địa chỉ IP
     */
    public synchronized boolean joinGroup(String groupIp) {
        if (joinedGroups.contains(groupIp)) {
            return false;
        }
        try {
            SocketAddress groupAddress = new InetSocketAddress(InetAddress.getByName(groupIp), port);
            socket.joinGroup(groupAddress, netIf);
            joinedGroups.add(groupIp);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Lỗi khi tham gia nhóm " + groupIp + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Rời khỏi một nhóm Multicast
     */
    public synchronized boolean leaveGroup(String groupIp) {
        if (!joinedGroups.contains(groupIp)) {
            return false;
        }
        try {
            SocketAddress groupAddress = new InetSocketAddress(InetAddress.getByName(groupIp), port);
            socket.leaveGroup(groupAddress, netIf);
            joinedGroups.remove(groupIp);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Lỗi khi rời nhóm " + groupIp + ": " + e.getMessage());
            return false;
        }
    }

    public synchronized Set<String> getJoinedGroups() {
        return new HashSet<>(joinedGroups);
    }

    @Override
    public void run() {
        byte[] buffer = new byte[4096];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String senderIp = packet.getAddress().getHostAddress();
                String content = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                // Hiển thị tin nhắn nhận được cùng với địa chỉ IP máy gửi
                System.out.println();
                System.out.println("📩 [TIN NHẮN ĐẾN] từ IP: " + senderIp);
                System.out.println("   Nội dung: " + content);
                System.out.print("> "); // Trả lại dấu nhắc lệnh cho người dùng
                System.out.flush();

                if (callback != null) {
                    try {
                        callback.onMessageReceived(senderIp, content);
                    } catch (Exception e) {
                        System.err.println("⚠️ Lỗi callback GUI: " + e.getMessage());
                    }
                }

            } catch (IOException e) {
                if (!running) {
                    break; // Đã gọi stop()
                }
                System.err.println("⚠️ Lỗi đọc gói tin: " + e.getMessage());
            }
        }
    }

    /**
     * Dừng lắng nghe và đóng socket
     */
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
