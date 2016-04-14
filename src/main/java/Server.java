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

public class Server {
    private static Logger logger = LoggerFactory.getLogger(Server.class);

    private class IncomingMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        protected QueryHandler queryHandler;

        public IncomingMessageHandler(QueryHandler dispatcher) {
            this.queryHandler = dispatcher;
        }

        @Override
        protected void messageReceived(ChannelHandlerContext context, DatagramPacket message) {
            InetSocketAddress clientAddress = message.sender();
            Thread.currentThread().setName("query" + clientAddress.toString());
            // extract message bytes
            ByteBuf messageContent = message.content();
            byte[] messageBytes = new byte[messageContent.readableBytes()];
            messageContent.readBytes(messageBytes);
            // construct DNS message
            try {
                Message query = new Message(messageBytes);
                Message response = this.queryHandler.handle(query);
                context.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(response.toWire()), clientAddress));
            } catch (IOException e) {
                logger.error("failed to parse message");
            }
        }
    }

    private IncomingMessageHandler incomingMessageHandler;

    public Server(QueryHandler queryHandler) {
        this.incomingMessageHandler = new IncomingMessageHandler(queryHandler);
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

