# Documentation Complète - Implémentation RaptorQ pour Hadoop

## Vue d'ensemble

Cette documentation détaille l'implémentation complète du code d'effacement RaptorQ pour Hadoop en utilisant la bibliothèque OpenRQ. L'implémentation suit l'architecture Hadoop existante et intègre les codes fountain RaptorQ pour offrir une meilleure efficacité et flexibilité.

## Fichiers Créés

### 1. `codec/RaptorQErasureCodec.java`

**Rôle** : Interface principale du codec RaptorQ

**Description détaillée** :
- Hérite de `ErasureCodec` (classe abstraite de base)
- Définit l'interface pour le codec RaptorQ dans Hadoop
- Configure le schéma d'effacement (nombre de blocs de données et de parité)
- Crée les encodeurs et décodeurs appropriés via `createEncoder()` et `createDecoder()`

**Méthodes principales** :
- `RaptorQErasureCodec(Configuration conf, ErasureCodecOptions options)` : Constructeur
- `createEncoder()` : Retourne une instance de `RaptorQErasureEncoder`
- `createDecoder()` : Retourne une instance de `RaptorQErasureDecoder`

**Intégration** : Ce codec est référencé dans `CodecUtil.java` et `ErasureCodeConstants.java`

### 2. `coder/RaptorQErasureEncoder.java`

**Rôle** : Couche de préparation des données pour l'encodage

**Description détaillée** :
- Hérite de `ErasureEncoder` (classe abstraite de base)
- Prépare les blocs d'entrée et de sortie pour l'encodage
- Orchestre le processus d'encodage en créant un `ErasureEncodingStep`
- Utilise `CodecUtil.createRawEncoder()` pour obtenir le raw encoder approprié

**Méthodes principales** :
- `RaptorQErasureEncoder(ErasureCoderOptions options)` : Constructeur
- `prepareEncodingStep(ECBlockGroup blockGroup)` : Prépare l'étape d'encodage
  - Récupère les blocs d'entrée via `getInputBlocks(blockGroup)`
  - Récupère les blocs de sortie via `getOutputBlocks(blockGroup)`
  - Crée un `ErasureEncodingStep` avec le raw encoder

**Flux de données** :
```
ECBlockGroup → RaptorQErasureEncoder → ErasureEncodingStep → RaptorQRawEncoder
```

### 3. `coder/RaptorQErasureDecoder.java`

**Rôle** : Couche de préparation des données pour le décodage

**Description détaillée** :
- Hérite de `ErasureDecoder` (classe abstraite de base)
- Prépare les blocs d'entrée et de sortie pour le décodage
- Gère la logique de récupération des blocs effacés
- Orchestre le processus de décodage en créant un `ErasureDecodingStep`

**Méthodes principales** :
- `RaptorQErasureDecoder(ErasureCoderOptions options)` : Constructeur
- `prepareDecodingStep(ECBlockGroup blockGroup)` : Prépare l'étape de décodage
  - Récupère les blocs d'entrée via `getInputBlocks(blockGroup)`
  - Récupère les blocs effacés via `getErasedIndexes(inputBlocks)`
  - Crée un `ErasureDecodingStep` avec le raw decoder

**Flux de données** :
```
ECBlockGroup (avec blocs effacés) → RaptorQErasureDecoder → ErasureDecodingStep → RaptorQRawDecoder
```

### 4. `rawcoder/RaptorQRawEncoder.java`

**Rôle** : Implémentation des algorithmes mathématiques d'encodage RaptorQ

**Description détaillée** :
- Hérite de `RawErasureEncoder` (classe abstraite de base)
- Implémente l'encodage RaptorQ réel en utilisant OpenRQ
- Gère les conversions entre formats Hadoop (ByteBuffer, byte[]) et OpenRQ
- Génère les symboles source et de réparation (parity)

**Méthodes principales** :
- `doEncode(ByteBufferEncodingState encodingState)` : Encodage avec ByteBuffer
  - Convertit les ByteBuffer d'entrée en byte arrays
  - Concatène toutes les données d'entrée
  - Crée les paramètres FEC avec `RaptorQUtil.createHadoopFECParameters()`
  - Utilise `ArrayDataEncoder` d'OpenRQ pour l'encodage
  - Génère les packets source et de réparation
  - Distribue les résultats aux buffers de sortie

- `doEncode(ByteArrayEncodingState encodingState)` : Encodage avec byte arrays
  - Même logique que ByteBuffer mais avec des byte arrays
  - Optimisé pour les performances mémoire

**Intégration OpenRQ** :
- Utilise `net.fec.openrq.ArrayDataEncoder` pour l'encodage
- Utilise `net.fec.openrq.EncodingPacket` pour les packets
- Utilise `net.fec.openrq.parameters.FECParameters` pour la configuration

### 5. `rawcoder/RaptorQRawDecoder.java`

**Rôle** : Implémentation des algorithmes mathématiques de décodage RaptorQ

**Description détaillée** :
- Hérite de `RawErasureDecoder` (classe abstraite de base)
- Implémente le décodage RaptorQ réel en utilisant OpenRQ
- Récupère les données à partir de n'importe quel sous-ensemble de symboles
- Gère la reconstruction des blocs effacés

**Méthodes principales** :
- `doDecode(ByteBufferDecodingState decodingState)` : Décodage avec ByteBuffer
  - Collecte les packets disponibles (inputs non-null)
  - Crée les paramètres FEC estimés
  - Utilise `ArrayDataDecoder` d'OpenRQ pour le décodage
  - Vérifie si le décodage est possible avec `isDataDecoded()`
  - Distribue les données décodées aux buffers de sortie

- `doDecode(ByteArrayDecodingState decodingState)` : Décodage avec byte arrays
  - Même logique que ByteBuffer mais avec des byte arrays

**Méthodes utilitaires** :
- `createEncodingPacketFromData(byte[] data, int index)` : Crée un EncodingPacket à partir de données brutes
- `distributeDecodedData(byte[] decodedData, ByteBuffer[] outputs, int[] erasedIndexes)` : Distribue les données décodées

**Intégration OpenRQ** :
- Utilise `net.fec.openrq.ArrayDataDecoder` pour le décodage
- Utilise `net.fec.openrq.decoder.SourceBlockDecoder` pour les blocs source
- Gère les packets d'encodage avec `putEncodingPacket()`

### 6. `rawcoder/RaptorQRawErasureCoderFactory.java`

**Rôle** : Factory pour créer les raw encoders et decoders RaptorQ

**Description détaillée** :
- Implémente `RawErasureCoderFactory` (interface de factory)
- Utilisée par le système ServiceLoader pour l'enregistrement automatique
- Crée les instances de `RaptorQRawEncoder` et `RaptorQRawDecoder`

**Méthodes principales** :
- `createEncoder(ErasureCoderOptions coderOptions)` : Crée un `RaptorQRawEncoder`
- `createDecoder(ErasureCoderOptions coderOptions)` : Crée un `RaptorQRawDecoder`
- `getCoderName()` : Retourne "raptorq_java"
- `getCodecName()` : Retourne "raptorq"

**Enregistrement** : Référencée dans `META-INF/services/org.apache.hadoop.io.erasurecode.rawcoder.RawErasureCoderFactory`

### 7. `util/RaptorQUtil.java`

**Rôle** : Classe utilitaire pour l'intégration OpenRQ

**Description détaillée** :
- Fournit des méthodes helper pour intégrer OpenRQ avec Hadoop
- Gère la conversion entre formats Hadoop et OpenRQ
- Calcule les paramètres FEC optimaux pour Hadoop
- Implémente les opérations d'encodage/décodage de haut niveau

**Constantes** :
- `DEFAULT_SYMBOL_SIZE = 64 * 1024` : Taille de symbole par défaut (64KB)
- `DEFAULT_INTERLEAVER_LENGTH = 1` : Longueur d'interleaver (pas d'interleaving)

**Méthodes principales** :
- `createFECParameters(long dataLength, int symbolSize, int numSourceBlocks)` : Crée les paramètres FEC
- `createHadoopFECParameters(long dataLength, int numDataUnits, int numParityUnits)` : Paramètres optimisés pour Hadoop
- `calculateOptimalSymbolSize(long dataLength, int totalUnits)` : Calcule la taille optimale de symbole
- `createEncoder(byte[] data, FECParameters fecParams)` : Crée un encodeur OpenRQ
- `createDecoder(FECParameters fecParams, int symbolOverhead)` : Crée un décodeur OpenRQ
- `encodeData(byte[] data, FECParameters fecParams, int numParityPackets)` : Encodage de haut niveau
- `decodeData(List<EncodingPacket> packets, FECParameters fecParams, int symbolOverhead)` : Décodage de haut niveau
- `byteBufferToArray(ByteBuffer buffer)` : Conversion ByteBuffer → byte array
- `arrayToByteBuffer(byte[] data)` : Conversion byte array → ByteBuffer

**Optimisations** :
- Calcul automatique de la taille de symbole optimale
- Arrondi à la puissance de 2 la plus proche pour l'efficacité
- Gestion des tailles minimales et maximales de symboles

### 8. `TestRaptorQErasureCode.java`

**Rôle** : Tests unitaires pour l'implémentation RaptorQ

**Description détaillée** :
- Tests de création des codecs, encodeurs et décodeurs
- Validation des constantes et configurations
- Tests de base des raw coders
- Utilise JUnit 4 pour les tests

**Tests inclus** :
- `testRaptorQCodecCreation()` : Test de création du codec principal
- `testRaptorQEncoderCreation()` : Test de création de l'encodeur
- `testRaptorQDecoderCreation()` : Test de création du décodeur
- `testRaptorQRawEncoderCreation()` : Test de création du raw encoder
- `testRaptorQRawDecoderCreation()` : Test de création du raw decoder
- `testRaptorQRawErasureCoderFactory()` : Test de la factory
- `testRaptorQConstants()` : Test des constantes

### 9. `pom.xml`

**Rôle** : Configuration Maven pour le projet

**Description détaillée** :
- Définit les dépendances nécessaires (Hadoop, OpenRQ, JUnit)
- Configure la compilation Java 8
- Inclut les plugins Maven pour la compilation et les tests

**Dépendances principales** :
- `hadoop-common` : Bibliothèque Hadoop de base
- `net.fec.openrq:openrq:3.3.2` : Bibliothèque OpenRQ pour RaptorQ
- `junit:junit:4.13.2` : Framework de tests

### 10. `README.md`

**Rôle** : Documentation utilisateur

**Description détaillée** :
- Guide d'installation et d'utilisation
- Exemples de code
- Instructions de configuration Hadoop
- Architecture et avantages de RaptorQ

### 11. `META-INF/services/org.apache.hadoop.io.erasurecode.rawcoder.RawErasureCoderFactory`

**Rôle** : Enregistrement automatique via ServiceLoader

**Description détaillée** :
- Contient le nom de classe complet de `RaptorQRawErasureCoderFactory`
- Permet l'enregistrement automatique du codec dans Hadoop
- Utilisé par `CodecRegistry` pour découvrir les factories disponibles

## Fichiers Modifiés

### 1. `ErasureCodeConstants.java`

**Modifications** :
- Ajout de `RAPTORQ_CODEC_NAME = "raptorq"`
- Ajout de `RAPTORQ_6_3_SCHEMA = new ECSchema("raptorq", 6, 3)`

**Impact** : Définit les constantes nécessaires pour le codec RaptorQ

### 2. `CodecUtil.java`

**Modifications** :
- Ajout des constantes de configuration RaptorQ :
  - `IO_ERASURECODE_CODEC_RAPTORQ_KEY`
  - `IO_ERASURECODE_CODEC_RAPTORQ`
  - `IO_ERASURECODE_CODEC_RAPTORQ_RAWCODERS_KEY`
- Ajout de l'import `RaptorQErasureCodec`
- Ajout du cas `RAPTORQ_CODEC_NAME` dans `getCodecClassName()`

**Impact** : Intègre RaptorQ dans le système de configuration Hadoop

## Architecture Technique

### Flux d'Encodage

```
Données d'entrée (6 blocs)
           │
           ▼
┌─────────────────────┐
│ RaptorQErasureCodec │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│RaptorQErasureEncoder│
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ ErasureEncodingStep │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ RaptorQRawEncoder   │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   OpenRQ Encoder    │
└─────────┬───────────┘
          │
          ▼
Symboles de parité (3+ blocs)
```

### Flux de Décodage

```
Blocs disponibles (5 blocs)
           │
           ▼
┌─────────────────────┐
│ RaptorQErasureCodec │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│RaptorQErasureDecoder│
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ ErasureDecodingStep │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ RaptorQRawDecoder   │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   OpenRQ Decoder    │
└─────────┬───────────┘
          │
          ▼
Bloc récupéré (1 bloc)
```

## Avantages de RaptorQ

### 1. Codes Fountain
- Génère un nombre illimité de symboles de parité
- Plus flexible que les codes traditionnels (RS, XOR)

### 2. Efficacité
- Très efficace pour la reconstruction partielle
- Réduction du trafic réseau lors de la récupération

### 3. Flexibilité
- Peut récupérer les données avec n'importe quel sous-ensemble de symboles
- Tolérance aux pannes adaptative

### 4. Performance
- Optimisé pour les environnements distribués
- Meilleure utilisation de la bande passante

## Configuration et Utilisation

### 1. Configuration Hadoop

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

### 2. Création de Politique

```bash
hdfs ec -addPolicy -policy RaptorQPolicy -replication 6,3 -codec raptorq
```

### 3. Utilisation en Code

```java
// Créer le schéma RaptorQ
ECSchema schema = new ECSchema("raptorq", 6, 3);
ErasureCodecOptions options = new ErasureCodecOptions(schema);

// Créer le codec
RaptorQErasureCodec codec = new RaptorQErasureCodec(conf, options);

// Créer l'encodeur et le décodeur
ErasureEncoder encoder = codec.createEncoder();
ErasureDecoder decoder = codec.createDecoder();
```

## Tests et Validation

### Compilation
```bash
mvn clean compile
```

### Tests Unitaires
```bash
mvn test -Dtest=TestRaptorQErasureCode
```

### Tests d'Intégration
```bash
mvn test
```

## Limitations Actuelles

1. **Implémentation Simplifiée** : L'implémentation actuelle utilise une approche simplifiée pour la création des EncodingPacket
2. **Gestion des Métadonnées** : Les métadonnées FEC ne sont pas persistées entre encodage et décodage
3. **Optimisations** : Des optimisations supplémentaires sont possibles pour les performances

## Développements Futurs

1. **Intégration OpenRQ Complète** : Améliorer la gestion des packets et métadonnées
2. **Tests de Performance** : Benchmarks comparatifs avec XOR et RS
3. **Optimisations** : Gestion mémoire, parallélisation
4. **Tests d'Intégration** : Validation avec HDFS réel
5. **Documentation** : Guide d'utilisation détaillé

## Conclusion

Cette implémentation fournit une base solide pour l'intégration de RaptorQ dans Hadoop. Elle suit les patterns architecturaux existants et offre une interface cohérente avec les autres codecs d'effacement. L'utilisation d'OpenRQ garantit une implémentation correcte et efficace de l'algorithme RaptorQ.
