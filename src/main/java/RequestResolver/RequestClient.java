/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package RequestResolver;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.xbill.DNS.Message;

import java.net.InetSocketAddress;

public class RequestClient {
    private final InetSocketAddress remoteAddress;
    private final Channel channel;

    public RequestClient(String subId, RequestRegistry registry, InetSocketAddress remoteAddress, String localAddress) {
        this.remoteAddress = remoteAddress;
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        this.channel = bootstrap.group(group).channel(NioDatagramChannel.class)
                .handler(new ChannelPipelineInitializer(subId, registry))
                .bind(localAddress, 0).syncUninterruptibly().channel();
    }

    public void send(Message message) {
        this.channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(message.toWire()), this.remoteAddress));
    }

    private class ChannelPipelineInitializer extends ChannelInitializer<NioDatagramChannel> {
        private final String subId;
        private final RequestRegistry registry;

        public ChannelPipelineInitializer(String subId, RequestRegistry registry) {
            this.subId = subId;
            this.registry = registry;
        }

        @Override
        protected void initChannel(NioDatagramChannel channel) throws Exception {
            channel.pipeline()
                    .addLast(new MessageDecoder())
                    .addLast(new ResponseHandler(this.subId, this.registry));
        }
    }
}
