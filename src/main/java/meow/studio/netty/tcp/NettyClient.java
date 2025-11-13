package meow.studio.netty.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {
    public static void main(String[] args) throws Exception{
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            // 客户端使用 Bootstrap 而不是 ServerBootstrap

            Bootstrap bootstrap = new Bootstrap();

            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            System.out.printf("客户端 SocketChannel Hashcode=%s\n", socketChannel.hashCode());
                            socketChannel.pipeline().addLast(new NettyClientHandler());
                        }
                    });
            System.out.println("客户端已就绪.");

            // 链接服务器端
            ChannelFuture channelFuture = null;
            try {
                channelFuture = bootstrap.connect("127.0.0.1", 1123).sync();
                System.out.println("客户端链接服务器端成功.");
            } catch (Exception e) {
                System.out.println("客户端链接服务器端失败: " + e.getMessage());
                throw e;
            }

            channelFuture.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}