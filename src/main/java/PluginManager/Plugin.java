/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package PluginManager;

import org.xbill.DNS.Message;
import org.xbill.DNS.Record;

import java.util.HashMap;

public interface Plugin {
    Record[] select(HashMap<String, Message> responses);
}
