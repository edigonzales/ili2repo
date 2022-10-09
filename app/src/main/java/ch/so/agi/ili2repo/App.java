package ch.so.agi.ili2repo;

import java.io.Console;
import java.io.File;
import java.util.concurrent.Callable;

import ch.so.agi.ili2repo.httpd.Httpd;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "ili2repo",
        description = "Creates an ilimodels.xml file from a directory with INTERLIS model files.",
        //version = "ili2repo version 0.0.1",
        mixinStandardHelpOptions = true,
        
        headerHeading = "%n",
        //synopsisHeading = "%n",
        descriptionHeading = "%nDescription: ",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n"
      )
public class App implements Callable<Integer> {

    @Option(names = { "-d", "--directory" }, required = true, paramLabel = "MODELDIR", description = "The directory containing the INTERLIS models.")
    File modelsDir;
    
    @Option(names = { "-s", "--server" }, required = false, description = "Runs HTTPS server to serve out the models repository.") 
    boolean server;
    
    @Override
    public Integer call() throws Exception {
        
        // TODO: if/else logic
        
        if (!server) {
            var failed = new ListModels().listModels(modelsDir);
            return failed ? 1 : 0;
        }

        if (server) {
            new Httpd(modelsDir.getAbsolutePath()).start();
            
            final Console console = System.console();
            String line;
            do {
                System.out.println("Ctrl-C to stop server...");
                line = console.readLine();
            } while (line == null);
        }
        
        return 0;
    }
    
    public static void main(String... args) {
        int exitCode = new CommandLine(new App())
                .execute(args);
        System.exit(exitCode);
    }
}
