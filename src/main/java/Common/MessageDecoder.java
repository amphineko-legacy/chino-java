/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package Common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.InetSocketAddress;

public abstract class MessageDecoder extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object obj) {
        DatagramPacket packet = (DatagramPacket) obj;
        InetSocketAddress sender = packet.sender();
        ByteBuf byteBuf = packet.content();
        try {
            byte[] buffer = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(buffer);
            Message message = new Message(buffer);
            ctx.fireChannelRead(new MessageContext(sender, message));
        } catch (IOException e) {
            this.decodeExceptionCaught(sender);
        } finally {
            byteBuf.release();
        }
    }

    protected abstract void decodeExceptionCaught(InetSocketAddress clientAddress);
}
