package com.wiservoda.vims.msg.receive.util;

import com.wiservoda.vims.msg.receive.entity.MessageFile;
import com.wiservoda.vims.msg.receive.entity.MessageSerialNumber;
import com.wiservoda.vims.msg.util.BytesUtil;
import org.apache.commons.lang3.ArrayUtils;

public final class MessageUtil {

    public static final int MAX_SERIAL_NUM = 0x7FFF;

    public static void setSendSerialNum(byte[] bytes, MessageSerialNumber serialNumber) {
        byte[] serialNumBytes = serialNumber.getSerialNumBytes();
        bytes[2] = serialNumBytes[0];
        bytes[3] = serialNumBytes[1];
    }

    public static void setRecSerialNum(byte[] bytes, MessageSerialNumber serialNumber) {
        byte[] serialNumBytes = serialNumber.getSerialNumBytes();
        bytes[4] = serialNumBytes[0];
        bytes[5] = serialNumBytes[1];
    }

    public static MessageFile getFileInfoByFileSendRequest(byte[] appendix) {
        int filenameLen = BytesUtil.reverseMergeBytesToDecInt(appendix, 5, 6);
        byte[] filenameBytes = ArrayUtils.subarray(appendix, 6, 6 + filenameLen);
        ArrayUtils.reverse(filenameBytes);
        return MessageFile.builder()
                .id(BytesUtil.reverseMergeBytesToDecInt(appendix, 1, 5))
                .filename(new String(filenameBytes))
                .size((long) BytesUtil.reverseMergeBytesToDecInt(appendix, 6 + filenameLen, 10 + filenameLen))
                .testPointId(ArrayUtils.subarray(appendix, 10 + filenameLen, 74 + filenameLen))
                .build();
    }

    private MessageUtil() {
    }
}
