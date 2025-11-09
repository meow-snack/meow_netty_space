package meow.studio.nio.buffer;

import java.nio.IntBuffer;

public class NIOBufferUsage {
    public static void main(String[] args) {
        IntBuffer intBuffer = IntBuffer.allocate(5);

        // 1. 写入 Buffer
        for (int i = 0; i < intBuffer.capacity(); i++) {
            intBuffer.put(i * i);
        }

        // 2. 读取 Buffer
        // [!] 读写切换
        intBuffer.flip();
        while (intBuffer.hasRemaining()) {
            System.out.println(intBuffer.get());
        }
    }
}
