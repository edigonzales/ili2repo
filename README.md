# ili2repo

A command line tool that creates an ilimodels.xml file from a directory with INTERLIS model files.

## Manual

A german user manual can be found here [docs/user/](docs/user/user-manual-de.md) 

## Licencse

_ili2repo_ is licensed under the [MIT License](LICENSE).

## Status

_ili2repo_ is in early development state.

## System requirements

For JVM builds you need a Java Development Kit (JDK) version 17 or a more recent version. For building native images you need GraalVM version 22.2.0 for Java 17.
 
## Konfigurieren und Starten

### JVM

Die JVM-Version ist auf Github via [latest Release](https://github.com/edigonzales/ili2repo/releases/latest) unter dem Namen "ili2repo-<Version>-.zip" verfübar. Die Zip-Datei enthält sämtliche Bibliotheken ("lib") und eine Shell- resp. Batch-Datei ("bin") zum Starten des Programmes. Sie benötigt eine Installation von Java / einer JVM Version 17 oder höher.

```
./bin/ili2repo --help
```

### Native Images

Das Native Image benötigt keine Installation von Java / einer JVM. Dafür muss für das vorliegende Betriebssystem die jeweilige Version heruntergeladen werden. Diese liegen auf Github via [latest Release](https://github.com/edigonzales/ili2repo/releases/latest). In der Zip-Datei liegt ein einzelnes ausführbares Binary "ili2repo".

```
./ili2repo --help
```

## Develop