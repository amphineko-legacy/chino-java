/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class MessageDecoder extends Common.MessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);

    @Override
    protected void decodeExceptionCaught(InetSocketAddress clientAddress) {
        logger.debug("CLIENT {} INVALID MESSAGE", clientAddress.toString());
    }
}
