package meow.studio.netty.complex.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;


public class NettyHttpInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast("codec", new HttpServerCodec())
                .addLast("expect", new HttpServerExpectContinueHandler())
                .addLast("keepAlive", new HttpServerKeepAliveHandler())
                .addLast("compressor", new HttpContentCompressor())
                .addLast("chunked", new ChunkedWriteHandler())
                .addLast("agg", new HttpObjectAggregator(1 << 20))
                .addLast("router", new NettyHttpRouterHandler());
    }
}
