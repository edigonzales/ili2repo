package ch.so.agi.ili2repo;

import java.io.File;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help;
import picocli.CommandLine.Option;
import picocli.CommandLine.Help.Ansi;

@Command(
        name = "ili2repo",
        description = "Creates an ilimodel.xml file from a directory with INTERLIS models.",
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
    File modeldir;

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        System.out.println("Hallo Welt.");
        return 0;
    }
    
    public static void main(String... args) {
        int exitCode = new CommandLine(new App())
                .execute(args);
        System.exit(exitCode);
    }
}
