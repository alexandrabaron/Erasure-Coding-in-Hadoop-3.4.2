# RaptorQ Erasure Code pour Hadoop

Ce projet implémente un code d'effacement RaptorQ pour Hadoop en utilisant la bibliothèque OpenRQ.

## À propos de RaptorQ

RaptorQ est un code fountain (fountain code) qui peut générer un nombre illimité de symboles de parité et récupérer les données à partir de n'importe quel sous-ensemble de symboles. Il est particulièrement efficace pour les systèmes de stockage distribués comme Hadoop.

## Caractéristiques

- **Codes Fountain** : Génère un nombre illimité de symboles de parité
- **Efficacité** : Très efficace pour la reconstruction partielle des données
- **Flexibilité** : Peut récupérer les données avec n'importe quel sous-ensemble de symboles
- **Performance** : Optimisé pour les environnements distribués

## Installation

### Prérequis

- Java 8 ou supérieur
- Maven 3.6 ou supérieur
- Hadoop 3.3.0 ou supérieur

### Compilation

```bash
mvn clean compile
```

### Tests

```bash
mvn test
```

### Installation

```bash
mvn clean install
```

## Configuration

### 1. Ajouter la dépendance OpenRQ

Ajoutez la dépendance OpenRQ à votre projet :

```xml
<dependency>
    <groupId>net.fec.openrq</groupId>
    <artifactId>openrq</artifactId>
    <version>3.3.2</version>
</dependency>
```

### 2. Configuration Hadoop

Ajoutez les propriétés suivantes à votre `core-site.xml` :

```xml
<property>
    <name>io.erasurecode.codec.raptorq</name>
    <value>org.apache.hadoop.io.erasurecode.codec.RaptorQErasureCodec</value>
</property>

<property>
    <name>io.erasurecode.codec.raptorq.rawcoders</name>
    <value>raptorq_java</value>
</property>
```

### 3. Créer une politique d'effacement

```bash
hdfs ec -addPolicy -policy RaptorQPolicy -replication 6,3 -codec raptorq
```

## Utilisation

### Exemple de code

```java
import org.apache.hadoop.io.erasurecode.ErasureCodecOptions;
import org.apache.hadoop.io.erasurecode.ECSchema;
import org.apache.hadoop.io.erasurecode.codec.RaptorQErasureCodec;
import org.apache.hadoop.io.erasurecode.coder.ErasureEncoder;
import org.apache.hadoop.io.erasurecode.coder.ErasureDecoder;

// Créer le schéma RaptorQ (6 blocs de données, 3 blocs de parité)
ECSchema schema = new ECSchema("raptorq", 6, 3);
ErasureCodecOptions options = new ErasureCodecOptions(schema);

// Créer le codec
RaptorQErasureCodec codec = new RaptorQErasureCodec(conf, options);

// Créer l'encodeur et le décodeur
ErasureEncoder encoder = codec.createEncoder();
ErasureDecoder decoder = codec.createDecoder();
```

## Architecture

Le codec RaptorQ suit l'architecture Hadoop standard :

1. **RaptorQErasureCodec** : Interface principale du codec
2. **RaptorQErasureEncoder/RaptorQErasureDecoder** : Couche de préparation des données
3. **RaptorQRawEncoder/RaptorQRawDecoder** : Implémentation des algorithmes mathématiques
4. **RaptorQRawErasureCoderFactory** : Factory pour créer les raw coders

## Intégration OpenRQ

L'implémentation actuelle utilise une approche XOR simplifiée pour la démonstration. Pour une implémentation complète avec OpenRQ :

1. Intégrer la bibliothèque OpenRQ dans `RaptorQUtil`
2. Implémenter la conversion entre formats Hadoop et OpenRQ
3. Utiliser les encodeurs/décodeurs OpenRQ dans les raw coders

## Tests

Les tests unitaires couvrent :

- Création des codecs, encodeurs et décodeurs
- Validation des constantes et configurations
- Tests de base des raw coders

```bash
mvn test -Dtest=TestRaptorQErasureCode
```

## Limitations actuelles

- L'implémentation utilise actuellement XOR au lieu de RaptorQ pur
- L'intégration OpenRQ complète nécessite un développement supplémentaire
- Les tests de performance ne sont pas encore implémentés

## Développement futur

- [ ] Intégration complète avec OpenRQ
- [ ] Tests de performance
- [ ] Optimisations pour les grandes tailles de blocs
- [ ] Support des configurations avancées RaptorQ

## Contribution

Les contributions sont les bienvenues ! Veuillez :

1. Fork le projet
2. Créer une branche pour votre fonctionnalité
3. Ajouter des tests
4. Soumettre une pull request

## Licence

Ce projet est sous licence Apache 2.0, comme Hadoop.
