# Chương trình Net Send Multicast (P2P UDP)

Dự án bài tập lập trình mạng mô phỏng lệnh `net send` sử dụng giao thức UDP Multicast & Unicast trên nền tảng Java thuần.

---

## 📌 1. Yêu cầu chức năng đã đáp ứng

- **Thực hiện lệnh**: `net send {IP | group | *} message`
  - **`IP` (Unicast)**: Gửi tin nhắn trực tiếp đến 1 máy đích cụ thể theo địa chỉ IP.
  - **`group` (Multicast)**: Gửi tin nhắn đến tất cả các máy đã tham gia (join) vào nhóm đó (ví dụ `group1`, `it`, ... theo dải IP `239.1.x.y`).
  - **`*` (Broadcast UDP thuần)**: Gửi tin nhắn Broadcast đến toàn bộ mạng LAN qua địa chỉ Subnet Broadcast / `255.255.255.255`.
- **Hiển thị tại máy nhận**: Tự động hiển thị tin nhắn kèm theo địa chỉ IP của máy gửi:
  `📩 [TIN NHẮN ĐẾN] từ IP: <IP_GỬI>`

---

## 🏗️ 2. Kiến trúc & Đặc điểm kỹ thuật

- **Mô hình**: Peer-to-Peer (P2P). Mỗi node vừa là người gửi (Sender) vừa là người lắng nghe (Listener) chạy song song trên các Thread độc lập.
- **Không cần Server trung gian**:
  - Gửi cá nhân: Sử dụng UDP Unicast trực tiếp đến IP máy nhận.
  - Gửi nhóm máy: Sử dụng UDP Multicast (Class D: `239.1.x.y`) với cơ chế `joinGroup` / `leaveGroup`.
  - Gửi toàn mạng (`*`): Sử dụng UDP Broadcast thuần (`setBroadcast(true)` tới Subnet Broadcast Address / `255.255.255.255`).

---

## 📁 3. Cấu trúc mã nguồn

```
src/
├── module-info.java
└── multicast/
    ├── MulticastConfig.java    # Cấu hình IP Multicast, Port (8888), chọn Card mạng IPv4
    ├── MessageCallback.java    # Interface callback truyền tin nhắn đến UI
    ├── MessageListener.java    # Luồng lắng nghe MulticastSocket (UDP receive)
    ├── NetSendApp.java         # Core P2P & Console CLI net send
    ├── NetSendGui.java         # Giao diện Desktop Java Swing (Trực quan, dễ demo)
    └── NetSendQuickTest.java   # Chương trình kiểm thử tự động
```

---

## 🚀 4. Hướng dẫn chạy chương trình

### Cách 1: Chạy Giao diện Desktop Swing (Khuyên dùng)
- Nhấp đúp vào file `run_gui.bat`.
- Có thể nhấp đúp mở 2 hoặc nhiều cửa sổ ứng dụng để nhắn tin qua lại trực tiếp trên màn hình!

### Cách 2: Chạy giao diện dòng lệnh Console CLI
- Nhấp đúp vào file `run.bat` (hoặc mở Terminal gõ `run.bat`).

### Cách 3: Chạy bằng dòng lệnh Java
```bash
# Biên dịch mã nguồn
javac -encoding UTF-8 -d bin src/module-info.java src/multicast/*.java

# Khởi chạy Desktop Swing GUI
java -cp bin multicast.NetSendGui

# Hoặc khởi chạy Console CLI
java -cp bin multicast.NetSendApp
```

---

## 💻 5. Bảng lệnh thao tác trong chương trình

| Lệnh | Ý nghĩa | Ví dụ |
|---|---|---|
| `net send <IP> <tin_nhắn>` | Gửi riêng tới 1 máy | `net send 192.168.1.15 Xin chào bạn` |
| `net send <group> <tin_nhắn>` | Gửi tới nhóm máy | `net send group1 Họp nhóm đồ án nhé` |
| `net send * <tin_nhắn>` | Gửi toàn bộ mạng | `net send * Thông báo toàn mạng` |
| `join <group>` | Tham gia một nhóm | `join group1` |
| `leave <group>` | Rời khỏi nhóm | `leave group1` |
| `groups` | Xem các nhóm đang tham gia | `groups` |
| `help` | Xem trợ giúp cú pháp | `help` |
| `exit` | Thoát chương trình | `exit` |
