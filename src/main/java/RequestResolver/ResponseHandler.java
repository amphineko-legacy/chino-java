/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package RequestResolver;

import Common.MessageContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ResponseHandler extends ChannelInboundHandlerAdapter {
    private final String subId;
    private final RequestRegistry registry;

    public ResponseHandler(String subId, RequestRegistry registry) {
        this.subId = subId;
        this.registry = registry;
    }

    @Override
    public void channelRead(ChannelHandlerContext channelCtx, Object obj) {
        MessageContext messageCtx = (MessageContext) obj;
        RequestEntry entry;
        if ((entry = this.registry.getRequestById(messageCtx.getMessage().getHeader().getID())) != null)
            entry.setSubResponse(this.subId, messageCtx.getMessage());
    }
}
