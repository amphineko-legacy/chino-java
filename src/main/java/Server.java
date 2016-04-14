import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static Logger logger = LoggerFactory.getLogger(Server.class);

    private class QueryHandleTask implements Runnable {
        private ChannelHandlerContext context;
        private DatagramPacket message;
        private byte[] messageBytes;
        private QueryHandler queryHandler;

        public QueryHandleTask(QueryHandler queryHandler, ChannelHandlerContext context, DatagramPacket message, byte[] messageBytes) {
            this.context = context;
            this.message = message;
            this.messageBytes = messageBytes;
            this.queryHandler = queryHandler;
        }

        @Override
        public void run() {
            InetSocketAddress clientAddress = this.message.sender();
            Thread.currentThread().setName("query" + clientAddress.toString());
            try {
                Message query = new Message(this.messageBytes);
                Message response = this.queryHandler.handle(query);
                context.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(response.toWire()), clientAddress));
            } catch (IOException e) {
                logger.error("failed to parse message");
            }
        }
    }

    private class IncomingMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        private QueryHandler queryHandler;
        private ExecutorService taskPool;

        public IncomingMessageHandler(QueryHandler queryHandler, ExecutorService taskPool) {
            this.queryHandler = queryHandler;
            this.taskPool = taskPool;
        }

        @Override
        protected void messageReceived(ChannelHandlerContext context, DatagramPacket message) {
            ByteBuf messageContent = message.content();
            byte[] messageBytes = new byte[messageContent.readableBytes()];
            messageContent.readBytes(messageBytes);
            this.taskPool.submit(new QueryHandleTask(this.queryHandler, context, message, messageBytes));
        }
    }

    private IncomingMessageHandler incomingMessageHandler;
    private ExecutorService taskPool;

    public Server(QueryHandler queryHandler) {
        this.taskPool = Executors.newCachedThreadPool();
        this.incomingMessageHandler = new IncomingMessageHandler(queryHandler, this.taskPool);
    }

    public void listen(String address, int port) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(this.incomingMessageHandler);
            ChannelFuture future = bootstrap.bind(new InetSocketAddress(address, port)).sync();
            logger.info("Listening on [{}, {}]", address, port);
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
            logger.info("Server shutdown");
        }
    }
}

