# NIO

1. Non-blocking IO, 同步非阻塞。 
2. 三大核心部分: Channel（通道）、Buffer（缓冲区）、Selector（选择器）

## NIO VS BIO

1. BIO 以流的方式处理数据，而 NIO 以块的方式处理数据，块 I/O 的效率比流 I/O 高很多。 
2. BIO 是阻塞的，NIO 则是非阻塞的。 
3. BIO 基于字节流和字符流进行操作，而 NIO 基于 Channel（通道）和 Buffer（缓冲区）进行操作，数据总是从通道读取到缓冲区中，或者从缓冲区写入到通道中。Selector（选择器）用于监听多个通道的事件（比如：连接请求，数据到达等），因此使用单个线程就可以监听多个客户端通道。
