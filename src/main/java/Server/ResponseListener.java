/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package Server;

import Common.MessageContext;
import RequestResolver.ResponseCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import java.util.HashMap;

public class ResponseListener implements ResponseCallback {
    private static final Logger logger = LoggerFactory.getLogger(ResponseListener.class);

    private final Server server;
    private final ResponseSelector selector;
    private final MessageContext context;

    public ResponseListener(Server server, ResponseSelector selector, MessageContext context) {
        this.server = server;
        this.selector = selector;
        this.context = context;
    }

    @Override
    public void setResponses(HashMap<String, Message> responses) {
        Record[] selected = this.selector.select(responses);
        Message response = new Message(this.context.getMessage().getHeader().getID());
        Header header = response.getHeader();
        header.setFlag(Flags.QR);
        header.setFlag(Flags.RA);
        header.setFlag(Flags.RD);
        if (selected != null) {
            response.addRecord(this.context.getMessage().getQuestion(), Section.QUESTION);
            for (Record record : selected)
                response.addRecord(record, Section.ANSWER);
            header.setRcode(Rcode.NOERROR);
        } else {
            logger.error("NOT SELECTED???");
            header.setRcode(Rcode.SERVFAIL);
        }
        response.setHeader(header);
        this.server.send(response, this.context.getSenderAddress());
    }

    @Override
    public void setTimeout() {
        Message response = new Message(this.context.getMessage().getHeader().getID());
        Header header = response.getHeader();
        header.setRcode(Rcode.SERVFAIL);
        response.setHeader(header);
    }
}
