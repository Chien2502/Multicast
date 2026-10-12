package multicast;

/**
 * Interface callback nhận thông báo khi có tin nhắn đến (phục vụ cập nhật GUI / Logger).
 */
@FunctionalInterface
public interface MessageCallback {
    void onMessageReceived(String senderIp, String content);
}
