/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package RequestResolver;

import org.xbill.DNS.Message;

import java.util.HashMap;

public interface ResponseCallback {
    void setResponses(HashMap<String, Message> responses);

    void setTimeout();
}
