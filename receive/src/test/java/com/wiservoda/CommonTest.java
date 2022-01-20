package com.wiservoda;

import com.wiservoda.vims.msg.util.BytesUtil;
import com.wiservoda.vims.msg.util.HexUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;
import java.util.Arrays;

public class CommonTest {

    @Test
    public void test() {
        byte[] a = new byte[] {0x68, 0x04,(byte) 0xC3, 0x00, 0x00, 0x00, 0x00};
        byte[] b = new byte[] {0x68, 0x04,(byte) 0xC3, 0x00, 0x00, 0x00, 0x00};
        System.out.println(Arrays.equals(a, b));
    }

    @Test
    public void sumTest() {
        byte[] bytes = HexUtil.hexToBytes("00 00 00 00 00 00 00 00 00 00 00 00 00 00 30 41 00 00 40 40 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 C0 40 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 40 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 80 3F 00 00 00 40 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 80 3F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00".replace(" ", ""));
        long l = 0;
        for (byte aByte : bytes) {
            l += Byte.toUnsignedInt(aByte);
        }
        byte[] b = BytesUtil.numberToBytes(l, ByteOrder.LITTLE_ENDIAN);
        Assertions.assertEquals(Byte.toUnsignedInt(b[b.length -1]), 0x6f);
    }
}
