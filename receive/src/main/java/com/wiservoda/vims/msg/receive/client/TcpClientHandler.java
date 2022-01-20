package com.wiservoda.vims.msg.receive.client;

import com.wiservoda.vims.msg.receive.entity.MessageFile;
import com.wiservoda.vims.msg.receive.entity.MessageSerialNumber;
import com.wiservoda.vims.msg.receive.util.MessageUtil;
import com.wiservoda.vims.msg.util.BytesUtil;
import com.wiservoda.vims.msg.util.HexUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TcpClientHandler extends ChannelInboundHandlerAdapter {

    private final Map<Integer, MessageFile> fileMap = new ConcurrentHashMap<>();
    private int serialNumber = 0;
    private final List<byte[]> list = new ArrayList<>();
    private MessageSerialNumber lastMessageSerialNumber = new MessageSerialNumber(0);
    private boolean replayedI = false;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Sending initialize...");
        sendMessage(ctx, new byte[]{0x68, 0x04, 0x07, 0x00, 0x00, 0x00});
    }

    private void handle(ChannelHandlerContext ctx) throws Exception {
        MessageSerialNumber messageSerialNumber = null;
        for (byte[] bytes : list) {
            if (Byte.toUnsignedInt(bytes[1]) != 0x04) {
                messageSerialNumber = new MessageSerialNumber(Arrays.copyOfRange(bytes, 2, 4));
            }
        }
        for (byte[] bytes : list) {
            if (Arrays.equals(new byte[]{0x68, 0x04, 0x43, 0x00, 0x00, 0x00}, bytes)) {
                log.debug("Heartbeat");
                sendMessage(ctx, new byte[]{0x68, 0x04, (byte) 0x83, 0x00, 0x00, 0x00});
                continue;
            }
            if (Arrays.equals(new byte[]{0x68, 0x04, 0x0B, 0x00, 0x00, 0x00}, bytes)) {
                log.info("Initialize Success!");
                ctx.read();
                continue;
            }
            if (bytes[1] != 0x04) {
                // file receive init
                if (Byte.toUnsignedInt(bytes[6]) == 0xD2 && Byte.toUnsignedInt(bytes[8]) == 0x05 && Byte.toUnsignedInt(bytes[16]) == 0x07) {
                    log.info("Received a file-upload-request");
                    // get appendix content
                    MessageFile messageFile = MessageUtil.getFileInfoByFileSendRequest(ArrayUtils.subarray(bytes, 16, bytes.length));
                    log.debug("{}", messageFile);
                    fileMap.put(messageFile.getId(), messageFile);
                    byte[] pre = ArrayUtils.subarray(bytes, 0, 16);
                    byte[] su = ArrayUtils.subarray(bytes, 17, bytes.length);
                    pre[1] = (byte) (Byte.toUnsignedInt(pre[1]) + 1);
                    byte[] merged = BytesUtil.merge(pre, new byte[]{0x08, 0x00}, su);
                    MessageUtil.setSendSerialNum(merged, serialNumber);
                    serialNumber += 1;
                    MessageUtil.setRecSerialNum(merged, messageSerialNumber.getSerialNumDec() + 1);
                    sendMessage(ctx, merged);
                    replayedI = true;
                }
                // file receive
                else if (Byte.toUnsignedInt(bytes[6]) == 0xD2 && Byte.toUnsignedInt(bytes[8]) == 0x05 && Byte.toUnsignedInt(bytes[16]) == 0x05) {
                    int fileId = BytesUtil.reverseMergeBytesToDecInt(bytes, 17, 21);
                    MessageFile messageFile = fileMap.get(fileId);
                    if (messageFile != null) {
                        byte[] fileBytes = messageFile.getBytes();
                        byte[] msgFileBytes = ArrayUtils.subarray(bytes, 26, bytes.length - 1);
                        // chack sum
                        boolean sumCorrect = BytesUtil.verifySum(msgFileBytes, Byte.toUnsignedInt(bytes[bytes.length - 1]));
                        if (!sumCorrect) {
                            log.error("Checksum error: {}", messageFile);
                        }
                        messageFile.setBytes(fileBytes == null ? msgFileBytes : ArrayUtils.addAll(fileBytes, msgFileBytes));
                        // if this file msg with an end flag
                        if (Byte.toUnsignedInt(bytes[25]) == 0x00) {
                            // send confirm message
                            byte[] _6msgBytes = ArrayUtils.subarray(bytes, 0, 26);
                            _6msgBytes[1] = 24;
                            _6msgBytes[16] = 0x06;
                            MessageUtil.setSendSerialNum(_6msgBytes, serialNumber);
                            serialNumber += 1;
                            MessageUtil.setRecSerialNum(_6msgBytes, messageSerialNumber.getSerialNumDec() + 1);
                            sendMessage(ctx, _6msgBytes);
                            replayedI = true;
                            log.info("File received finish: {}, base64: {}", messageFile, messageFile.getBase64());
                        }
                    } else {
                        log.error("file-read message with a unknown file id, msg: {}", HexUtil.bytesToHex(bytes));
                    }
                }
            }
        }
        sendSerialNumConfirm(ctx, messageSerialNumber);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] bytes = (byte[]) msg;
        log.info("Rec: {}", HexUtil.bytesToHex(bytes));
        list.add(bytes);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        log.info("read complete");
        ctx.flush();
        try {
            handle(ctx);
        } finally {
            list.clear();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("Channel inactive");
    }

    private void sendSerialNumConfirm(ChannelHandlerContext ctx, MessageSerialNumber serialNumber) throws InterruptedException {
        if (serialNumber == null) {
            return;
        }
        if (serialNumber.getSerialNumDec() - lastMessageSerialNumber.getSerialNumDec() < 8 || replayedI) {
            lastMessageSerialNumber = serialNumber;
            return;
        }
        int serialNumDec = serialNumber.getSerialNumDec();
        MessageSerialNumber sendSerialNumber = new MessageSerialNumber(serialNumDec == 0x7fff ? 0 : serialNumDec + 1);
        log.info("sending S message (serial: {})", sendSerialNumber.getSerialNumDec());
        sendMessage(ctx, ArrayUtils.addAll(new byte[]{0x68, 0x04, 0x01, 0x00}, sendSerialNumber.getSerialNumBytes()));
    }

    private void sendMessage(ChannelHandlerContext ctx, byte[] bytes) throws InterruptedException {
        ctx.writeAndFlush(bytes).sync();
        log.debug("Send Message: {}", HexUtil.bytesToHex(bytes));
    }
}
