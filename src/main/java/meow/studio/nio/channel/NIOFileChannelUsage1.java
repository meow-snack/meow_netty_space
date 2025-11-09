package meow.studio.nio.channel;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class NIOFileChannelUsage1 {
    public static void main(String[] args) throws Exception {
        String str = "hello, meow~";

        // 创建一个输出流
        FileOutputStream fileOutputStream = new FileOutputStream("meow.md");

        // 获取输出流对应的 channel
        FileChannel fineChannel = fileOutputStream.getChannel();

        // 创建一个缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        // 写入缓冲区
        byteBuffer.put(str.getBytes(StandardCharsets.UTF_8));

        // buffer 写->读
        byteBuffer.flip();

        // 缓冲区的数据写入到 channel
        fineChannel.write(byteBuffer);

        // 关闭输出流
        fileOutputStream.close();
    }
}
