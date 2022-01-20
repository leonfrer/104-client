package com.wiservoda;

import com.wiservoda.vims.msg.receive.entity.MessageSerialNumber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SerialNumberTest {

    @Test
    public void serialNumberSetTest() {
        MessageSerialNumber m1 = new MessageSerialNumber(4);
        Assertions.assertArrayEquals(m1.getSerialNumBytes(), new byte[] {0x08, 0x00});
        MessageSerialNumber m2 = new MessageSerialNumber(new byte[]{0x24, 0x00});
        Assertions.assertEquals(m2.getSerialNumDec(), 18);
    }
}
