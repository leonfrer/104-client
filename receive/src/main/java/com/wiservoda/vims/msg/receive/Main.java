package com.wiservoda.vims.msg.receive;

import com.wiservoda.vims.msg.receive.client.TcpClient;

public class Main {

    public static void main(String[] args) throws Exception {
        new TcpClient("192.168.2.171", 2404).start();
//        new TcpClient("192.168.14.98", 2404).start();
    }
}
