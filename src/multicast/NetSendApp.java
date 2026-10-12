package multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Ứng dụng Net Send Multicast P2P (Mô hình Peer-to-Peer tinh gọn).
 * Mỗi máy vừa là người gửi (Sender) vừa là người lắng nghe (Listener).
 * 
 * Hỗ trợ cú pháp:
 *   net send {IP | group | *} message
 */
public class NetSendApp {
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");

    private final int port;
    private final MessageListener listener;
    private final MulticastSocket sendSocket;

    public NetSendApp(int port) throws IOException {
        this.port = port;
        this.sendSocket = new MulticastSocket();
        this.sendSocket.setBroadcast(true); // Bật chế độ gửi Broadcast thuần UDP
        NetworkInterface netIf = MulticastConfig.getPreferredNetworkInterface();
        if (netIf != null) {
            try {
                this.sendSocket.setNetworkInterface(netIf);
                this.sendSocket.setTimeToLive(32);
            } catch (Exception ignored) {}
        }

        this.listener = new MessageListener(port);

        // Khởi chạy luồng lắng nghe tin nhắn đến
        Thread listenerThread = new Thread(listener, "NetSend-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public MessageListener getListener() {
        return listener;
    }

    public int getPort() {
        return port;
    }

    /**
     * Gửi tin nhắn đến địa chỉ IP đích (Unicast, Multicast hoặc Broadcast)
     */
    public void sendMessage(String target, String message) {
        try {
            InetAddress destAddress;
            String headerInfo;

            if ("*".equals(target)) {
                // 1. Gửi Broadcast thuần đến toàn bộ các máy trong mạng (*)
                destAddress = MulticastConfig.getBroadcastAddress();
                headerInfo = "[BROADCAST (*)] " + message;
            } else if (IPV4_PATTERN.matcher(target).matches() || "localhost".equalsIgnoreCase(target)) {
                // 2. Gửi Unicast đến 1 máy tính đích cụ thể
                destAddress = InetAddress.getByName(target);
                headerInfo = "[GỬI RIÊNG] " + message;
            } else {
                // 3. Gửi Multicast đến một nhóm máy (group)
                String groupIp = MulticastConfig.getGroupAddress(target);
                destAddress = InetAddress.getByName(groupIp);
                headerInfo = "[NHÓM: " + target + "] " + message;
            }

            byte[] data = headerInfo.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, destAddress, port);
            sendSocket.send(packet);

            System.out.printf("✔️ Đã gửi thành công tới '%s' (%s:%d)%n", target, destAddress.getHostAddress(), port);
        } catch (IOException e) {
            System.err.println("❌ Gửi tin thất bại: " + e.getMessage());
        }
    }

    /**
     * Phân tích và thực thi lệnh từ người dùng
     */
    public void handleCommand(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return;

        // Cú pháp chuẩn theo đề bài: net send {IP | group | *} message
        if (trimmed.toLowerCase().startsWith("net send ")) {
            String[] parts = trimmed.split("\\s+", 4);
            if (parts.length < 4) {
                System.out.println("⚠️ Cú pháp chưa đúng! Hãy dùng: net send {IP | group | *} <nội dung>");
                return;
            }
            String target = parts[2];
            String message = parts[3];
            sendMessage(target, message);
            return;
        }

        // Lệnh tham gia nhóm: join <group>
        if (trimmed.toLowerCase().startsWith("join ")) {
            String groupName = trimmed.substring(5).trim();
            String groupIp = MulticastConfig.getGroupAddress(groupName);
            if (listener.joinGroup(groupIp)) {
                System.out.printf("✔️ Đã tham gia nhóm '%s' (IP: %s)%n", groupName, groupIp);
            } else {
                System.out.printf("ℹ️ Bạn đã ở trong nhóm '%s' rồi.%n", groupName);
            }
            return;
        }

        // Lệnh rời khỏi nhóm: leave <group>
        if (trimmed.toLowerCase().startsWith("leave ")) {
            String groupName = trimmed.substring(6).trim();
            String groupIp = MulticastConfig.getGroupAddress(groupName);
            if (listener.leaveGroup(groupIp)) {
                System.out.printf("✔️ Đã rời khỏi nhóm '%s' (IP: %s)%n", groupName, groupIp);
            } else {
                System.out.printf("ℹ️ Bạn chưa từng tham gia nhóm '%s'.%n", groupName);
            }
            return;
        }

        // Xem danh sách nhóm đã tham gia
        if ("groups".equalsIgnoreCase(trimmed)) {
            System.out.println("📋 Danh sách IP Multicast đang lắng nghe:");
            for (String g : listener.getJoinedGroups()) {
                System.out.println("   - " + g);
            }
            return;
        }

        // Xem hướng dẫn trợ giúp
        if ("help".equalsIgnoreCase(trimmed)) {
            printHelp();
            return;
        }

        System.out.println("❓ Lệnh không xác định. Gõ 'help' để xem hướng dẫn lệnh.");
    }

    private static void printHelp() {
        System.out.println("======================================================================");
        System.out.println("                  HƯỚNG DẪN SỬ DỤNG LỆNH NET SEND                     ");
        System.out.println("======================================================================");
        System.out.println(" 1. Gửi tin nhắn theo cú pháp đề bài:");
        System.out.println("    - Đến 1 máy cụ thể (Unicast):  net send <IP> <message>");
        System.out.println("      VD: net send 192.168.1.15 Xin chao ban!");
        System.out.println("    - Đến một nhóm máy (Multicast): net send <group> <message>");
        System.out.println("      VD: net send group1 Hop nhom luc 9h nhe");
        System.out.println("    - Đến toàn bộ mạng (*):         net send * <message> (Broadcast UDP)");
        System.out.println("      VD: net send * Thong bao toan mang");
        System.out.println(" 2. Quản lý nhóm (để nhận tin nhắn của nhóm đó):");
        System.out.println("    - join <group>  : Tham gia vào nhóm (VD: join group1)");
        System.out.println("    - leave <group> : Rời khỏi nhóm (VD: leave group1)");
        System.out.println("    - groups        : Xem các nhóm đang tham gia");
        System.out.println(" 3. Lệnh hệ thống:");
        System.out.println("    - help          : Hiển thị lại hướng dẫn");
        System.out.println("    - exit / quit   : Thoát chương trình");
        System.out.println("======================================================================");
    }

    public void close() {
        if (listener != null) listener.stop();
        if (sendSocket != null && !sendSocket.isClosed()) sendSocket.close();
    }

    public static void main(String[] args) {
        int port = MulticastConfig.DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Cổng không hợp lệ, sử dụng cổng mặc định: " + port);
            }
        }

        try {
            NetSendApp app = new NetSendApp(port);

            System.out.println("======================================================================");
            System.out.println("             CHƯƠNG TRÌNH NET SEND MULTICAST (P2P PEER)              ");
            System.out.println("======================================================================");
            System.out.printf("  Máy này (IP):  %s%n", MulticastConfig.getLocalHostAddress());
            System.out.printf("  Cổng UDP:      %d%n", port);
            System.out.printf("  Chế độ (*):    Broadcast thuần (%s)%n", MulticastConfig.BROADCAST_ADDRESS);
            System.out.println("======================================================================");
            printHelp();

            // Mặc định tự join vào group1 để người dùng tiện test ngay
            app.listener.joinGroup(MulticastConfig.getGroupAddress("group1"));
            System.out.println("💡 Mẹo: Máy đã tự động tham gia nhóm 'group1' để bạn dễ dàng thử nghiệm!");

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) break;
                String line = scanner.nextLine();
                if ("exit".equalsIgnoreCase(line.trim()) || "quit".equalsIgnoreCase(line.trim())) {
                    System.out.println("Đang tắt ứng dụng...");
                    app.close();
                    break;
                }
                app.handleCommand(line);
            }

            scanner.close();
        } catch (IOException e) {
            System.err.println("❌ Khởi động ứng dụng thất bại: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
