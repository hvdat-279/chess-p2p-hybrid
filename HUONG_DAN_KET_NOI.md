# HƯỚNG DẪN KẾT NỐI MÁY KHÁC VÀO SERVER

## 📍 THÔNG TIN SERVER CỦA BẠN

**IP Address:** `10.148.139.12`  
**Port:** `9999`

---

## 🖥️ TRÊN MÁY CHẠY SERVER (Máy của bạn)

### Bước 1: Chạy Server
```bash
cd /home/tuaw-khoiii/IdeaProjects/chess-p2p-hybrid
mvn exec:java -Dexec.mainClass="com.example.chess_project_p2p_hybrid.server.ChessServer"
```

Hoặc nếu đã build:
```bash
java -cp "target/classes:target/dependency/*" com.example.chess_project_p2p_hybrid.server.ChessServer
```

Bạn sẽ thấy:
```
Chess Server (Hybrid Hub) running on port 9999
```

**⚠️ QUAN TRỌNG:** Để terminal này mở, không tắt!

### Bước 2: Chạy Client trên máy này (tùy chọn)
Mở terminal mới:
```bash
mvn javafx:run
```

Trong màn hình đăng nhập:
- **Tên người chơi:** Nhập tên (ví dụ: "Người chơi 1")
- **Máy chủ:** `127.0.0.1` hoặc `localhost`
- **Cổng:** `9999`
- Nhấn **Kết nối**

---

## 💻 TRÊN MÁY KHÁC (Máy thứ 2)

### Bước 1: Đảm bảo cùng mạng
- Máy khác phải **cùng mạng LAN** với máy server
- Hoặc có thể ping được IP `10.148.139.12`

**Kiểm tra kết nối:**
```bash
# Trên máy khác, chạy:
ping 10.148.139.12
```

Nếu ping được → OK!  
Nếu không ping được → Kiểm tra:
- Cả 2 máy có cùng WiFi/mạng LAN không?
- Firewall có chặn không?

### Bước 2: Chạy Client
```bash
cd /path/to/chess-p2p-hybrid
mvn javafx:run
```

### Bước 3: Nhập thông tin kết nối
Trong màn hình đăng nhập:
- **Tên người chơi:** Nhập tên khác (ví dụ: "Người chơi 2")
- **Máy chủ:** `10.148.139.12` ⬅️ **QUAN TRỌNG: Nhập IP này!**
- **Cổng:** `9999`
- Nhấn **Kết nối**

---

## ✅ KIỂM TRA

Sau khi cả 2 client kết nối:
- Màn hình sẽ tự động chuyển sang bàn cờ
- Cả 2 người chơi sẽ thấy bàn cờ
- Có thể bắt đầu chơi!

---

## 🔧 XỬ LÝ LỖI

### Lỗi: "Connection refused" hoặc "Cannot connect"

**Nguyên nhân có thể:**
1. Server chưa chạy → Chạy server trước!
2. Firewall chặn → Mở port 9999:
   ```bash
   sudo ./mo-port-firewall.sh
   ```
3. IP sai → Kiểm tra lại IP bằng:
   ```bash
   ./tim-ip-va-port.sh
   ```
4. Không cùng mạng → Đảm bảo cả 2 máy cùng WiFi/LAN

### Lỗi: "Connection timeout"

**Nguyên nhân:**
- Firewall đang chặn
- Không cùng mạng
- Router chặn kết nối

**Giải pháp:**
```bash
# Trên máy server, mở firewall:
sudo ./mo-port-firewall.sh

# Kiểm tra server đang chạy:
netstat -tuln | grep 9999
# hoặc
ss -tuln | grep 9999
```

---

## 📝 TÓM TẮT NHANH

**Máy Server (10.148.139.12):**
1. Chạy Server
2. (Tùy chọn) Chạy Client với `127.0.0.1`

**Máy khác:**
1. Chạy Client
2. Nhập `10.148.139.12` vào ô "Máy chủ"
3. Port: `9999`

---

## 🎮 TEST NHANH

Nếu muốn test trên cùng 1 máy:
1. Chạy Server
2. Chạy Client 1 (nhập `127.0.0.1`)
3. Chạy Client 2 (nhập `127.0.0.1`)
4. Cả 2 sẽ tự động ghép cặp!

---

Chúc bạn chơi vui vẻ! 🎉

