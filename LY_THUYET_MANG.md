# 📚 LÝ THUYẾT LẬP TRÌNH MẠNG - ÔN THI

## 1️⃣ MÔ HÌNH CLIENT-SERVER

### **Định nghĩa:**
- **Server:** Máy tính/phần mềm cung cấp dịch vụ, **chờ** client kết nối
- **Client:** Máy tính/phần mềm **yêu cầu** dịch vụ từ server

### **Kiến trúc:**
```
┌─────────┐         ┌─────────┐
│ Client  │────────►│ Server  │
│         │◄────────│         │
└─────────┘         └─────────┘
   (Nhiều)            (1 hoặc ít)
```

### **Đặc điểm:**
- ✅ **Tập trung:** Server quản lý tất cả
- ✅ **Dễ quản lý:** Dữ liệu tập trung ở server
- ✅ **Bảo mật:** Server kiểm soát truy cập
- ❌ **Bottleneck:** Server có thể quá tải
- ❌ **Single point of failure:** Server chết → tất cả chết

### **Ví dụ:**
- Web: Browser (Client) ↔ Web Server
- Email: Email Client ↔ Mail Server
- Database: Application ↔ Database Server

### **Trong code này:**
- `ChessServer.java` - Server lắng nghe port 9998
- `ServerConnection.java` - Client kết nối tới server
- Server quản lý rooms, ghép cặp players

---

## 2️⃣ MÔ HÌNH P2P (Peer-to-Peer)

### **Định nghĩa:**
- **P2P:** Các máy tính **kết nối trực tiếp** với nhau, không qua server trung gian
- Mỗi node vừa là **client** vừa là **server**

### **Kiến trúc:**
```
┌─────────┐
│ Peer A  │
└────┬────┘
     │
     │ Kết nối trực tiếp
     │
┌────▼────┐
│ Peer B  │
└─────────┘
```

### **Đặc điểm:**
- ✅ **Phân tán:** Không có server trung tâm
- ✅ **Scalable:** Thêm peer dễ dàng
- ✅ **Low latency:** Kết nối trực tiếp
- ❌ **Khó quản lý:** Không có điểm tập trung
- ❌ **Bảo mật kém:** Khó kiểm soát
- ❌ **NAT/Firewall:** Khó kết nối qua mạng

### **Ví dụ:**
- BitTorrent: Download file từ nhiều peers
- Skype (cũ): Gọi video trực tiếp
- Blockchain: Các node kết nối với nhau

### **Trong code này:**
- `DirectPeer.java` - Kết nối trực tiếp giữa 2 players
- Server chỉ làm "matchmaker" (ghép cặp)
- MOVE/CHAT đi trực tiếp, không qua server

---

## 3️⃣ TCP vs UDP

### **TCP (Transmission Control Protocol)**

#### **Đặc điểm:**
- ✅ **Connection-oriented:** Phải thiết lập kết nối trước
- ✅ **Reliable:** Đảm bảo gửi đúng, đủ, đúng thứ tự
- ✅ **Flow control:** Điều chỉnh tốc độ gửi
- ✅ **Congestion control:** Tránh quá tải mạng
- ❌ **Chậm hơn UDP:** Do overhead (ACK, retransmission)
- ❌ **Overhead lớn:** Header 20 bytes

#### **Cơ chế:**
1. **3-way handshake:**
   ```
   Client ──SYN──► Server
   Client ◄─SYN-ACK── Server
   Client ──ACK──► Server
   ```
2. **ACK (Acknowledgement):** Xác nhận nhận được
3. **Retransmission:** Gửi lại nếu mất gói
4. **Sequence number:** Đảm bảo thứ tự

#### **Khi nào dùng TCP:**
- Web browsing (HTTP)
- Email (SMTP)
- File transfer (FTP)
- **Chess game** (cần đảm bảo moves không mất)

### **UDP (User Datagram Protocol)**

#### **Đặc điểm:**
- ✅ **Connectionless:** Không cần thiết lập kết nối
- ✅ **Nhanh:** Ít overhead
- ✅ **Low latency:** Không có delay do ACK
- ❌ **Unreliable:** Không đảm bảo gửi đến
- ❌ **Không đảm bảo thứ tự:** Gói có thể đến sai thứ tự
- ❌ **Không flow control:** Có thể gửi quá nhanh

#### **Cơ chế:**
- Gửi và quên (fire and forget)
- Không có ACK, retransmission
- Header nhỏ (8 bytes)

#### **Khi nào dùng UDP:**
- Video streaming (mất 1 frame không sao)
- Voice chat (latency quan trọng hơn reliability)
- DNS queries
- Online games (real-time, mất 1 packet không sao)

### **So sánh:**

| Tiêu chí | TCP | UDP |
|----------|-----|-----|
| **Connection** | Có (3-way handshake) | Không |
| **Reliability** | ✅ Đảm bảo | ❌ Không đảm bảo |
| **Speed** | Chậm hơn | Nhanh hơn |
| **Overhead** | Lớn (20 bytes) | Nhỏ (8 bytes) |
| **Order** | Đảm bảo thứ tự | Không đảm bảo |
| **Use case** | Web, Email, File transfer | Video, Voice, Games |

### **Trong code này:**
- **Dùng TCP** (Socket, ServerSocket)
- **Lý do:** Cần đảm bảo moves không mất, đúng thứ tự
- Nếu dùng UDP → Moves có thể mất → Game lỗi!

---

## 4️⃣ BẬT SERVER TRƯỚC HAY CLIENT TRƯỚC?

### **Câu trả lời: SERVER TRƯỚC!**

### **Tại sao?**

#### **1. Server phải "lắng nghe" (listen) trước:**
```java
// Server
ServerSocket serverSocket = new ServerSocket(9998);
serverSocket.accept(); // ← Chờ client kết nối
```

- ServerSocket phải **bind** vào port trước
- Phải **listen** để chờ client
- Nếu client connect trước → **Connection refused!**

#### **2. Client "kết nối" (connect) tới server:**
```java
// Client
Socket socket = new Socket("serverIP", 9998); // ← Connect tới server
```

- Client cần server **đã sẵn sàng** để connect
- Nếu server chưa chạy → **Connection refused!**

### **Flow:**
```
1. Server start → Listen port 9998
2. Client start → Connect tới port 9998
3. Server accept → Connection established!
```

### **Ví dụ thực tế:**
- **Web:** Web server phải chạy trước → Browser mới truy cập được
- **Database:** Database server phải chạy trước → App mới kết nối được
- **Game:** Game server phải chạy trước → Players mới join được

### **Trong code này:**
```bash
# Bước 1: Bật server trước
java ChessServer
# → Server listening on port 9998

# Bước 2: Bật clients sau
java ChessApp  # Client 1
java ChessApp  # Client 2
# → Clients connect tới server
```

---

## 5️⃣ CÁC KHÁI NIỆM QUAN TRỌNG

### **Socket:**
- **Định nghĩa:** Endpoint của kết nối (IP + Port)
- **Vai trò:** Cho phép 2 chương trình giao tiếp qua mạng
- **Ví dụ:** `192.168.1.100:9998`

### **Port:**
- **Định nghĩa:** Số hiệu để phân biệt các dịch vụ trên cùng 1 máy
- **Range:** 0-65535
- **Well-known ports:** 0-1023 (HTTP: 80, HTTPS: 443, SSH: 22)
- **Dynamic ports:** 1024-65535 (dùng cho applications)

### **IP Address:**
- **Định nghĩa:** Địa chỉ định danh máy tính trên mạng
- **IPv4:** 4 số (0-255), ví dụ: `192.168.1.100`
- **IPv6:** 8 nhóm hex, ví dụ: `2001:0db8:85a3::8a2e:0370:7334`

### **ServerSocket:**
- **Định nghĩa:** Socket phía server, **lắng nghe** kết nối
- **Method:** `accept()` - Chờ và chấp nhận kết nối từ client

### **Socket (Client):**
- **Định nghĩa:** Socket phía client, **kết nối** tới server
- **Method:** `connect()` - Kết nối tới server

---

## 6️⃣ CÁC CÂU HỎI THƯỜNG GẶP

### **Q1: Tại sao cần Port?**
**A:** Một máy có thể chạy nhiều dịch vụ (Web, Email, Database...). Port giúp phân biệt dịch vụ nào.

### **Q2: TCP có thể mất gói tin không?**
**A:** Có thể mất do lỗi mạng, nhưng TCP sẽ **tự động gửi lại** (retransmission) cho đến khi nhận được ACK.

### **Q3: UDP có đảm bảo thứ tự không?**
**A:** Không. Gói tin có thể đến sai thứ tự. Ứng dụng phải tự xử lý.

### **Q4: Tại sao P2P khó qua NAT?**
**A:** NAT (Network Address Translation) che giấu IP thật. Client A không biết IP thật của Client B → Không thể connect trực tiếp.

### **Q5: Server có thể là Client không?**
**A:** Có! Ví dụ: Web server có thể là client của Database server.

### **Q6: P2P có cần server không?**
**A:** Tùy. **Pure P2P:** Không cần (BitTorrent). **Hybrid P2P:** Cần server để ghép cặp (như code này).

### **Q7: Tại sao game dùng TCP thay vì UDP?**
**A:** Game cần đảm bảo moves không mất, đúng thứ tự. UDP có thể mất moves → Game lỗi.

### **Q8: Socket và Port khác nhau gì?**
**A:** 
- **Port:** Chỉ là số (0-65535)
- **Socket:** IP + Port (ví dụ: `192.168.1.100:9998`)

### **Q9: Tại sao ServerSocket.accept() blocking?**
**A:** Vì nó **chờ** client kết nối. Nếu không có client → chờ mãi. Nên dùng thread riêng.

### **Q10: Client có thể lắng nghe không?**
**A:** Có! Trong P2P, mỗi client vừa là client vừa là server. Client mở ServerSocket để lắng nghe.

---

## 7️⃣ MÔ HÌNH TRONG CODE NÀY

### **Hybrid P2P:**
```
┌─────────┐         ┌─────────┐
│Client A │◄──P2P──►│Client B │
└────┬────┘         └────┬────┘
     │                   │
     │    TCP Socket     │
     │                   │
     └──────►┌─────┐◄─────┘
             │Server│
             └─────┘
```

### **Luồng hoạt động:**
1. **Server:** Ghép cặp, trao đổi IP/Port
2. **P2P:** Clients kết nối trực tiếp
3. **Fallback:** Nếu P2P fail → Dùng server relay

### **Protocol:**
- **TCP:** Đảm bảo moves không mất
- **Port:** Server 9998, P2P random (> 10000)

---

## 8️⃣ TÓM TẮT NHANH

### **Client-Server:**
- Server cung cấp dịch vụ
- Client yêu cầu dịch vụ
- **Bật server trước!**

### **P2P:**
- Kết nối trực tiếp giữa peers
- Không qua server trung gian
- **Khó qua NAT/Firewall**

### **TCP:**
- Connection-oriented
- Reliable, đảm bảo thứ tự
- **Chậm hơn UDP**

### **UDP:**
- Connectionless
- Fast, low latency
- **Không đảm bảo**

### **Socket:**
- IP + Port
- ServerSocket: Listen
- Socket: Connect

---

**Tạo bởi:** Auto - 2025-12-04
**Mục đích:** Ôn thi Lập trình Mạng

