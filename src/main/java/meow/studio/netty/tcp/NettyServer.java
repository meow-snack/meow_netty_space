package meow.studio.netty.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyServer {
    public static void main(String[] args) throws Exception {
        /*
         * https://netty.io/wiki/netty-4.2-migration-guide.html#new-best-practices
         * BossGroup & WorkerGroup
         * BossGroup 只负责处理 accept, 真正的业务逻辑处理交给 WorkerGroup
         * WorkerGroup 负责处理 read/write/业务逻辑
         * BossGroup 和 WorkerGroup 含有的子线程(NioEventLoop)的个数默认为: 实际 cpu 核*2
         */

        int cpuCoreNum = Runtime.getRuntime().availableProcessors();
        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(cpuCoreNum * 2, NioIoHandler.newFactory());

        try {
            // 服务端启动对象
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup) // 设置两个线程组
                    // 通道类型配置
                    // 使用 NioServerSocketChannel 作为服务器的通道
                    .channel(NioServerSocketChannel.class)
                    // 服务器选项
                    // SO_BACKLOG: 设置连接队列大小为128, 当客户端连接请求超过accept处理速度时，请求会在操作系统队列中等待
                    // 客户端请求 → [操作系统队列：SO_BACKLOG] → 服务器应用 accept() 处理
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 子通道选项
                    // SO_KEEPALIVE: 启用TCP心跳机制
                    // 自动检测空闲连接，防止连接僵死
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 处理器配置
                    // childHandler: 为每个新连接配置处理器
                    // ChannelInitializer: 连接建立时自动调用initChannel方法
                    // 在 initChannel() 方法中获取到 socketChannel 后，可以调用 handler 的 channelActive() 方法
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 创建一个通道初始化对象(匿名对象)
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            System.out.printf("客户端 SocketChannel Hashcode=%s\n", socketChannel.hashCode());
                            // ChannelPipeline 是 Netty 的核心概念，它是一个双向链表结构的处理器链：
                            // 客户端数据 → Head → Handler1 → Handler2 → ... → Tail → 业务处理
                            // 响应数据   ← Head ← Handler1 ← Handler2 ← ... ← Tail ← 业务逻辑
                            socketChannel.pipeline().addLast(new NettyServerHandler());
                        }
                    });
            System.out.println("服务器已就绪.");

            ChannelFuture channelFuture = null;
            try {
                // .bind(): 绑定服务器到指定端口: 1123, 监听端口并准备接收客户端的链接
                // .sync(): 同步等待: 阻塞当前线程直到绑定操作完成, 确保服务器启动后再执行后续代码
                // ChannelFuture: Netty的异步操作结果对象, 可监听操作完成事件, 获取操作状态, 用于管理 Channel 的生命周期
                channelFuture = bootstrap.bind(1123).sync();
                System.out.println("监听端口 1123 成功.");
            } catch (Exception e) {
                System.out.println("监听端口 1123 失败: " + e.getMessage());
                throw e;
            }

            // .closeFuture(): 表示 Channel 的异步关闭操作, 当服务器 Channel 关闭时, 这个 Future 会完成
            // .sync(): 阻塞当前线程, 等待服务器 Channel 关闭, 只有当服务器关闭时，才会继续执行finally块
            channelFuture.channel().closeFuture().sync();
        } finally {
            System.out.println("服务器已关闭.");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
