# HƯỚNG DẪN CHƠI CỜ VUA TRÊN 2 MÁY TÍNH

## 📋 YÊU CẦU

- 2 máy tính kết nối cùng mạng (LAN) hoặc Internet
- Java đã được cài đặt trên cả 2 máy
- Ứng dụng đã được build/compile

---

## 🚀 CÁCH THỰC HIỆN

### **Bước 1: Tìm IP của máy chạy Server**

Trên máy tính sẽ chạy Server, mở terminal và chạy:

**Trên Linux (CÁCH DỄ NHẤT - Dùng script):**
```bash
cd /home/tuaw-khoiii/IdeaProjects/chess-p2p-hybrid
./tim-ip-va-port.sh
```

Script này sẽ tự động:
- ✅ Tìm IP address của máy
- ✅ Kiểm tra port 9999 có đang dùng không
- ✅ Kiểm tra firewall
- ✅ Hiển thị thông tin để máy khác kết nối

**Hoặc dùng lệnh thủ công:**
```bash
# Cách 1 (đơn giản nhất)
hostname -I | awk '{print $1}'

# Cách 2 (chi tiết hơn)
ip -4 addr show | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -1

# Cách 3 (nếu có ifconfig)
ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1'
```

**Trên Windows:**
```bash
ipconfig
```

Tìm dòng có **IPv4 Address**, ví dụ: `192.168.1.100` hoặc `192.168.0.5`

**Lưu ý:** 
- Nếu 2 máy cùng mạng LAN → dùng IP local (192.168.x.x hoặc 10.x.x.x)
- Nếu 2 máy khác mạng → cần IP public và cấu hình port forwarding/router
- **Port server:** `9999` (cố định)

---

### **Bước 2: Chạy Server trên máy 1**

Trên máy tính có IP vừa tìm được (ví dụ: `192.168.1.100`):

```bash
cd /home/tuaw-khoiii/IdeaProjects/chess-p2p-hybrid
java -cp "target/classes:target/dependency/*" com.example.chess_project_p2p_hybrid.server.ChessServer
```

Hoặc nếu dùng Maven:
```bash
mvn exec:java -Dexec.mainClass="com.example.chess_project_p2p_hybrid.server.ChessServer"
```

Bạn sẽ thấy thông báo:
```
Chess Server (Hybrid Hub) running on port 9999
```

**⚠️ QUAN TRỌNG:** Để terminal này mở, không tắt!

---

### **Bước 3: Chạy Client trên máy 1**

Trên **cùng máy** với Server, mở terminal mới:

```bash
cd /home/tuaw-khoiii/IdeaProjects/chess-p2p-hybrid
java -cp "target/classes:target/dependency/*" com.example.chess_project_p2p_hybrid.ChessApp
```

Hoặc:
```bash
mvn javafx:run
```

Trong màn hình đăng nhập:
- **Tên người chơi:** Nhập tên bất kỳ (ví dụ: "Người chơi 1")
- **Máy chủ:** Nhập `127.0.0.1` hoặc `localhost` (vì đang ở cùng máy với server)
- **Cổng:** `9999`
- Nhấn **Kết nối**

---

### **Bước 4: Chạy Client trên máy 2**

Trên máy tính thứ 2, mở terminal:

```bash
cd /path/to/chess-p2p-hybrid
java -cp "target/classes:target/dependency/*" com.example.chess_project_p2p_hybrid.ChessApp
```

Hoặc:
```bash
mvn javafx:run
```

Trong màn hình đăng nhập:
- **Tên người chơi:** Nhập tên khác (ví dụ: "Người chơi 2")
- **Máy chủ:** Nhập **IP của máy 1** (ví dụ: `192.168.1.100`)
- **Cổng:** `9999`
- Nhấn **Kết nối**

---

## ✅ KIỂM TRA KẾT NỐI

Sau khi cả 2 client kết nối thành công:
- Màn hình sẽ tự động chuyển sang bàn cờ
- Cả 2 người chơi sẽ thấy bàn cờ và có thể bắt đầu chơi
- Nếu có lỗi, kiểm tra:
  1. Server đã chạy chưa?
  2. IP nhập đúng chưa?
  3. Firewall có chặn port 9999 không?
  4. Cả 2 máy có cùng mạng không?

---

## 🔥 XỬ LÝ LỖI FIREWALL

Nếu không kết nối được, có thể do firewall chặn port 9999:

**Trên Linux (máy chạy Server) - CÁCH DỄ NHẤT:**
```bash
cd /home/tuaw-khoiii/IdeaProjects/chess-p2p-hybrid
sudo ./mo-port-firewall.sh
```

Script này sẽ tự động mở port 9999 cho tất cả firewall (UFW, firewalld, iptables).

**Hoặc mở thủ công:**

**UFW (Ubuntu/Debian):**
```bash
sudo ufw allow 9999/tcp
sudo ufw reload
```

**Firewalld (CentOS/RHEL/Fedora):**
```bash
sudo firewall-cmd --add-port=9999/tcp --permanent
sudo firewall-cmd --reload
```

**iptables (nếu không dùng UFW/firewalld):**
```bash
sudo iptables -I INPUT -p tcp --dport 9999 -j ACCEPT
```

**Kiểm tra firewall đã mở chưa:**
```bash
./tim-ip-va-port.sh
```

**Trên Windows:**
1. Mở Windows Defender Firewall
2. Advanced Settings → Inbound Rules → New Rule
3. Chọn Port → TCP → 9999 → Allow

---

## 🌐 CHƠI QUA INTERNET (Khác mạng)

Nếu 2 máy khác mạng (không cùng LAN):

1. **Máy chạy Server** cần có IP public (tìm bằng cách truy cập: https://whatismyipaddress.com/)
2. **Cấu hình Router:**
   - Vào router admin (thường là 192.168.1.1)
   - Port Forwarding: Forward port 9999 tới IP local của máy server
3. **Client** nhập IP public của máy server

**⚠️ Lưu ý:** Cần đảm bảo an toàn, chỉ mở port khi cần thiết!

---

## 📝 TÓM TẮT NHANH (LINUX)

### **Trên máy chạy Server (Máy 1):**

```bash
# 1. Tìm IP và kiểm tra port
./tim-ip-va-port.sh

# 2. Mở firewall (nếu cần)
sudo ./mo-port-firewall.sh

# 3. Chạy Server
mvn exec:java -Dexec.mainClass="com.example.chess_project_p2p_hybrid.server.ChessServer"
# (Để terminal này mở!)

# 4. Mở terminal mới, chạy Client
mvn javafx:run
# Nhập: Tên người chơi, Máy chủ = 127.0.0.1, Port = 9999
```

### **Trên máy khác (Máy 2):**

```bash
# 1. Chạy Client
mvn javafx:run
# Nhập: Tên người chơi, Máy chủ = [IP của máy 1], Port = 9999
```

### **Kết quả:**
1. **Máy 1:** Chạy Server → Chạy Client (nhập `127.0.0.1`)
2. **Máy 2:** Chạy Client (nhập IP của máy 1)
3. Cả 2 kết nối → Tự động ghép cặp → Bắt đầu chơi!

---

## 🎮 CHƠI THỬ NHANH (Cùng 1 máy)

Nếu muốn test nhanh trên cùng 1 máy:

1. Chạy Server
2. Chạy Client lần 1 (nhập `127.0.0.1`)
3. Chạy Client lần 2 (nhập `127.0.0.1`)
4. Cả 2 sẽ tự động ghép cặp và chơi!

---

Chúc bạn chơi vui vẻ! 🎉


