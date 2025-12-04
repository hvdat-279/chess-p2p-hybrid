#!/bin/bash

# Script mở port 9999 trên firewall (Linux)

echo "=========================================="
echo "  MỞ PORT 9999 TRÊN FIREWALL"
echo "=========================================="
echo ""

PORT=9999

# Kiểm tra quyền root
if [ "$EUID" -ne 0 ]; then 
    echo "⚠️  Cần quyền root (sudo) để mở port!"
    echo "   Chạy: sudo ./mo-port-firewall.sh"
    exit 1
fi

# UFW
if command -v ufw &> /dev/null; then
    echo "🔧 Đang cấu hình UFW..."
    ufw allow $PORT/tcp
    echo "✅ Đã mở port $PORT trong UFW"
    echo ""
fi

# Firewalld
if command -v firewall-cmd &> /dev/null; then
    if systemctl is-active --quiet firewalld; then
        echo "🔧 Đang cấu hình firewalld..."
        firewall-cmd --add-port=$PORT/tcp --permanent
        firewall-cmd --reload
        echo "✅ Đã mở port $PORT trong firewalld"
        echo ""
    fi
fi

# iptables (nếu không dùng UFW/firewalld)
if command -v iptables &> /dev/null; then
    echo "🔧 Đang cấu hình iptables..."
    iptables -I INPUT -p tcp --dport $PORT -j ACCEPT
    # Lưu rules (tùy distro)
    if command -v iptables-save &> /dev/null; then
        if [ -f /etc/redhat-release ]; then
            service iptables save 2>/dev/null || true
        elif [ -f /etc/debian_version ]; then
            iptables-save > /etc/iptables/rules.v4 2>/dev/null || true
        fi
    fi
    echo "✅ Đã mở port $PORT trong iptables"
    echo ""
fi

echo "=========================================="
echo "✅ Hoàn tất! Port $PORT đã được mở."
echo "=========================================="
echo ""
echo "Kiểm tra lại bằng: ./tim-ip-va-port.sh"

