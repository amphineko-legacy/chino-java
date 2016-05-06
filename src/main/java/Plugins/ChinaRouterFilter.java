/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package Plugins;

import PluginManager.Plugin;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChinaRouterFilter implements Plugin {
    private final List<SubnetInfo> rules;

    public ChinaRouterFilter(List<String> cidrList) {
        this.rules = new ArrayList<>();
        cidrList.forEach((rule) -> rules.add(new SubnetUtils(rule).getInfo()));
    }

    @Override
    public Record[] select(HashMap<String, Message> responses) {
        Record[] dirtyRecords = responses.get("dirty").getSectionArray(Section.ANSWER);
        for (Record record : dirtyRecords)
            for (SubnetInfo subnet : this.rules)
                if (!subnet.isInRange(record.rdataToString()))
                    return responses.get("clean").getSectionArray(Section.ANSWER);
        return dirtyRecords;
    }
}
