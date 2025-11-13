package meow.studio.netty.diytcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class NettyServer {
    public static void main(String[] args) throws Exception {
        int port = 1123;

        int cpuCoreNum = Runtime.getRuntime().availableProcessors();
        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(cpuCoreNum * 2, NioIoHandler.newFactory());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    // maxFrameLength = 16MB        : 最大帧长度 16MB
                                    // lengthFieldOffset = 8Byte    : 长度字段从帧起始处算起，偏移 8 个字节
                                    // lengthFieldLength = 4Byte    : 长度字段占 4 字节
                                    // lengthAdjustment = 4Byte     : 长度字段后面还有 4 字节额外头部(CRC)，不包含在长度值里
                                    // initialBytesToStrip = 0Byte  : 解码后保留整帧，从第 0 个字节开始交给下游 handler, 我们自己进行解包
                                    .addLast(new LengthFieldBasedFrameDecoder(16 * 1024 * 1024, 8, 4, 4, 0))
                                    .addLast(new MessageDecoder())
                                    .addLast(new MessageEncoder())
                                    .addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS))
                                    .addLast(new ServerHandler());
                        }
                    });

            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            System.out.println("[Meow Server] started on 0.0.0.0:" + port);
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
