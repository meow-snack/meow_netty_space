package meow.studio.nio.channel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class NIOFileChannelUsage4 {
    public static void main(String[] args) throws Exception {
        FileInputStream fileInputStream = new FileInputStream("meow.md");
        FileOutputStream fileOutputStream = new FileOutputStream("meow3.md");

        FileChannel fileInputChannel = fileInputStream.getChannel();
        FileChannel fileOutputChannel = fileOutputStream.getChannel();

        fileOutputChannel.transferFrom(fileInputChannel, 0, fileInputChannel.size());

        fileInputChannel.close();
        fileOutputChannel.close();
        fileInputStream.close();
        fileOutputStream.close();
    }
}
