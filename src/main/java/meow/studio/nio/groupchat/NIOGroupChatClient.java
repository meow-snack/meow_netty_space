package meow.studio.nio.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;

public class NIOGroupChatClient {

    private Selector selector;
    private SocketChannel socketChannel;
    private String username;


    public NIOGroupChatClient() throws IOException {
        selector = Selector.open();

        // 链接服务器
        socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 1123));
        socketChannel.configureBlocking(false);

        // 将 channel 注册到 selector
        socketChannel.register(selector, SelectionKey.OP_READ);

        username = socketChannel.getLocalAddress().toString();
        System.out.printf("客户端[" + username + "]已就绪.");
    }

    public void sendMessage(String message) {
        String fullMessage = String.format("[%s]: [%s]", username, message);
        try {
            socketChannel.write(ByteBuffer.wrap(fullMessage.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMessage() {
        try {
            int readChannels = selector.select();
            if (readChannels > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        socketChannel.read(byteBuffer);
                        String message = new String(byteBuffer.array(), StandardCharsets.UTF_8);
                        System.out.println(message.trim());
                    }
                    iterator.remove();
                }
            } else {
                System.out.println("暂无可用的读通道...");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        NIOGroupChatClient groupChatClient = new NIOGroupChatClient();

        new Thread(){
            @Override
            public void run() {
                while (true){
                    groupChatClient.readMessage();
                    try {
                        Thread.currentThread().sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String message = scanner.nextLine();
            groupChatClient.sendMessage(message);
        }
    }
}
