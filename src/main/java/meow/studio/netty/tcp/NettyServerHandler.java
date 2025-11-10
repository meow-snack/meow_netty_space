package meow.studio.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.CharsetUtil;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    /**
     * read 逻辑处理
     * @param ctx: 上下文对象, 包含 pipeline, channel, address
     * @param msg: 客户端发送的消息
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        System.out.printf("服务器读取线程[%s]: channel=%s; ctx=%s\n", Thread.currentThread().getName(), ctx.channel(), ctx);
        System.out.println("服务器检查 channel 和 pipeline 之间的关系: ");

        Channel channel = ctx.channel();
        ChannelPipeline pipeline = ctx.pipeline(); // 双向链接

        // 注意: 这里是 Netty 自己实现的 ByteBuf 而非 Nio 提供的 ByteBuffer
        ByteBuf buf = (ByteBuf) msg;
        System.out.printf("客户端[%s]发送消息: %s\n", channel.remoteAddress(), buf.toString(CharsetUtil.UTF_8));
    }

    /**
     * 当一次数据读取完成后触发, 向客户端发送响应: hello, 客户端~
     * @param ctx: 上下文对象, 包含 pipeline, channel, address
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        String message = String.format("服务器接收客户端[%s]消息完毕.", ctx.channel().remoteAddress());
        ctx.writeAndFlush(Unpooled.copiedBuffer(message.getBytes()));
    }

    /**
     * 异常处理
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
