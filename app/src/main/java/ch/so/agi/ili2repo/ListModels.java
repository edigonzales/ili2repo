package ch.so.agi.ili2repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.interlis2.validator.Validator;

import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ili2c.metamodel.Model;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ilirepository.IliManager;
import ch.interlis.iom_j.Iom_jObject;
import ch.interlis.iom_j.xtf.XtfModel;
import ch.interlis.iom_j.xtf.XtfWriterBase;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.EndBasketEvent;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.StartBasketEvent;
import ch.interlis.iox_j.StartTransferEvent;
import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.settings.Settings;
import ch.interlis.models.ILIREPOSITORY20;

public class ListModels {
    private final static String REPOSITORY_MODELNAME = "IliRepository20";
    
    private final static String BID="b1";
    private final static String ILI_TOPIC = REPOSITORY_MODELNAME+".RepositoryIndex";
    private final static String ILI_CLASS = ILI_TOPIC+".ModelMetadata";
    private final static String ILI_STRUCT_MODELNAME = REPOSITORY_MODELNAME+".ModelName_";
    
    private final static String SENDER = "ILI2REPO";

    private final static List<String> DEFAULT_MODEL_REPOS = List.of("http://models.interlis.ch/", "https://models.geo.admin.ch/", "https://models.kgk-cgc.ch/");

    
    public boolean listModels(File modelsDir) {
        var failed = false;
                    
        OutputStream outStream = null;
        XtfWriterBase ioxWriter = null;
        try {
            var destFile = Paths.get(modelsDir.getAbsolutePath(), "ilimodels.xml");
            outStream = new FileOutputStream(destFile.toFile());
            ioxWriter = new XtfWriterBase(outStream, ILIREPOSITORY20.getIoxMapping(),"2.3");
            
            ioxWriter.setModels(new XtfModel[]{ILIREPOSITORY20.getXtfModel()});
            var startTransferEvent = new StartTransferEvent();
            startTransferEvent.setSender(SENDER);
            ioxWriter.write(startTransferEvent);

            var startBasketEvent = new StartBasketEvent(ILIREPOSITORY20.RepositoryIndex, BID);
            ioxWriter.write(startBasketEvent);
                                        
            List<Path> models = new ArrayList<Path>();
            try (Stream<Path> walk = Files.walk(modelsDir.toPath())) {
                models = walk
                        .filter(p -> !Files.isDirectory(p))   
                        .filter(f -> isEndWith(f.toString()))
                        .collect(Collectors.toList());        
            }
            
            // In einem ersten Durchlauf werden die Verzeichnisse eruiert, damit diese
            // als (lokales) Repository beim Kompilieren verwendet werden k??nnen.
            // Welches Problem l??st das? Importieren lokale Modelle wiederum lokale Modelle,
            // werden diese entweder nicht gefunden (falls sie in keinem Online-Repo sind) oder
            // sie werden aus einem bestehenden/vorhanden Repository verwendet. Dies sollte 
            // m.E. nicht die Regel sein.
            Set<String> parentModelDirSet = new TreeSet<>();
            for (Path model : models) {
                parentModelDirSet.add(model.toFile().getAbsoluteFile().getParent());
            }
            List<String> repositories = new ArrayList<>();
            repositories.addAll(DEFAULT_MODEL_REPOS);
            repositories.addAll(parentModelDirSet);
            
            // Im zweiten Durchlauf wird die ilimodels.xml-Datei geschrieben.
            int i = 1;
            for (Path modelPath : models) {
                File file = modelPath.toFile();
                EhiLogger.logState(file.getName().toString());
                
                var td = getTransferDescriptionFromFileName(file.getAbsolutePath(), repositories);

                // Mehrere Modelle in einer ili-Datei.
                for (Model lastModel : td.getModelsFromLastFile()) {
                    Iom_jObject iomObj = new Iom_jObject(ILI_CLASS, String.valueOf(i));
                    iomObj.setattrvalue("Name", lastModel.getName());

                    if (lastModel.getIliVersion().equalsIgnoreCase("1")) {
                        iomObj.setattrvalue("SchemaLanguage", "ili1");
                    } else if (lastModel.getIliVersion().equalsIgnoreCase("2.2")) {
                        iomObj.setattrvalue("SchemaLanguage", "ili2_2");
                    } else if (lastModel.getIliVersion().equalsIgnoreCase("2.3")) {
                        iomObj.setattrvalue("SchemaLanguage", "ili2_3");
                    } else if (lastModel.getIliVersion().equalsIgnoreCase("2.4")) {
                        iomObj.setattrvalue("SchemaLanguage", "ili2_4");
                    }
                    
                    String filePath = file.getAbsoluteFile().getParent().replace(modelsDir.getAbsolutePath(), "");

                    if (filePath.startsWith(FileSystems.getDefault().getSeparator())) {
                        filePath = filePath.substring(1);
                    }
                    
                    // Falls das Modell im models-Root-Verzeichnis liegt, darf
                    // kein Separator hinzugef??gt werden.
                    String iomFileString;
                    if (filePath.length() == 0) {
                        iomFileString = file.getName();
                    } else  {
                        iomFileString = filePath + "/" + file.getName();
                    }
                    
                    iomObj.setattrvalue("File", iomFileString);

                    if (lastModel.getModelVersion() == null) {
                        iomObj.setattrvalue("Version", "1582-01-01");
                    } else {
                        iomObj.setattrvalue("Version", lastModel.getModelVersion());
                    }

                    try {
                        iomObj.setattrvalue("Issuer", lastModel.getIssuer());
                    } catch (IllegalArgumentException e) {
                    }

                    var modelTechnicalContact = lastModel.getMetaValue("technicalContact");
                    if (modelTechnicalContact != null) {
                        if (isValidEmail(modelTechnicalContact) && !modelTechnicalContact.startsWith("mailto")) {
                            iomObj.setattrvalue("technicalContact", "mailto:"+modelTechnicalContact);     
                        } else {
                            iomObj.setattrvalue("technicalContact", lastModel.getMetaValue("technicalContact"));     
                        }
                    }

                    var furtherInformation = lastModel.getMetaValue("furtherInformation"); 
                    if (furtherInformation != null && furtherInformation.startsWith("http")) {
                        iomObj.setattrvalue("furtherInformation", furtherInformation);
                    }

                    var title = lastModel.getMetaValue("Title");                
                    if (title != null) {
                        iomObj.setattrvalue("Title", title);
                    }
                    
                    var shortDescription = lastModel.getMetaValue("shortDescription");
                    if (shortDescription != null) {
                        iomObj.setattrvalue("shortDescription", shortDescription);
                    }

                    // JDK only ben??tigt trotzdem JAXB wegen "DatatypeConverter.printHexBinary"
                    try (var is = Files.newInputStream(Paths.get(file.getAbsolutePath()))) {
                        var md5 = DigestUtils.md5Hex(is);
                        iomObj.setattrvalue("md5", md5);
                    }

                    // Importierte Modell                
                    for (Model model : lastModel.getImporting()) {
                        Iom_jObject iomObjDependsOnModel = new Iom_jObject(ILI_STRUCT_MODELNAME, null);
                        
                        // see https://github.com/claeis/ili2c/issues/75
                        if (!model.getName().equalsIgnoreCase("INTERLIS")) {
                            iomObjDependsOnModel.setattrvalue("value",  model.getName());
                            iomObj.addattrobj("dependsOnModel", iomObjDependsOnModel);
                        }
                    }

                    // TODO: translationOf
                    
                    ioxWriter.write(new ch.interlis.iox_j.ObjectEvent(iomObj));   
                    i++;  

                }                
            }

            ioxWriter.write(new EndBasketEvent());
            ioxWriter.write(new EndTransferEvent());
            
            ioxWriter.flush();
            
            // Validate ilimodels.xml
            Settings settings = new Settings();
            settings.setValue(Validator.SETTING_ILIDIRS, Validator.SETTING_DEFAULT_ILIDIRS);
            settings.setValue(Validator.SETTING_ALL_OBJECTS_ACCESSIBLE, Validator.TRUE);
            boolean valid = Validator.runValidation(destFile.toFile().getAbsolutePath(), settings);

            if (!valid) {
                EhiLogger.logError("ilimodels.xml is not valid");
                failed = true;
            }
            
        } catch (FileNotFoundException e) {
            e.printStackTrace(); // TODO: remove
            
            EhiLogger.logError(e);
            failed = true;
        } catch (IoxException e) {
            EhiLogger.logError(e);
            failed = true;
        } catch (IOException e) {
            EhiLogger.logError(e);
            failed = true;
        } catch (Ili2cException e) {
            EhiLogger.logError(e);
            failed = true;
        } finally {
            if (ioxWriter != null) {
                try {
                    ioxWriter.close();
                } catch (IoxException e) {
                    EhiLogger.logError(e);
                }
                ioxWriter = null;
            }
            if (outStream!=null && outStream!=System.out) {
                try {
                    outStream.close();              
                } catch (IOException e){
                    EhiLogger.logError(e);
                }
                outStream=null;
            }
        }
        return failed;
    }
    
    private static boolean isEndWith(String file) {
        if (file.toLowerCase().endsWith("ili")) {
            return true;
        }
        return false;
    }
     
    private TransferDescription getTransferDescriptionFromFileName(String fileName, List<String> repositories) throws Ili2cException {
        IliManager manager = new IliManager();               
        manager.setRepositories(repositories.toArray(new String[0]));
        
        ArrayList<String> ilifiles = new ArrayList<String>();
        ilifiles.add(fileName);
        var config = manager.getConfigWithFiles(ilifiles);
        var td = Ili2c.runCompiler(config);
                
        if (td == null) {
            throw new IllegalArgumentException("INTERLIS compiler failed");
        }
        
        return td;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";
        Pattern pat = Pattern.compile(emailRegex);
        if (email == null) {
            return false;
        } 
        return pat.matcher(email).matches();
    }
}
