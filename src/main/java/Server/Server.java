/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package Server;

import RequestResolver.RequestResolver;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.xbill.DNS.Message;

import java.net.InetSocketAddress;

public class Server {
    private final Channel channel;

    public Server(String address, int port, ResponseSelector selector, RequestResolver resolver) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        this.channel = bootstrap.group(group).channel(NioDatagramChannel.class)
                .handler(new ChannelPipelineInitializer(this, selector, resolver))
                .bind(address, port).syncUninterruptibly().channel();
    }

    public void send(Message message, InetSocketAddress remote) {
        this.channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(message.toWire()), remote));
    }

    private class ChannelPipelineInitializer extends ChannelInitializer<NioDatagramChannel> {
        private final Server server;
        private final ResponseSelector selector;
        private final RequestResolver resolver;

        public ChannelPipelineInitializer(Server server, ResponseSelector selector, RequestResolver resolver) {
            this.server = server;
            this.selector = selector;
            this.resolver = resolver;
        }

        @Override
        protected void initChannel(NioDatagramChannel channel) throws Exception {
            channel.pipeline()
                    .addLast(new MessageDecoder())
                    .addLast(new MessageHandler(this.server, this.selector, this.resolver));
        }
    }
}
