/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package PluginManager;

import Server.ResponseSelector;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class PluginManager implements ResponseSelector {
    private final ArrayList<Plugin> plugins;

    public PluginManager() {
        this.plugins = new ArrayList<>();
    }

    public void add(Plugin plugin) {
        this.plugins.add(plugin);
    }

    @Override
    public Record[] select(HashMap<String, Message> responses) {
        Record[] responseSet = null;
        Iterator<Plugin> iterator = this.plugins.iterator();
        while ((responseSet == null) && iterator.hasNext())
            responseSet = iterator.next().select(responses);
        return responseSet;
    }
}
