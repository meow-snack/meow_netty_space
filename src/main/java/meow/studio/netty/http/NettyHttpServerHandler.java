package meow.studio.netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.URI;

public class NettyHttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        System.out.printf("[%s] channel = %s\n", this.getClass().getName(), ctx.channel());
        System.out.printf("[%s] pipeline = %s\n", this.getClass().getName(), ctx.pipeline());
        System.out.printf("[%s] 通过 pipeline 获取 channel = %s\n", this.getClass().getName(),ctx.pipeline().channel());
        System.out.printf("[%s] 当前 context 的 handler = %s\n", this.getClass().getName(),ctx.handler());
        System.out.println("==============================================================================");

        if (msg instanceof HttpRequest request) {
            System.out.printf("[%s] 捕捉到 HTTP 请求 = %s\n", this.getClass().getName(), request);
            System.out.printf("[%s] context type = %s\n", this.getClass().getName(), ctx.getClass());
            System.out.printf("[%s] pipeline hashcode = %s\n", this.getClass().getName(), ctx.pipeline().hashCode());
            System.out.printf("[%s] NettyHttpServerHandler hashcode = %s\n", this.getClass().getName(), this.hashCode());
            System.out.printf("[%s] message type = %s\n", this.getClass().getName(), msg.getClass());
            System.out.printf("[%s] client address = %s\n", this.getClass().getName(), ctx.channel().remoteAddress());

            URI uri = new URI(request.uri());

            // 资源过滤
            if ("/favicon.ico".equals(uri.getPath())) {
                System.out.printf("[%s] 客户端请求了: favicon.ico, 不作响应\n", this.getClass().getName());
                return;
            }

            // http 协议回复信息
            ByteBuf content = Unpooled.copiedBuffer("Meow~ 我是服务器!", CharsetUtil.UTF_8);

            // 构建 http 响应
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

            ctx.writeAndFlush(response);
        }
    }
}
