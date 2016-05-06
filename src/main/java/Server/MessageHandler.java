/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package Server;

import Common.MessageContext;
import RequestResolver.RequestResolver;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class MessageHandler extends ChannelInboundHandlerAdapter {
    private final Server server;
    private final ResponseSelector selector;
    private final RequestResolver resolver;

    public MessageHandler(Server server, ResponseSelector selector, RequestResolver resolver) {
        this.server = server;
        this.selector = selector;
        this.resolver = resolver;
    }

    @Override
    public void channelRead(ChannelHandlerContext channelCtx, Object obj) {
        MessageContext messageCtx = (MessageContext) obj;
        this.resolver.resolve(messageCtx.getMessage().getQuestion(), new ResponseListener(this.server, this.selector, messageCtx));
    }
}
