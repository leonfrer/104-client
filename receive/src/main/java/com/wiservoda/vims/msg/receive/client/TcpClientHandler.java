package com.wiservoda.vims.msg.receive.client;

import com.wiservoda.vims.msg.receive.entity.MessageFile;
import com.wiservoda.vims.msg.receive.entity.MessageSerialNumber;
import com.wiservoda.vims.msg.receive.util.MessageUtil;
import com.wiservoda.vims.msg.util.BytesUtil;
import com.wiservoda.vims.msg.util.HexUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TcpClientHandler extends ChannelInboundHandlerAdapter {

    private final Map<Integer, MessageFile> fileMap = new ConcurrentHashMap<>();
    private final MessageSerialNumber sendMessageSerialNumber = new MessageSerialNumber(0);
    private MessageSerialNumber recentMessageSerialNumber = null;
    private MessageSerialNumber lastRepliedSerialNumber = null;
    private ScheduledFuture<?> sendSSchedule;
    private boolean casual = false;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Sending initialize...");
        sendMessage(ctx, new byte[]{0x68, 0x04, 0x07, 0x00, 0x00, 0x00});
        ctx.channel().eventLoop().schedule(() -> {
            casual = fileMap.isEmpty() && recentMessageSerialNumber.equals(lastRepliedSerialNumber);
            if (casual) {
                sendSSchedule.cancel(true);
                sendSSchedule = null;
            } else if (sendSSchedule == null) {
                sendSSchedule = ctx.channel().eventLoop().schedule(() -> {
                    if (lastRepliedSerialNumber.getSerialNumDec() > recentMessageSerialNumber.getSerialNumDec()) {
                        try {
                            sendSerialNumConfirm(ctx);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            log.error("Error during send S message.");
                        }
                    }
                }, 10, TimeUnit.SECONDS);
            }
        }, 10, TimeUnit.SECONDS);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] bytes = (byte[]) msg;
        log.info("Rec: {}", HexUtil.bytesToHex(bytes));
        if (Arrays.equals(new byte[]{0x68, 0x04, 0x43, 0x00, 0x00, 0x00}, bytes)) {
            log.debug("Heartbeat");
            sendMessage(ctx, new byte[]{0x68, 0x04, (byte) 0x83, 0x00, 0x00, 0x00});
            return;
        }
        if (Arrays.equals(new byte[]{0x68, 0x04, 0x0B, 0x00, 0x00, 0x00}, bytes)) {
            log.info("Initialize Success!");
            ctx.read();
            return;
        }
        if (bytes[1] != 0x04) {
            casual = false;
            recentMessageSerialNumber = new MessageSerialNumber(Arrays.copyOfRange(bytes, 2, 4));
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
                sendIMessage(ctx, merged);
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
                        sendIMessage(ctx, _6msgBytes);
                        log.info("File received finish: {}, base64: {}", messageFile, messageFile.getBase64());
                    }
                } else {
                    log.error("file-read message with a unknown file id, msg: {}", HexUtil.bytesToHex(bytes));
                }
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        log.info("read complete");
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("Channel inactive");
    }

    private void sendIMessage(ChannelHandlerContext ctx, byte[] bytes) throws Exception {
        MessageUtil.setSendSerialNum(bytes, sendMessageSerialNumber);
        sendMessageSerialNumber.plus(1);
        MessageUtil.setRecSerialNum(bytes, recentMessageSerialNumber.getPlus(1));
        sendMessage(ctx, bytes);
        lastRepliedSerialNumber = recentMessageSerialNumber;
    }

    private void sendSerialNumConfirm(ChannelHandlerContext ctx) throws InterruptedException {
        sendMessage(ctx, ArrayUtils.addAll(new byte[]{0x68, 0x04, 0x01, 0x00}, recentMessageSerialNumber.getPlus(1).getSerialNumBytes()));
        lastRepliedSerialNumber = recentMessageSerialNumber;
    }

    private void sendMessage(ChannelHandlerContext ctx, byte[] bytes) throws InterruptedException {
        ctx.writeAndFlush(bytes).sync();
        log.debug("Send Message: {}", HexUtil.bytesToHex(bytes));
    }
}
