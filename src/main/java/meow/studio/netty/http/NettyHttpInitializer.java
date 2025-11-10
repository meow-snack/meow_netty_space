package meow.studio.netty.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

public class NettyHttpInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();

        // HTTP 请求编解码器
        pipeline.addLast("MeowHttpServerCodec", new HttpServerCodec());

        // HTTP 请求处理器
        pipeline.addLast("MeowHttpServerHandler", new NettyHttpServerHandler());

        System.out.printf("[%s] 已就绪.\n", this.getClass().getName());
    }
}
