package meow.studio.netty.diytcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import static meow.studio.netty.diytcp.Protocol.*;

public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    private int seqGen = 1;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        switch (msg.type) {
            case TYPE_ECHO_REQ -> {
                ctx.writeAndFlush(new Message(TYPE_ECHO_RES, msg.seq, msg.payload));
            }
            case TYPE_PING -> {
                ctx.writeAndFlush(new Message(TYPE_PONG, seqGen++, new byte[0]));
            }
            default -> {
                ctx.close();
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("[Meow Server] client connected: "+ ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
