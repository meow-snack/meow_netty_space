package meow.studio.nio.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class NIOGroupChatServer {
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    private static final int PORT = 1123;

    public NIOGroupChatServer() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();

            // 绑定端口
            serverSocketChannel.socket().bind(new InetSocketAddress(PORT));

            // 非阻塞模式
            serverSocketChannel.configureBlocking(false);

            // 将 channel 注册到 selector
            // 关心的事件类型为: OP_ACCEPT - 服务器通道已经有新的连接等着被接受
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        try {
            while (true) {
                // selector.select() 会挂起当前线程
                // 直到至少有一个已注册通道的感兴趣事件就绪（或被唤醒）
                // 返回值是“有多少个 SelectionKey 处于就绪状态”
                int count = selector.select();

                if (count > 0) {
                    // 有事件要处理, 遍历 selectedKey 集合
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();

                        if (key.isAcceptable()) { // 判断事件是否处于 accept ready 的状态
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            socketChannel.configureBlocking(false);
                            // 将 socketChannel 注册到 selector
                            // 关心的事件类型为: OP_READ - 通道可读
                            socketChannel.register(selector, SelectionKey.OP_READ);
                            System.out.println(socketChannel.getRemoteAddress() + " 上线 ");
                        }

                        if (key.isReadable()) {
                            readData(key);
                        }
                        iterator.remove();
                    }
                } else {
                    System.out.println("服务器等待接入...");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readData(SelectionKey key) {
        SocketChannel socketChannel = null;
        try {
            // 获取当初注册到该 SelectionKey 上的那个 SelectableChannel 实例
            socketChannel = (SocketChannel) key.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            int count = socketChannel.read(byteBuffer);

            if (count > 0) {
                String message = new String(byteBuffer.array(), StandardCharsets.UTF_8);
                System.out.println(message);
                sendInfoToOthers(message, socketChannel);
            }

        } catch (IOException e1) {
            try {
                System.out.println(socketChannel.getRemoteAddress() + "离线了");
                // 取消注册
                key.cancel();
                // 关闭通道
                socketChannel.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void sendInfoToOthers(String message, SocketChannel fromChannel) throws IOException {
        System.out.println("服务器转发消息...");
        for (SelectionKey key : selector.keys()) {
            Channel channel = key.channel();
            if (channel instanceof SocketChannel && (!channel.equals(fromChannel))) {
                SocketChannel toChannel = (SocketChannel) channel;

                ByteBuffer byteBuffer = ByteBuffer.wrap(message.getBytes());
                toChannel.write(byteBuffer);
            }
        }
    }

    public static void main(String[] args) {
        NIOGroupChatServer server = new NIOGroupChatServer();
        server.listen();
    }
}
