package meow.studio.netty.diytcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class NettyClient {
    public static void main(String[] args) throws Exception {
        connect();
    }

    static void connect() {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(16 * 1024 * 1024, 8, 4, 4, 0))
                                .addLast(new MessageDecoder())
                                .addLast(new MessageEncoder())
                                // 20s 写空闲发心跳
                                .addLast(new IdleStateHandler(0, 20, 0, TimeUnit.SECONDS))
                                .addLast(new ClientHandler());

                    }
                });
        bootstrap.connect("127.0.0.1", 1123).addListener(f ->{
            if (f.isSuccess()) {
                System.out.println("[Client] connected successfully.");
            } else {
                System.out.println("[Client] connect failed, retry in 3s.");
                group.schedule(NettyClient::connect, 3, TimeUnit.SECONDS);
            }
        });
    }
}
