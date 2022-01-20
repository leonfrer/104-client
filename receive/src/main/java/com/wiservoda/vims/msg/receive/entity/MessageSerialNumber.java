package com.wiservoda.vims.msg.receive.entity;

import com.wiservoda.vims.msg.receive.util.MessageUtil;
import com.wiservoda.vims.msg.util.BytesUtil;
import lombok.Setter;

import java.nio.ByteOrder;

@Setter
public class MessageSerialNumber {

    private byte[] serialNum;

    public MessageSerialNumber(byte[] serialNum) {
        if (serialNum.length != 2 || (serialNum[0] & 1) != 0) {
            throw new IllegalArgumentException();
        }
        this.serialNum = serialNum;
    }

    public MessageSerialNumber(int serialNum) {
        if (serialNum < 0 || serialNum > MessageUtil.MAX_SERIAL_NUM) {
            throw new IllegalArgumentException();
        }
        this.serialNum = BytesUtil.numberToBytes((long) serialNum << 1, ByteOrder.BIG_ENDIAN, 2);
    }

    public int getSerialNumDec() {
        return BytesUtil.reverseMergeBytesToDecInt(serialNum) >>> 1;
    }

    public byte[] getSerialNumBytes() {
        return serialNum;
    }
}
