/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

package Plugins;

import PluginManager.Plugin;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChinaRouterFilter implements Plugin {
    private static final Logger logger = LoggerFactory.getLogger(ChinaRouterFilter.class);

    private final List<SubnetInfo> rules;

    public ChinaRouterFilter(List<String> cidrList) throws IllegalArgumentException {
        this.rules = new ArrayList<>();
        cidrList.forEach((rule) -> rules.add(new SubnetUtils(rule).getInfo()));
    }

    public static ChinaRouterFilter loadFromFile(String filename) {
        List<String> results = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null)
                results.add(line);
            br.close();

            return new ChinaRouterFilter(results);
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Failed to load CIDR list");
            return null;
        }
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
