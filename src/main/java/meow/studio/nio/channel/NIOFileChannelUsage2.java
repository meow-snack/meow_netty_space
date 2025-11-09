package meow.studio.nio.channel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class NIOFileChannelUsage2 {
    public static void main(String[] args) throws Exception {

        // 创建一个输入流
        File file = new File("meow.md");
        FileInputStream fileInputStream = new FileInputStream(file);

        // 获取输入流对应的 channel
        FileChannel fineChannel = fileInputStream.getChannel();

        // 创建一个缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) file.length());

        // channel 读取数据并放到到缓冲区
        fineChannel.read(byteBuffer);

        // console 打印结果
        System.out.println(new String(byteBuffer.array(), StandardCharsets.UTF_8));

        // 关闭输入流
        fileInputStream.close();
    }
}
