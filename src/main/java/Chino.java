/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

import PluginManager.PluginManager;
import Plugins.ChinaRouterFilter;
import RequestResolver.RequestResolver;
import Server.Server;
import com.moandjiezana.toml.Toml;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Chino {
    public static Toml config;
    protected static Logger logger = LoggerFactory.getLogger(Chino.class.getSimpleName());

    public static List<String> loadChnRouteCidr(String filename) {
        String line;
        List<String> results = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            while ((line = br.readLine()) != null)
                results.add(line);
            br.close();
        } catch (IOException e) {
            logger.error("Failed to read ChinaRoute CIDR list");
            System.exit(1);
        }
        return results;
    }

    public static void loadConfig(String filename) {
        try {
            try (FileReader fr = new FileReader(filename)) {
                config = new Toml().read(fr);
            }
        } catch (IOException e) {
            logger.error("Failed to read config file");
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
        // arguments
        Options options = new Options();
        options.addOption("f", "config", true, "Configuration file");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // demand argument "config"
        if (!cmd.hasOption("f")) {
            logger.error("Config file missing");
            System.exit(1);
        }

        // load config toml
        loadConfig(cmd.getOptionValue("f"));
        logger.info("Config loaded");

        // setup resolver
        String cleanRemote = config.getString("remote.clean", null);
        String dirtyRemote = config.getString("remote.dirty", null);
        if ((cleanRemote == null) || (dirtyRemote == null)) {
            logger.error("Invalid or undefined remotes");
            System.exit(1);
        }
        HashMap<String, InetSocketAddress> remotes = new HashMap<>();
        remotes.put("clean", new InetSocketAddress(config.getString("remote.clean"), 53));
        remotes.put("dirty", new InetSocketAddress(config.getString("remote.dirty"), 53));
        RequestResolver resolver = new RequestResolver(remotes, config.getLong("remote.timeout", (long) 3000).intValue(), "0.0.0.0");
        logger.info("Remotes set to [clean={}, dirty={}]", cleanRemote, dirtyRemote);

        // setup plugin manager
        PluginManager pluginManager = new PluginManager();

        // setup ChinaRouteFilter
        if (config.contains("chn-route.path")) {
            pluginManager.add(new ChinaRouterFilter(loadChnRouteCidr(config.getString("chn-route.path"))));
            logger.info("ChinaRouteFilter setup completed");
        }

        // setup server
        Server server = new Server(
                config.getString("listen.address", "localhost"),
                config.getLong("listen.port", 53L).intValue(),
                pluginManager, resolver);
        // TODO: replace hard coded default address & port

        logger.info("Chino ready, prprpr >w<");
    }
}
