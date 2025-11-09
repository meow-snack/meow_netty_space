package meow.studio.nio.channel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class NIOFileChannelUsage5Client {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("127.0.0.1", 1123);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        System.out.printf("已连接到服务器, 端口号=%d\n", socket.getPort());
        System.out.println("输入文本后后回车发送, 空行退出。");

        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            // 将文本发送给服务器, flush 立即推送
            outputStream.write((line+"\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            // 读取服务器响应
            byte[] buffer = new byte[1024];
            int read = inputStream.read(buffer);
            if (read != -1) {
                System.out.println("服务器返回: " + new String(buffer, 0, read));
            } else {
                System.out.println("服务器已关闭连接");
                break;
            }

        }
    }
}
