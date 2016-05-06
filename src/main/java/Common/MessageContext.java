/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package Common;

import org.xbill.DNS.Message;

import java.net.InetSocketAddress;

public class MessageContext {
    private final InetSocketAddress senderAddress;
    private final Message message;

    public MessageContext(InetSocketAddress senderAddress, Message message) {
        this.senderAddress = senderAddress;
        this.message = message;
    }

    public InetSocketAddress getSenderAddress() {
        return senderAddress;
    }

    public Message getMessage() {
        return message;
    }
}
