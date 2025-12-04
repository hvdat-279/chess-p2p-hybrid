#!/bin/bash

# Script tìm IP address và kiểm tra port trên Linux

echo "=========================================="
echo "  TÌM IP ADDRESS VÀ KIỂM TRA PORT"
echo "=========================================="
echo ""

# Tìm IP address chính
echo "📡 IP ADDRESS CỦA MÁY NÀY:"
echo "----------------------------------------"

# Thử nhiều cách để tìm IP
IP1=$(hostname -I 2>/dev/null | awk '{print $1}')
IP2=$(ip -4 addr show | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -1)
IP3=$(ifconfig 2>/dev/null | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1' | head -1)

if [ ! -z "$IP1" ]; then
    echo "✅ IP chính: $IP1"
elif [ ! -z "$IP2" ]; then
    echo "✅ IP chính: $IP2"
elif [ ! -z "$IP3" ]; then
    echo "✅ IP chính: $IP3"
else
    echo "❌ Không tìm thấy IP. Kiểm tra kết nối mạng!"
fi

echo ""
echo "📋 TẤT CẢ IP ADDRESS:"
echo "----------------------------------------"
ip -4 addr show | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | while read ip; do
    echo "  - $ip"
done

echo ""
echo "🔌 PORT SERVER:"
echo "----------------------------------------"
echo "  Port: 9999"
echo ""

# Kiểm tra port có đang được sử dụng không
echo "🔍 KIỂM TRA PORT 9999:"
echo "----------------------------------------"
if command -v netstat &> /dev/null; then
    if netstat -tuln | grep -q ':9999'; then
        echo "⚠️  Port 9999 đang được sử dụng!"
        netstat -tuln | grep ':9999'
    else
        echo "✅ Port 9999 đang trống (sẵn sàng cho server)"
    fi
elif command -v ss &> /dev/null; then
    if ss -tuln | grep -q ':9999'; then
        echo "⚠️  Port 9999 đang được sử dụng!"
        ss -tuln | grep ':9999'
    else
        echo "✅ Port 9999 đang trống (sẵn sàng cho server)"
    fi
else
    echo "ℹ️  Không thể kiểm tra (cần cài netstat hoặc ss)"
fi

echo ""
echo "🔥 KIỂM TRA FIREWALL:"
echo "----------------------------------------"

# Kiểm tra UFW
if command -v ufw &> /dev/null; then
    UFW_STATUS=$(ufw status | head -1)
    echo "UFW Status: $UFW_STATUS"
    if echo "$UFW_STATUS" | grep -q "active"; then
        if ufw status | grep -q "9999"; then
            echo "✅ Port 9999 đã được mở trong UFW"
        else
            echo "⚠️  Port 9999 CHƯA được mở trong UFW"
            echo "   Chạy lệnh: sudo ufw allow 9999/tcp"
        fi
    else
        echo "ℹ️  UFW đang tắt (không cần mở port)"
    fi
fi

# Kiểm tra firewalld
if command -v firewall-cmd &> /dev/null; then
    if systemctl is-active --quiet firewalld; then
        echo "Firewalld: Đang chạy"
        if firewall-cmd --list-ports 2>/dev/null | grep -q "9999"; then
            echo "✅ Port 9999 đã được mở trong firewalld"
        else
            echo "⚠️  Port 9999 CHƯA được mở trong firewalld"
            echo "   Chạy lệnh: sudo firewall-cmd --add-port=9999/tcp --permanent"
            echo "             sudo firewall-cmd --reload"
        fi
    else
        echo "Firewalld: Không chạy"
    fi
fi

echo ""
echo "=========================================="
echo "📝 THÔNG TIN ĐỂ MÁY KHÁC KẾT NỐI:"
echo "=========================================="
if [ ! -z "$IP1" ]; then
    echo "  IP: $IP1"
elif [ ! -z "$IP2" ]; then
    echo "  IP: $IP2"
elif [ ! -z "$IP3" ]; then
    echo "  IP: $IP3"
fi
echo "  Port: 9999"
echo ""
echo "👉 Trên máy khác, nhập vào ô 'Máy chủ':"
if [ ! -z "$IP1" ]; then
    echo "     $IP1"
elif [ ! -z "$IP2" ]; then
    echo "     $IP2"
elif [ ! -z "$IP3" ]; then
    echo "     $IP3"
fi
echo ""

