package meow.studio.bio;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BIOServer {
    public static void main(String[] args) throws Exception {
        // 1. 创建一个线程池
        ExecutorService newCachedThreadPool = Executors.newCachedThreadPool();
        // 2. 创建 ServerSocket, 端口: 1123
        ServerSocket serverSocket = new ServerSocket(1123);
        System.out.printf("[main] 服务器已启动, 端口: %d\n", serverSocket.getLocalPort());

        while (true) {
            System.out.printf("[main] 线程ID=%d, 线程名称=%s\n", Thread.currentThread().threadId(), Thread.currentThread().getName());

            // 3. 监听端口, 等待客户端链接
            System.out.println("[main] 等待客户端链接...");
            final Socket socket = serverSocket.accept();
            System.out.println("[main] 获取到客户端链接");

            // 4. 创建一个线程, 与客户端通信
            newCachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    handler(socket);
                }
            });
        }
    }

    public static void handler(Socket socket) {
        try {
            System.out.printf("[handler] 线程ID=%d, 线程名称=%s\n", Thread.currentThread().threadId(), Thread.currentThread().getName());

            // 1. 通过 socket 获取输入流
            byte[] bytes = new byte[1024];
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            while (true) {
                System.out.printf("[handler] 线程ID=%d, 线程名称=%s\n", Thread.currentThread().threadId(), Thread.currentThread().getName());
                System.out.println("[handler] 读取客户端数据...");

                int read = inputStream.read(bytes);
                if (read != -1) {
                    String message = new String(bytes, 0, read);
                    System.out.println("[handler] 客户端输入: "+ message);
                    outputStream.write(("服务器已收到消息: " + message).getBytes(StandardCharsets.UTF_8));
                } else {
                    System.out.println("[handler] 客户端已关闭连接");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("[handler] 关闭和客户端的链接");
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
