package com.wiservoda.vims.msg.receive.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TcpClient {

    private final String host;
    private final int port;
    private final Bootstrap bootstrap;
    private ChannelFuture channelFuture;
    private final EventLoopGroup group;

    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.bootstrap = new Bootstrap();
        this.group = new NioEventLoopGroup();
        TcpClient tcpClient = this;
        bootstrap.group(group)
                .remoteAddress(new InetSocketAddress(host, port))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, 65537, 1, 1, 0, 0, true))
                                .addLast(new ByteArrayDecoder())
                                .addLast(new ByteArrayEncoder())
                                .addLast(new TcpClientHandler(tcpClient))
                        ;
                    }
                });
    }

    public void start() {
        channelFuture = bootstrap.connect()
                .addListener((ChannelFutureListener) channelFuture -> {
                    if (channelFuture.cause() != null) {
                        log.warn("Connect server failed, retry in 3s");
                        channelFuture.channel().eventLoop().schedule(this::start, 3, TimeUnit.SECONDS);
                    } else {
                        log.info("Connect server success");
                    }
                });
    }
}
