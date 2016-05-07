/*
 * Copyright (c) 2016 Naoki Rinmous. This file is released under MIT license.
 */

import PluginManager.PluginManager;
import Plugins.ChinaRouterFilter;
import RequestResolver.RequestResolver;
import Server.Server;
import com.moandjiezana.toml.Toml;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

public class Chino {
    public static Toml config;
    protected static Logger logger = LoggerFactory.getLogger(Chino.class.getSimpleName());

    public static CommandLine getCommandLine(String[] args) {
        Options options = new Options();
        options.addOption("f", "config", true, "Configuration file");
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            logger.error("Invalid arguments");
            return null;
        }
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

    public static void main(String[] args) {
        CommandLine cmd;
        if ((cmd = getCommandLine(args)) == null)
            System.exit(1);

        // demand argument "config"
        if (!cmd.hasOption("f")) {
            logger.error("Missing configuration file");
            System.exit(1);
        }

        // load config toml
        loadConfig(cmd.getOptionValue("f"));
        logger.info("Loaded configuration file");

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
        logger.info("RequestResolver initialized, [clean={}, dirty={}]", cleanRemote, dirtyRemote);

        // construct plugin manager
        PluginManager pluginManager = new PluginManager();

        // setup ChinaRouteFilter
        if (config.contains("chn-route.path")) {
            ChinaRouterFilter plugin = ChinaRouterFilter.loadFromFile(config.getString("chn-route.path"));
            if (plugin == null)
                System.exit(1);
            pluginManager.add(plugin);
            logger.info("ChinaRouteFilter setup completed");
        }

        // setup server
        new Server(
                config.getString("listen.address", "localhost"),
                config.getLong("listen.port", 53L).intValue(),
                pluginManager, resolver);
        // TODO: replace hard coded default address & port

        logger.info("Chino ready, prprpr >w<");
    }
}
