/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package RequestResolver;

import org.xbill.DNS.*;

import java.net.InetSocketAddress;
import java.util.HashMap;

public class RequestResolver {
    private final HashMap<String, RequestClient> clients;
    private final RequestRegistry registry;

    public RequestResolver(HashMap<String, InetSocketAddress> remotes, int defaultTimeout, String localAddress) {
        this.clients = new HashMap<>();
        this.registry = new RequestRegistry(defaultTimeout, remotes.size());
        remotes.forEach((subId, remoteAddress) ->
                this.clients.put(subId, new RequestClient(subId, this.registry, remoteAddress, localAddress)));
    }

    public void resolve(Record question, ResponseCallback callback) {
        RequestEntry entry = this.registry.registerRequest(callback);
        Message message = new Message(entry.getId());
        Header header = message.getHeader();
        header.setFlag(Flags.RD);
        message.setHeader(header);
        message.addRecord(question, Section.QUESTION);
        this.clients.forEach((subId, client) -> client.send(message));
    }
}
