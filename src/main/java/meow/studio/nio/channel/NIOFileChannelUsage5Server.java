package meow.studio.nio.channel;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class NIOFileChannelUsage5Server {
    public static void main(String[] args) throws Exception {
        /*
         * NIO 支持 Buffer[] 完成读写操作
         * Scattering: 将数据写入 buffer 时, 支持 buffer 数组
         * Gathering: 从 buffer 读取数据时, 支持 buffer 数组
         */

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(1123);

        // 绑定端口到 socket 并启动
        serverSocketChannel.socket().bind(inetSocketAddress);

        // 创建 buffer 数组
        ByteBuffer[] byteBuffers = new ByteBuffer[2];
        byteBuffers[0] = ByteBuffer.allocate(5);
        byteBuffers[1] = ByteBuffer.allocate(3);

        // 等待客户端链接
        SocketChannel socketChannel = serverSocketChannel.accept();

        // 读够 8 个字节后再返回
        int messageLength = 8;

        while (true) {
            long byteRead = 0;

            while (byteRead < messageLength) {
                long l = socketChannel.read(byteBuffers);
                byteRead += l;
                System.out.println("byteRead = " + byteRead);

                Arrays.stream(byteBuffers).map(
                        buffer -> "position = " + buffer.position() + ", limit = " + buffer.limit()
                ).forEach(System.out::println);
            }

            Arrays.asList(byteBuffers).forEach(ByteBuffer::flip);

            long byteWrite = 0;
            while (byteWrite < messageLength) {
                long l = socketChannel.write(byteBuffers);
                byteWrite += l;
            }

            Arrays.asList(byteBuffers).forEach(ByteBuffer::clear);
            System.out.println("byteRead = " + byteRead + ", byteWrite = " + byteWrite + ", messageLength = " + messageLength);
        }
    }
}
