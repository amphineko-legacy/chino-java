/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package RequestResolver;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.xbill.DNS.Message;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class RequestEntry implements TimerTask {
    private final int id;
    private final int totalSubResponses;
    private final ResponseCallback callback;
    private final RequestRegistry registry;
    private boolean closed;
    private ReentrantLock closeStateLock;
    private HashMap<String, Message> responses;
    private ReentrantLock responseHashMapLock;

    public RequestEntry(int id, int totalSubResponses, ResponseCallback callback, RequestRegistry registry) {
        this.id = id;
        this.totalSubResponses = totalSubResponses;
        this.callback = callback;
        this.registry = registry;
        this.closed = false;
        this.closeStateLock = new ReentrantLock();
        this.responses = new HashMap<>();
        this.responseHashMapLock = new ReentrantLock();
    }

    public boolean close() {
        try {
            this.closeStateLock.lock();
            if (this.closed)
                return false;
            else {
                this.registry.removeRequest(this.id);
                return this.closed = true;
            }
        } finally {
            this.closeStateLock.unlock();
        }
    }

    public int getId() {
        return this.id;
    }

    @Override
    public void run(Timeout timeout) {
        if (this.close())
            this.callback.setTimeout();
    }

    public void setSubResponse(String subId, Message response) {
        try {
            this.responseHashMapLock.lock();
            this.responses.put(subId, response);
            if (this.responses.size() == this.totalSubResponses)
                if (this.close())
                    this.callback.setResponses(this.responses); // TODO: possible encounter longer gc or memory leak
        } finally {
            this.responseHashMapLock.unlock();
        }
    }
}
