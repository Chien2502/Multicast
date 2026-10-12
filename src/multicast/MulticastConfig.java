package multicast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình mạng và tiện ích cho chương trình Net Send Multicast.
 */
public class MulticastConfig {
    static {
        // Ưu tiên IPv4 stack trên Windows để Multicast hoạt động ổn định
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    // Cổng UDP dùng chung cho cả Unicast và Multicast
    public static final int DEFAULT_PORT = 8888;

    // Địa chỉ Multicast đại diện cho "tất cả máy trên mạng (*)"
    public static final String ALL_HOSTS_ADDRESS = "239.255.255.255";

    // Bảng ánh xạ group có sẵn (người dùng có thể gõ tên nhóm bất kỳ)
    private static final Map<String, String> PREDEFINED_GROUPS = new HashMap<>();

    static {
        PREDEFINED_GROUPS.put("group1", "239.1.1.1");
        PREDEFINED_GROUPS.put("group2", "239.1.1.2");
        PREDEFINED_GROUPS.put("group3", "239.1.1.3");
        PREDEFINED_GROUPS.put("it", "239.1.1.10");
        PREDEFINED_GROUPS.put("vku", "239.1.1.20");
    }

    /**
     * Lấy địa chỉ Multicast IP tương ứng với tên group.
     * Nếu tên group chưa có sẵn, tự động sinh IP hợp lệ trong dải 239.1.x.y.
     */
    public static String getGroupAddress(String groupName) {
        String normalized = groupName.toLowerCase().trim();
        if (PREDEFINED_GROUPS.containsKey(normalized)) {
            return PREDEFINED_GROUPS.get(normalized);
        }

        // Tự động sinh địa chỉ multicast an toàn (Class D: 239.1.x.y)
        int hash = Math.abs(normalized.hashCode());
        int octet3 = (hash % 200) + 1;
        int octet4 = ((hash / 200) % 250) + 1;
        return String.format("239.1.%d.%d", octet3, octet4);
    }

    /**
     * Tìm NetworkInterface phù hợp nhất có IPv4 hợp lệ và hỗ trợ Multicast.
     */
    public static NetworkInterface getPreferredNetworkInterface() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface loopback = null;

            for (NetworkInterface ni : Collections.list(interfaces)) {
                if (!ni.isUp() || !ni.supportsMulticast()) continue;

                if (ni.isLoopback()) {
                    if (loopback == null) loopback = ni;
                    continue;
                }

                String name = (ni.getName() + " " + ni.getDisplayName()).toLowerCase();
                if (name.contains("bluetooth") || name.contains("virtual") || name.contains("wsl") || name.contains("hyper-v")) {
                    continue;
                }

                // Kiểm tra xem interface có địa chỉ IPv4 hợp lệ (không phải link-local 169.254.x.x)
                boolean hasValidIpv4 = false;
                for (java.net.InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress addr = ia.getAddress();
                    if (addr instanceof java.net.Inet4Address && !addr.isLinkLocalAddress()) {
                        hasValidIpv4 = true;
                        break;
                    }
                }

                if (hasValidIpv4) {
                    return ni;
                }
            }

            return loopback != null ? loopback : NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
        } catch (SocketException e) {
            return null;
        }
    }

    /**
     * Lấy địa chỉ IP cục bộ của máy hiện tại.
     */
    public static String getLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
