package meow.studio.nio.channel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class NIOFileChannelUsage3 {
    public static void main(String[] args) throws Exception {
        FileInputStream fileInputStream = new FileInputStream("meow.md");
        FileChannel fileInputChannel = fileInputStream.getChannel();
        FileOutputStream fileOutputStream = new FileOutputStream("meow2.md");
        FileChannel fileOutputChannel = fileOutputStream.getChannel();

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        while (true) {
            byteBuffer.clear();
            int read = fileInputChannel.read(byteBuffer);
            System.out.println("read: " + read);
            if (read == -1) {
                System.out.println("文件拷贝完毕");
                break;
            }

            // 读->写
            byteBuffer.flip();
            fileOutputChannel.write(byteBuffer);
        }

        fileInputChannel.close();
        fileOutputChannel.close();
        fileInputStream.close();
        fileOutputStream.close();
    }
}
