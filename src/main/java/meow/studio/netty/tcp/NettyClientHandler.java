package meow.studio.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.printf("客户端就绪线程[%s]: channel=%s; ctx=%s\n", Thread.currentThread().getName(), ctx.channel(), ctx);
        ctx.writeAndFlush(Unpooled.copiedBuffer("Hello, 服务器~", CharsetUtil.UTF_8));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.printf("客户端读取线程[%s]: channel=%s; ctx=%s\n", Thread.currentThread().getName(), ctx.channel(), ctx);
        ByteBuf bytebuf = (ByteBuf) msg;
        System.out.printf("服务器[%s]回复消息: %s\n", ctx.channel().remoteAddress(), bytebuf.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}

