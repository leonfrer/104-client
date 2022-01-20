package com.wiservoda.vims.msg.util;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteOrder;
import java.util.Arrays;

public final class BytesUtil {

    /**
     * 返回的byte数组不固定长度
     */
    public static byte[] numberToBytes(long l, ByteOrder order) {
        byte[] bytes = new byte[8];
        bytes[0] = (byte) (l >>> 56);
        bytes[1] = (byte) (l >>> 48);
        bytes[2] = (byte) (l >>> 40);
        bytes[3] = (byte) (l >>> 32);
        bytes[4] = (byte) (l >>> 24);
        bytes[5] = (byte) (l >>> 16);
        bytes[6] = (byte) (l >>> 8);
        bytes[7] = (byte) (l);
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != 0) {
                bytes = ArrayUtils.subarray(bytes, i, bytes.length);
                break;
            }
        }
        if (ByteOrder.BIG_ENDIAN.equals(order)) {
            ArrayUtils.reverse(bytes);
        }
        return bytes;
    }

    /**
     * 返回的byte数组固定长度
     */
    public static byte[] numberToBytes(long l, ByteOrder order, int bytesLen) {
        if (l == 0) {
            return new byte[bytesLen];
        }
        byte[] bytes = numberToBytes(l, ByteOrder.LITTLE_ENDIAN);
        if (bytes.length > bytesLen) {
            throw new IllegalArgumentException("the bytes length you want is too short!");
        }
        byte[] data = ArrayUtils.addAll(new byte[bytesLen - bytes.length], bytes);
        if (ByteOrder.BIG_ENDIAN.equals(order)) {
            ArrayUtils.reverse(data);
        }
        return data;
    }

    public static long bytesToLong(final byte[] bytes) {
        if (bytes.length > 8) {
            throw new IllegalArgumentException("byte array too long to parse long type number");
        }
        byte[] data = ArrayUtils.addAll(new byte[8 - bytes.length], bytes);
        return (((long)data[0] << 56) +
                ((long)(data[1] & 255) << 48) +
                ((long)(data[2] & 255) << 40) +
                ((long)(data[3] & 255) << 32) +
                ((long)(data[4] & 255) << 24) +
                ((data[5] & 255) << 16) +
                ((data[6] & 255) <<  8) +
                ((data[7] & 255)));
    }

    /**
     * hex bytes: 3D 64 00 00 : 25661
     *
     * @param bytes bytes.length <= 4
     */
    public static int reverseMergeBytesToDecInt(byte[] bytes) {
        if (bytes.length > 4) {
            throw new IllegalArgumentException("bytes too long");
        }
        return (int) reverseMergeBytesToDecLong(bytes);
    }

    public static long reverseMergeBytesToDecLong(byte[] bytes) {
        if (bytes.length > 8) {
            throw new IllegalArgumentException("bytes too long");
        }
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result += Byte.toUnsignedInt(bytes[i]) << (i * 8);
        }
        return result;
    }

    public static int reverseMergeBytesToDecInt(byte[] bytes, int from, int to) {
        byte[] copy = Arrays.copyOfRange(bytes, from, to);
        return reverseMergeBytesToDecInt(copy);
    }

    public static Long reversedBcd2dec(final byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ArrayUtils.reverse(bytes);
        String hex = HexUtil.bytesToHex(bytes, false);
        return Long.parseLong(hex);
    }

    public static Long reversedBcd2dec(final byte[] bytes, int from, int to) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        byte[] subarray = ArrayUtils.subarray(bytes, from, to);
        ArrayUtils.reverse(subarray);
        String hex = HexUtil.bytesToHex(subarray, false);
        return Long.parseLong(hex);
    }

    public static byte[] merge(byte[]... bytesArr) {
        if (bytesArr.length == 0) {
            return null;
        }
        if (bytesArr.length == 1) {
            return bytesArr[0];
        }
        byte[] bytes = bytesArr[0];
        for (int i = 1; i < bytesArr.length; i++) {
            bytes = ArrayUtils.addAll(bytes, bytesArr[i]);
        }
        return bytes;
    }

    public static boolean verifySum(byte[] bytes, int sum) {
        if (bytes == null) {
            return false;
        }
        long l = 0;
        for (byte b : bytes) {
            l += b;
        }
        return sum == Byte.toUnsignedInt(numberToBytes(l, ByteOrder.BIG_ENDIAN)[0]);
    }
}
