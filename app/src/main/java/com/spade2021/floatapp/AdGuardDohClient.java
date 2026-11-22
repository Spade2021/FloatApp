
package com.spade2021.floatapp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class AdGuardDohClient {

    private static final String DOH_URL =
            "https://dns.adguard-dns.com/dns-query";

    private AdGuardDohClient() {}

    public static String resolveIpv4(String hostname) throws Exception {
        byte[] query = buildAQuery(hostname);

        HttpURLConnection connection =
                (HttpURLConnection) new URL(DOH_URL).openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setRequestProperty(
                    "Content-Type", "application/dns-message");
            connection.setRequestProperty(
                    "Accept", "application/dns-message");
            connection.setFixedLengthStreamingMode(query.length);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(query);
            }

            int status = connection.getResponseCode();

            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException(
                        "AdGuard DoH HTTP " + status);
            }

            byte[] response;

            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream buffer =
                         new ByteArrayOutputStream()) {

                byte[] chunk = new byte[1024];
                int count;

                while ((count = input.read(chunk)) != -1) {
                    buffer.write(chunk, 0, count);
                }

                response = buffer.toByteArray();
            }

            return firstIpv4Answer(response);

        } finally {
            connection.disconnect();
        }
    }

    private static byte[] buildAQuery(String hostname)
            throws Exception {

        String name = hostname.endsWith(".")
                ? hostname.substring(0, hostname.length() - 1)
                : hostname;

        ByteArrayOutputStream packet =
                new ByteArrayOutputStream();

        packet.write(new byte[] {
                0x41, 0x7A, 0x01, 0x00,
                0x00, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
        });

        for (String label : name.split("\\.")) {
            byte[] bytes =
                    label.getBytes(StandardCharsets.US_ASCII);

            if (bytes.length == 0 || bytes.length > 63) {
                throw new IllegalArgumentException(
                        "Invalid hostname");
            }

            packet.write(bytes.length);
            packet.write(bytes);
        }

        packet.write(0);
        packet.write(0);
        packet.write(1);
        packet.write(0);
        packet.write(1);

        return packet.toByteArray();
    }

    private static String firstIpv4Answer(byte[] message)
            throws Exception {

        if (message == null || message.length < 12) {
            throw new IllegalStateException(
                    "Invalid DNS response");
        }

        int flags = u16(message, 2);

        if ((flags & 0x8000) == 0) {
            throw new IllegalStateException(
                    "Not a DNS response");
        }

        int rcode = flags & 0x000F;

        if (rcode != 0) {
            throw new IllegalStateException(
                    "DNS response code: " + rcode);
        }

        int questions = u16(message, 4);
        int answers = u16(message, 6);
        int offset = 12;

        for (int i = 0; i < questions; i++) {
            offset = skipName(message, offset);
            offset += 4;
            requireBounds(message, offset);
        }

        for (int i = 0; i < answers; i++) {
            offset = skipName(message, offset);
            requireBounds(message, offset + 10);

            int type = u16(message, offset);
            int dnsClass = u16(message, offset + 2);
            int dataLength = u16(message, offset + 8);

            offset += 10;
            requireBounds(message, offset + dataLength);

            if (type == 1 && dnsClass == 1
                    && dataLength == 4) {

                return (message[offset] & 0xFF) + "."
                        + (message[offset + 1] & 0xFF) + "."
                        + (message[offset + 2] & 0xFF) + "."
                        + (message[offset + 3] & 0xFF);
            }

            offset += dataLength;
        }

        throw new IllegalStateException(
                "No IPv4 answer found");
    }

    private static int skipName(byte[] message, int offset)
            throws Exception {

        while (true) {
            requireBounds(message, offset);

            int length = message[offset] & 0xFF;

            if ((length & 0xC0) == 0xC0) {
                requireBounds(message, offset + 2);
                return offset + 2;
            }

            offset++;

            if (length == 0) {
                return offset;
            }

            if ((length & 0xC0) != 0 || length > 63) {
                throw new IllegalStateException(
                        "Invalid DNS name");
            }

            requireBounds(message, offset + length);
            offset += length;
        }
    }

    private static int u16(byte[] data, int offset)
            throws Exception {

        requireBounds(data, offset + 2);

        return ((data[offset] & 0xFF) << 8)
                | (data[offset + 1] & 0xFF);
    }

    private static void requireBounds(byte[] data, int end)
            throws Exception {

        if (end < 0 || end > data.length) {
            throw new IllegalStateException(
                    "Truncated DNS response");
        }
    }
              }

