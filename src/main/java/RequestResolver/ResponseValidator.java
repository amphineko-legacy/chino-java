/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package RequestResolver;

import Common.MessageContext;
import Server.ResponseListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Flags;

public class ResponseValidator extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ResponseListener.class);

    @Override
    public void channelRead(ChannelHandlerContext channelCtx, Object msg) {
        MessageContext messageCtx = (MessageContext) msg;
        if (messageCtx.getMessage().getHeader().getFlag(Flags.RA))
            channelCtx.fireChannelRead(msg);
        else
            logger.error("UPSTREAM {} INVALID RESPONSE FLAG", messageCtx.getSenderAddress().toString());
    }
}
