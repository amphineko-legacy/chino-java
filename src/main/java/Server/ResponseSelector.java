/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package Server;

import org.xbill.DNS.Message;
import org.xbill.DNS.Record;

import java.util.HashMap;

public interface ResponseSelector {
    Record[] select(HashMap<String, Message> responses);
}
