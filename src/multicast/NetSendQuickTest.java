package multicast;

import java.io.IOException;

/**
 * Kiểm thử tự động tính năng gửi nhận Unicast, Multicast Group và Broadcast (*).
 */
public class NetSendQuickTest {
    public static void main(String[] args) {
        System.out.println("=== BẮT ĐẦU KIỂM THỬ NET SEND MULTICAST ===");
        try {
            int testPort = 8899; // Cổng riêng cho test để tránh xung đột
            NetSendApp app = new NetSendApp(testPort);

            // Chờ luồng listener sẵn sàng
            Thread.sleep(300);

            // 1. Test gửi Unicast (IP localhost)
            System.out.println("\n[TEST 1] Thử nghiệm: net send 127.0.0.1 Chào bạn (Unicast)");
            app.handleCommand("net send 127.0.0.1 Chào bạn (Unicast)");
            Thread.sleep(500);

            // 2. Test gửi Multicast Group (Phải tham gia nhóm trước)
            System.out.println("\n[TEST 2] Thử nghiệm: join group1 và net send group1");
            app.handleCommand("join group1");
            Thread.sleep(200);
            app.handleCommand("net send group1 Thông báo họp nhóm (Group Multicast)");
            Thread.sleep(500);

            // 3. Test gửi Broadcast (*)
            System.out.println("\n[TEST 3] Thử nghiệm: net send * Thông báo toàn mạng (*)");
            app.handleCommand("net send * Thông báo toàn mạng (*)");
            Thread.sleep(500);

            System.out.println("\n=== HOÀN TẤT KIỂM THỬ THÀNH CÔNG ===");
            app.close();
            System.exit(0);
        } catch (Exception e) {
            System.err.println("❌ Lỗi kiểm thử: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
