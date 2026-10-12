package multicast;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * Giao diện Desktop Java Swing cho ứng dụng Net Send Multicast P2P.
 * Thiết kế tinh gọn, trực quan, hỗ trợ đầy đủ Unicast, Multicast Group và Broadcast (*).
 */
public class NetSendGui extends JFrame {
    private final NetSendApp app;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    // UI Components
    private JTextArea chatArea;
    private JComboBox<String> targetCombo;
    private JTextField messageField;
    private JTextField groupInputField;
    private DefaultListModel<String> groupListModel;
    private JList<String> groupList;
    private JLabel statusLabel;

    public NetSendGui(int port) throws IOException {
        super("VKU - Net Send Multicast (P2P UDP)");

        // Khởi tạo core network backend
        this.app = new NetSendApp(port);

        // Đăng ký callback khi nhận tin nhắn để cập nhật UI
        this.app.getListener().setMessageCallback((senderIp, content) -> {
            SwingUtilities.invokeLater(() -> appendIncomingMessage(senderIp, content));
        });

        // Thiết lập giao diện Swing
        initComponents();
        setupWindow();

        // Mặc định join group1 để tiện demo
        joinGroup("group1");
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // 1. TOP PANEL: Thông tin máy & Quản lý Nhóm
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setBorder(new EmptyBorder(8, 10, 0, 10));

        // Info bar
        JPanel infoBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        infoBar.setBackground(new Color(240, 244, 250));
        infoBar.setBorder(BorderFactory.createLineBorder(new Color(210, 225, 240)));

        JLabel lblIp = new JLabel("🌐 IP của máy: " + MulticastConfig.getLocalHostAddress());
        lblIp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblIp.setForeground(new Color(15, 60, 120));

        JLabel lblPort = new JLabel("🔌 Cổng UDP: " + app.getPort());
        lblPort.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPort.setForeground(new Color(15, 60, 120));

        statusLabel = new JLabel("🟢 Đang hoạt động (P2P Ready)");
        statusLabel.setForeground(new Color(30, 130, 60));

        infoBar.add(lblIp);
        infoBar.add(lblPort);
        infoBar.add(statusLabel);
        topPanel.add(infoBar, BorderLayout.NORTH);

        add(topPanel, BorderLayout.NORTH);

        // 2. CENTER PANEL: Chia 2 cột (Trái: Danh sách nhóm, Phải: Khung chat)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(220);
        splitPane.setBorder(new EmptyBorder(0, 10, 0, 10));

        // Cột trái: Quản lý nhóm (Multicast Groups)
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(new TitledBorder("Quản lý nhóm (Multicast)"));

        groupListModel = new DefaultListModel<>();
        groupListModel.addElement("* (Toàn mạng)");
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane groupScroll = new JScrollPane(groupList);
        leftPanel.add(groupScroll, BorderLayout.CENTER);

        JPanel groupActionPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        groupInputField = new JTextField();
        groupInputField.setToolTipText("Nhập tên nhóm, ví dụ: group1, it, vku");

        JPanel groupButtons = new JPanel(new GridLayout(1, 2, 4, 0));
        JButton btnJoin = new JButton("Vào nhóm");
        JButton btnLeave = new JButton("Rời nhóm");

        btnJoin.addActionListener(e -> {
            String gName = groupInputField.getText().trim();
            if (!gName.isEmpty()) {
                joinGroup(gName);
                groupInputField.setText("");
            }
        });

        btnLeave.addActionListener(e -> {
            String selected = groupList.getSelectedValue();
            if (selected != null && !selected.startsWith("*")) {
                leaveGroup(selected);
            } else {
                String gName = groupInputField.getText().trim();
                if (!gName.isEmpty()) {
                    leaveGroup(gName);
                    groupInputField.setText("");
                }
            }
        });

        groupButtons.add(btnJoin);
        groupButtons.add(btnLeave);
        groupActionPanel.add(groupInputField);
        groupActionPanel.add(groupButtons);
        leftPanel.add(groupActionPanel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);

        // Cột phải: Nhật ký tin nhắn (Chat Area)
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(new TitledBorder("Nhật ký tin nhắn nhận được & gửi đi"));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(new Color(252, 252, 252));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        rightPanel.add(chatScroll, BorderLayout.CENTER);

        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // 3. BOTTOM PANEL: Thanh nhập lệnh và gửi tin nhắn
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setBorder(new EmptyBorder(5, 10, 10, 10));

        JPanel targetPanel = new JPanel(new BorderLayout(5, 0));
        targetPanel.add(new JLabel("Đích gửi: "), BorderLayout.WEST);

        // ComboBox cho phép chọn nhanh hoặc gõ IP / group tùy ý
        targetCombo = new JComboBox<>(new String[]{"*", "group1", "127.0.0.1"});
        targetCombo.setEditable(true);
        targetCombo.setPreferredSize(new Dimension(160, 30));
        targetPanel.add(targetCombo, BorderLayout.CENTER);

        JPanel sendPanel = new JPanel(new BorderLayout(8, 0));
        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageField.setPreferredSize(new Dimension(300, 32));

        JButton btnSend = new JButton("  GỬI (Send) ✈  ");
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSend.setBackground(new Color(15, 100, 210));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFocusPainted(false);

        btnSend.addActionListener(this::handleSend);
        messageField.addActionListener(this::handleSend);

        sendPanel.add(messageField, BorderLayout.CENTER);
        sendPanel.add(btnSend, BorderLayout.EAST);

        bottomPanel.add(targetPanel, BorderLayout.WEST);
        bottomPanel.add(sendPanel, BorderLayout.CENTER);

        // Ghi chú cú pháp bên dưới
        JLabel lblHint = new JLabel("💡 Mẹo: Nhập IP để gửi riêng | Nhập tên nhóm để gửi multicast | Nhập * để gửi toàn mạng. Hoặc gõ thẳng lệnh 'net send <target> <msg>' vào ô tin nhắn.");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblHint.setForeground(Color.GRAY);
        bottomPanel.add(lblHint, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupWindow() {
        setSize(860, 600);
        setMinimumSize(new Dimension(720, 480));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                app.close();
            }
        });
    }

    private void handleSend(ActionEvent e) {
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) return;

        // Nếu người dùng gõ trực tiếp lệnh "net send ..."
        if (msg.toLowerCase().startsWith("net send ")) {
            app.handleCommand(msg);
            appendLog(String.format("[%s] [LỆNH] %s", timeFormat.format(new Date()), msg));
            messageField.setText("");
            return;
        }

        Object targetObj = targetCombo.getSelectedItem();
        String target = (targetObj != null) ? targetObj.toString().trim() : "*";
        if (target.isEmpty()) target = "*";

        // Thực hiện gửi
        app.sendMessage(target, msg);

        // Hiển thị lên khung chat của máy này
        String time = timeFormat.format(new Date());
        String targetDisplay = target.equals("*") ? "Toàn mạng (*)" : target;
        appendLog(String.format("[%s] [BẠN ĐÃ GỬI] tới '%s': %s", time, targetDisplay, msg));

        // Cập nhật lại danh sách gợi ý trong combo nếu chưa có
        boolean exists = false;
        for (int i = 0; i < targetCombo.getItemCount(); i++) {
            if (targetCombo.getItemAt(i).equalsIgnoreCase(target)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            targetCombo.addItem(target);
        }

        messageField.setText("");
        messageField.requestFocus();
    }

    private void joinGroup(String groupName) {
        String groupIp = MulticastConfig.getGroupAddress(groupName);
        if (app.getListener().joinGroup(groupIp)) {
            if (!groupListModel.contains(groupName)) {
                groupListModel.addElement(groupName);
            }
            appendLog(String.format("[%s] ➕ Đã tham gia nhóm Multicast: '%s' (IP: %s)",
                    timeFormat.format(new Date()), groupName, groupIp));
        } else {
            appendLog(String.format("[%s] ℹ️ Bạn đã ở trong nhóm '%s' từ trước.",
                    timeFormat.format(new Date()), groupName));
        }
    }

    private void leaveGroup(String groupName) {
        String groupIp = MulticastConfig.getGroupAddress(groupName);
        if (app.getListener().leaveGroup(groupIp)) {
            groupListModel.removeElement(groupName);
            appendLog(String.format("[%s] ➖ Đã rời khỏi nhóm Multicast: '%s' (IP: %s)",
                    timeFormat.format(new Date()), groupName, groupIp));
        } else {
            appendLog(String.format("[%s] ⚠️ Chưa tham gia nhóm '%s' nên không thể rời.",
                    timeFormat.format(new Date()), groupName));
        }
    }

    private void appendIncomingMessage(String senderIp, String content) {
        String time = timeFormat.format(new Date());
        appendLog(String.format("[%s] 📩 NHẬN TỪ [%s]: %s", time, senderIp, content));
    }

    private void appendLog(String text) {
        chatArea.append(text + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        // Thiết lập Look and Feel hệ thống cho giao diện đẹp và tự nhiên
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        int port = MulticastConfig.DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        final int finalPort = port;
        SwingUtilities.invokeLater(() -> {
            try {
                NetSendGui gui = new NetSendGui(finalPort);
                gui.setVisible(true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Không thể khởi động Net Send GUI: " + e.getMessage(),
                        "Lỗi Khởi Động", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
