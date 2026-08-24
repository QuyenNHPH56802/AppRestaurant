package com.restaurant.server.network;

import com.restaurant.server.config.RestaurantProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Detects a non-loopback IPv4 address on the LAN. Used by the QR pairing payload
 * and the server dashboard.
 *
 * Preference order:
 *   1. site-local (RFC 1918) IPv4 address of the first non-loopback interface
 *   2. Any non-loopback IPv4 address
 *   3. "127.0.0.1"
 *
 * PHASE 8 will compose this with port + protocol into a JSON payload for the QR code.
 */
@Component
public class LanIpDetector {

    private static final Logger log = LoggerFactory.getLogger(LanIpDetector.class);

    private final RestaurantProperties props;
    private String cachedIp = "127.0.0.1";

    public LanIpDetector(RestaurantProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void detect() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            String fallback = null;
            while (ifaces != null && ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (!(a instanceof Inet4Address v4)) continue;
                    if (v4.isLoopbackAddress()) continue;
                    String host = v4.getHostAddress();
                    byte[] oct = v4.getAddress();
                    boolean siteLocal = (oct[0] == 10)
                            || (oct[0] == 172 && (oct[1] & 0xF0) == 16)
                            || (oct[0] == 192 && oct[1] == 168);
                    if (siteLocal) {
                        cachedIp = host;
                        log.info("Detected LAN IP: {} (iface={})", host, iface.getName());
                        return;
                    }
                    if (fallback == null) fallback = host;
                }
            }
            if (fallback != null) {
                cachedIp = fallback;
                log.info("Detected non-site-local IP: {}", fallback);
            } else {
                log.warn("No non-loopback IPv4 address found; using 127.0.0.1");
            }
        } catch (SocketException e) {
            log.warn("LAN IP detection failed: {}", e.getMessage());
        }
    }

    public String getLanIp() { return cachedIp; }

    public int getPort() {
        return Integer.parseInt(System.getProperty("server.port",
                System.getenv().getOrDefault("SERVER_PORT", "8080")));
    }

    public String getProtocol() {
        return "http";
    }
}