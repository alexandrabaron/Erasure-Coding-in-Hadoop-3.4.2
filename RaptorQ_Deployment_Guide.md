# Guide de Déploiement et d'Utilisation - RaptorQ pour Hadoop

## Prérequis

### 1. Environnement de Développement
- **Java** : JDK 8 ou supérieur
- **Maven** : 3.6 ou supérieur
- **Hadoop** : 3.3.0 ou supérieur
- **IDE** : IntelliJ IDEA, Eclipse, ou VS Code

### 2. Dépendances
- **OpenRQ** : Bibliothèque RaptorQ (intégrée dans le projet)
- **Hadoop Common** : Bibliothèque Hadoop de base
- **JUnit** : Framework de tests

## Installation

### 1. Compilation du Projet

```bash
# Cloner ou télécharger le projet
cd erasurecode

# Compiler le projet
mvn clean compile

# Exécuter les tests
mvn test

# Créer le package JAR
mvn clean package
```

### 2. Intégration dans Hadoop

#### Option A : Intégration Directe
```bash
# Copier les fichiers dans le répertoire Hadoop
cp -r codec/ $HADOOP_HOME/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/erasurecode/
cp -r coder/ $HADOOP_HOME/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/erasurecode/
cp -r rawcoder/ $HADOOP_HOME/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/erasurecode/
cp -r util/ $HADOOP_HOME/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/erasurecode/
cp META-INF/ $HADOOP_HOME/hadoop-common-project/hadoop-common/src/main/resources/

# Modifier les fichiers existants
# - ErasureCodeConstants.java
# - CodecUtil.java
```

#### Option B : Module Séparé
```bash
# Créer un module séparé dans Hadoop
mkdir $HADOOP_HOME/hadoop-common-project/hadoop-erasurecode-raptorq
cp -r * $HADOOP_HOME/hadoop-common-project/hadoop-erasurecode-raptorq/

# Ajouter la dépendance dans le pom.xml parent
```

### 3. Compilation de Hadoop

```bash
cd $HADOOP_HOME
mvn clean package -DskipTests
```

## Configuration

### 1. Configuration Core-Site.xml

```xml
<configuration>
  <!-- Configuration du codec RaptorQ -->
  <property>
    <name>io.erasurecode.codec.raptorq</name>
    <value>org.apache.hadoop.io.erasurecode.codec.RaptorQErasureCodec</value>
    <description>Classe du codec RaptorQ</description>
  </property>

  <!-- Configuration des raw coders -->
  <property>
    <name>io.erasurecode.codec.raptorq.rawcoders</name>
    <value>raptorq_java</value>
    <description>Raw coder à utiliser pour RaptorQ</description>
  </property>

  <!-- Activation des codes d'effacement natifs -->
  <property>
    <name>io.erasurecode.codec.native.enabled</name>
    <value>true</value>
    <description>Activer les implémentations natives</description>
  </property>
</configuration>
```

### 2. Configuration HDFS-Site.xml

```xml
<configuration>
  <!-- Activation des codes d'effacement -->
  <property>
    <name>dfs.namenode.ec.policies.enabled</name>
    <value>true</value>
    <description>Activer les politiques de codes d'effacement</description>
  </property>

  <!-- Répertoire pour les métadonnées EC -->
  <property>
    <name>dfs.namenode.ec.policies.dir</name>
    <value>/tmp/hadoop-ec-policies</value>
    <description>Répertoire pour les politiques EC</description>
  </property>
</configuration>
```

## Utilisation

### 1. Création de Politiques d'Effacement

```bash
# Créer une politique RaptorQ 6+3
hdfs ec -addPolicy -policy RaptorQ-6-3 -replication 6,3 -codec raptorq

# Lister les politiques disponibles
hdfs ec -listPolicies

# Obtenir les détails d'une politique
hdfs ec -getPolicy -policy RaptorQ-6-3
```

### 2. Application de Politiques

```bash
# Appliquer une politique à un répertoire
hdfs ec -setPolicy -path /data/important -policy RaptorQ-6-3

# Vérifier la politique appliquée
hdfs ec -getPolicy -path /data/important

# Supprimer une politique
hdfs ec -unsetPolicy -path /data/important
```

### 3. Utilisation Programmatique

#### Exemple Simple
```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.erasurecode.ErasureCodecOptions;
import org.apache.hadoop.io.erasurecode.ECSchema;
import org.apache.hadoop.io.erasurecode.codec.RaptorQErasureCodec;
import org.apache.hadoop.io.erasurecode.coder.ErasureEncoder;
import org.apache.hadoop.io.erasurecode.coder.ErasureDecoder;

public class RaptorQExample {
    public static void main(String[] args) {
        Configuration conf = new Configuration();
        
        // Créer le schéma RaptorQ (6 blocs de données, 3 blocs de parité)
        ECSchema schema = new ECSchema("raptorq", 6, 3);
        ErasureCodecOptions options = new ErasureCodecOptions(schema);
        
        // Créer le codec
        RaptorQErasureCodec codec = new RaptorQErasureCodec(conf, options);
        
        // Créer l'encodeur et le décodeur
        ErasureEncoder encoder = codec.createEncoder();
        ErasureDecoder decoder = codec.createDecoder();
        
        System.out.println("RaptorQ codec créé avec succès !");
        System.out.println("Nombre de blocs de données : " + encoder.getNumDataUnits());
        System.out.println("Nombre de blocs de parité : " + encoder.getNumParityUnits());
    }
}
```

#### Exemple Avancé avec OpenRQ
```java
import org.apache.hadoop.io.erasurecode.util.RaptorQUtil;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.ArrayDataEncoder;
import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;

public class AdvancedRaptorQExample {
    public static void main(String[] args) {
        // Données à encoder
        byte[] data = "Hello, RaptorQ World!".getBytes();
        
        // Créer les paramètres FEC
        FECParameters fecParams = RaptorQUtil.createHadoopFECParameters(
            data.length, 6, 3);
        
        // Créer l'encodeur
        ArrayDataEncoder encoder = RaptorQUtil.createEncoder(data, fecParams);
        
        // Générer des packets d'encodage
        List<EncodingPacket> packets = RaptorQUtil.encodeData(data, fecParams, 3);
        
        System.out.println("Packets générés : " + packets.size());
        
        // Simuler la perte de quelques packets
        List<EncodingPacket> availablePackets = packets.subList(0, 7); // Garder 7 packets
        
        // Créer le décodeur
        ArrayDataDecoder decoder = RaptorQUtil.createDecoder(fecParams, 0);
        
        // Décoder les données
        byte[] decodedData = RaptorQUtil.decodeData(availablePackets, fecParams, 0);
        
        System.out.println("Données décodées : " + new String(decodedData));
    }
}
```

## Tests et Validation

### 1. Tests Unitaires

```bash
# Exécuter tous les tests
mvn test

# Exécuter seulement les tests RaptorQ
mvn test -Dtest=TestRaptorQErasureCode,TestRaptorQIntegration

# Exécuter avec rapport de couverture
mvn test jacoco:report
```

### 2. Tests d'Intégration

```bash
# Test avec un cluster Hadoop local
hdfs namenode -format
start-dfs.sh
start-yarn.sh

# Créer une politique et tester
hdfs ec -addPolicy -policy TestRaptorQ -replication 4,2 -codec raptorq
hdfs ec -setPolicy -path /test -policy TestRaptorQ

# Copier des fichiers et tester la récupération
hdfs dfs -put largefile.txt /test/
hdfs dfs -ls /test/
```

### 3. Tests de Performance

```java
import org.apache.hadoop.io.erasurecode.util.RaptorQUtil;
import net.fec.openrq.parameters.FECParameters;

public class PerformanceTest {
    public static void main(String[] args) {
        // Test avec différentes tailles de données
        int[] dataSizes = {1024, 10240, 102400, 1024000}; // 1KB à 1MB
        
        for (int size : dataSizes) {
            byte[] data = new byte[size];
            // Remplir avec des données aléatoires
            
            long startTime = System.currentTimeMillis();
            
            FECParameters fecParams = RaptorQUtil.createHadoopFECParameters(
                size, 6, 3);
            List<EncodingPacket> packets = RaptorQUtil.encodeData(data, fecParams, 3);
            
            long encodeTime = System.currentTimeMillis() - startTime;
            
            startTime = System.currentTimeMillis();
            byte[] decodedData = RaptorQUtil.decodeData(packets, fecParams, 0);
            long decodeTime = System.currentTimeMillis() - startTime;
            
            System.out.printf("Taille: %d bytes, Encodage: %d ms, Décodage: %d ms%n",
                            size, encodeTime, decodeTime);
        }
    }
}
```

## Monitoring et Debugging

### 1. Logs Hadoop

```bash
# Activer les logs de debug pour RaptorQ
export HADOOP_ROOT_LOGGER=DEBUG,console

# Vérifier les logs des codes d'effacement
tail -f $HADOOP_HOME/logs/hadoop-*-namenode-*.log | grep -i erasure
```

### 2. Métriques de Performance

```bash
# Vérifier les métriques HDFS
hdfs dfsadmin -report

# Vérifier l'utilisation des codes d'effacement
hdfs ec -listPolicies
hdfs ec -getPolicy -path /data/important
```

### 3. Debugging des Problèmes

#### Problème : Codec non trouvé
```bash
# Vérifier la configuration
hdfs ec -listCodecs

# Vérifier les logs
grep -i "raptorq" $HADOOP_HOME/logs/hadoop-*-namenode-*.log
```

#### Problème : Erreur de décodage
```java
// Activer le debug verbose
ErasureCoderOptions options = new ErasureCoderOptions(6, 3, false, true);
RaptorQRawDecoder decoder = new RaptorQRawDecoder(options);
```

## Optimisations

### 1. Configuration des Performances

```xml
<!-- Optimisations pour les performances -->
<property>
    <name>io.erasurecode.codec.raptorq.symbol.size</name>
    <value>65536</value>
    <description>Taille de symbole optimale (64KB)</description>
</property>

<property>
    <name>io.erasurecode.codec.raptorq.interleaver.length</name>
    <value>1</value>
    <description>Longueur d'interleaver (pas d'interleaving)</description>
</property>
```

### 2. Optimisations Mémoire

```java
// Utiliser des ByteBuffer directs pour de meilleures performances
ByteBuffer directBuffer = ByteBuffer.allocateDirect(dataSize);

// Libérer les ressources après utilisation
encoder.release();
decoder.release();
```

### 3. Optimisations Réseau

```xml
<!-- Configuration réseau pour les codes d'effacement -->
<property>
    <name>dfs.namenode.ec.policies.replication.factor</name>
    <value>3</value>
    <description>Facteur de réplication pour les politiques EC</description>
</property>
```

## Dépannage

### 1. Problèmes Courants

#### Erreur : "Codec not configured"
```bash
# Solution : Vérifier la configuration
hdfs ec -listCodecs
# Ajouter la configuration manquante dans core-site.xml
```

#### Erreur : "Insufficient packets for decoding"
```java
// Solution : Vérifier le nombre de packets disponibles
if (availablePackets.size() < numDataUnits) {
    throw new RuntimeException("Pas assez de packets pour le décodage");
}
```

#### Erreur : "OpenRQ library not found"
```bash
# Solution : Vérifier que OpenRQ est dans le classpath
java -cp ".:OpenRQ-master/lib/*" YourClass
```

### 2. Support et Communauté

- **Documentation Hadoop** : https://hadoop.apache.org/docs/
- **Documentation OpenRQ** : https://github.com/openrq-team/OpenRQ
- **Issues GitHub** : Créer une issue pour les bugs
- **Forum Hadoop** : https://hadoop.apache.org/mailing_lists.html

## Conclusion

Cette implémentation de RaptorQ pour Hadoop offre une alternative efficace aux codes d'effacement traditionnels. Avec ses codes fountain, RaptorQ peut générer un nombre illimité de symboles de parité et récupérer les données à partir de n'importe quel sous-ensemble de symboles, offrant une flexibilité et une efficacité supérieures pour les environnements distribués.
