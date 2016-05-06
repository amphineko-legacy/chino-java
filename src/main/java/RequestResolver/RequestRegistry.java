/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package RequestResolver;

import io.netty.util.HashedWheelTimer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RequestRegistry {
    private final int defaultTimeout;
    private final int totalSubResponses;
    private final ConcurrentHashMap<Integer, RequestEntry> registry;
    private final HashedWheelTimer timer;
    private int currentId;

    public RequestRegistry(int defaultTimeout, int totalSubResponses) {
        this.defaultTimeout = defaultTimeout;
        this.totalSubResponses = totalSubResponses;
        this.currentId = 1;
        this.registry = new ConcurrentHashMap<>();
        this.timer = new HashedWheelTimer();
    }

    private int allocateId() {
        for (; this.registry.containsKey(this.currentId); this.currentId++)
            if (this.currentId >= 65530)
                this.currentId = 1;
        return this.currentId;
    }

    public RequestEntry getRequestById(int id) {
        return this.registry.getOrDefault(id, null);
    }

    public RequestEntry registerRequest(ResponseCallback callback) {
        RequestEntry entry = new RequestEntry(this.allocateId(), this.totalSubResponses, callback, this);
        this.registry.put(entry.getId(), entry);
        this.timer.newTimeout(entry, this.defaultTimeout, TimeUnit.MILLISECONDS);
        return entry;
    }

    public void removeRequest(int id) {
        this.registry.remove(id);
    }
}
