package meow.studio.netty.diytcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

import static meow.studio.netty.diytcp.Protocol.*;

public class ClientHandler extends SimpleChannelInboundHandler<Message> {
    private int seq = 1;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("[Client] connected: " + ctx.channel());
        byte[] hello = "Hello from client".getBytes(CharsetUtil.UTF_8);
        ctx.writeAndFlush(new Message(TYPE_ECHO_REQ, seq++, hello));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        switch (msg.type) {
            case TYPE_ECHO_RES -> {
                System.out.println("[Client] echo res seq=" + msg.seq + ", body=" + new String(msg.payload, CharsetUtil.UTF_8));
            }
            case TYPE_PONG -> {
                System.out.println("[Client] pong seq=" + msg.seq);
            }
            default -> {ctx.close();}
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent e && e.state() == IdleState.WRITER_IDLE) {
            ctx.writeAndFlush(new Message(TYPE_PING, seq++, new byte[0]));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("[Client] disconnected, try reconnect in 3s.");
        ctx.channel().eventLoop().schedule(NettyClient::connect, 3, TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
