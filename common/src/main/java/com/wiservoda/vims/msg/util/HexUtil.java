package com.wiservoda.vims.msg.util;

import org.apache.commons.lang3.ArrayUtils;

public final class HexUtil {

    public static String bytesToHex(final byte[] bytes) {
        return bytesToHex(bytes, true);
    }

    public static String bytesToHex(final byte[] bytes, boolean withSpaces) {
        return bytesToHex(bytes, withSpaces, 0, bytes.length);
    }

    public static String bytesToHex(final byte[] bytes, boolean withSpaces, int begin, int end) {
        byte[] bs = ArrayUtils.subarray(bytes, begin, end);
        StringBuilder sb = new StringBuilder();
        for (byte b : bs) {
            String hex = Integer.toHexString((int) b & 0xFF).toUpperCase();
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
            if (withSpaces) {
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static byte hexStrToByte(String hex) {
        return (byte) Integer.parseInt(hex, 16);
    }

    public static byte[] hexToBytes(final String hex) {
        String temp = hex.replace(" ", "");
        if (temp.length() < 1) {
            return null;
        } else {
            byte[] result = new byte[temp.length() / 2];
            int j = 0;
            for(int i = 0; i < temp.length(); i += 2) {
                result[j++] = (byte)Integer.parseInt(temp.substring(i, i + 2), 16);
            }
            return result;
        }
    }

    private HexUtil() {
    }
}
