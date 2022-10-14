package ch.so.agi.ili2repo.httpd;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * 
 * @author Stefan Ziegler. It's more or less copied from https://github.com/jbangdev/jbang-catalog/blob/main/httpd.java
 * All credits belong to the authors of the jbang "script".
 *
 */
public class Httpd {

    private String bind = "127.0.0.1";
    private int port = 8820;
    private String directory;

    static Logger logger;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%5$s %n");
        logger = Logger.getLogger("http");
    }

    public Httpd(String bind, int port, String directory) {
        this.bind = bind;
        this.port = port;
        this.directory = directory;
    }
    
    public Httpd(String directory) {
        this.directory = directory;
    }
    
    public void start() throws UnknownHostException, IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(bind), port), 0);
        StaticFileHandler sfh = new StaticFileHandler("/", directory);
        server.createContext("/", sfh).getFilters().add(logging(logger));
        server.setExecutor(null);

        server.start();

        logger.info("Serving HTTP on " + bind + " port " + port + " (http://" + bind + ":" + port + "/) from " + directory + " ...");   
    }
    
    private static Filter logging(Logger logger) {
        return new Filter() {

            final SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");

            @Override
            public void doFilter(HttpExchange http, Chain chain) throws IOException {
                try {


                    chain.doFilter(http);
                } finally {
                    logger.info(String.format("%s %s %s [%s] \"%s %s\" %s %s",
                            http.getRemoteAddress().getAddress().getHostAddress(),
                            "-",
                            "-",
                            df.format(new Date()),
                            http.getRequestMethod(),
                            http.getRequestURI(),
                            http.getResponseCode(),
                            "-"));
                }
            }

            @Override
            public String description() {
                return "logging";
            }
        };
    }
}
