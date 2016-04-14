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
import java.util.ArrayList;
import java.util.List;

public class Chino {
    protected static Logger logger = LoggerFactory.getLogger(Chino.class.getSimpleName());

    public static Toml config;

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

        // setup queryHandler
        String cleanRemote = config.getString("remote.clean", null);
        String dirtyRemote = config.getString("remote.dirty", null);
        if ((cleanRemote == null) || (dirtyRemote == null)) {
            logger.error("Invalid or undefined remotes");
            System.exit(1);
        }
        QueryHandler resolver = new QueryHandler(config.getString("remote.clean"), config.getString("remote.dirty"));
        logger.info("Remotes set to [clean={}, dirty={}]", cleanRemote, dirtyRemote);

        // setup ChinaRouteFilter
        if (config.contains("chn-route.path")) {
            resolver.addValidator(new ChinaRouteFilter(loadChnRouteCidr(config.getString("chn-route.path"))));
            logger.info("ChinaRouteFilter setup completed");
        }

        // setup server
        Server server = new Server(resolver);
        // TODO: replace hard coded default address & port
        server.listen(config.getString("listen.address", "localhost"), config.getLong("listen.port", 53L).intValue());
    }
}
