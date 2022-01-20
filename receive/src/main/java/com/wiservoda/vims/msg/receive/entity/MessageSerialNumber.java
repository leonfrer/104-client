package com.wiservoda.vims.msg.receive.entity;

import com.wiservoda.vims.msg.util.BytesUtil;
import lombok.EqualsAndHashCode;
import lombok.Setter;

import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

@Setter
@EqualsAndHashCode
public class MessageSerialNumber {

    private AtomicInteger decNum;

    public MessageSerialNumber(byte[] serialNum) {
        int i = BytesUtil.reverseMergeBytesToDecInt(serialNum);
        decNum = new AtomicInteger(i >>> 1);
    }

    public MessageSerialNumber(int serialNum) {
        decNum = new AtomicInteger(serialNum);
    }

    public int getSerialNumDec() {
        return decNum.get();
    }

    public byte[] getSerialNumBytes() {
        return BytesUtil.numberToBytes((long) decNum.get() << 1, ByteOrder.BIG_ENDIAN, 2);
    }

    public void plus(int n) {
        this.decNum.addAndGet(n);
        if (this.decNum.get() > 32767) {
            this.decNum.set(this.decNum.get() % 32768);
        }
    }

    public MessageSerialNumber getPlus(int n) {
        return new MessageSerialNumber(decNum.addAndGet(n));
    }
}
