package ch.so.agi.ili2repo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class ListModelsTest {
    @Test
    public void listModels_Ok() throws IOException {
        var modelsDir = new File("src/test/data/models/");
        var failed = new ListModels().listModels(modelsDir);
        
        assertFalse(failed);
        
        Path xmlPath = Path.of(modelsDir.getAbsolutePath(), "ilimodels.xml");
        String xmlContent = Files.readString(xmlPath);
        
        assertTrue(xmlContent.contains("<Name>SO_ARP_Nutzungsplanung_Nachfuehrung_20201005_Validierung_20201005</Name>"));
        assertTrue(xmlContent.contains("<Name>SO_ARP_Nutzungsplanung_Nachfuehrung_20201005</Name>"));
        assertTrue(xmlContent.contains("<Name>SO_FunctionsExt</Name>"));
        assertTrue(xmlContent.contains("<File>dm01avso24lv95.ili</File>"));
        
        // TODO: org.xmlunit wäre zum Vergleich gut. Wie gut ist GraalVM native Unterstützung?
    }
}
