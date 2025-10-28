# Analyse Détaillée de l'Architecture des Codes d'Effacement Hadoop

## Vue d'ensemble

Le système de codes d'effacement (erasure codes) dans Hadoop est organisé en plusieurs couches architecturales distinctes qui permettent une séparation claire des responsabilités et une extensibilité maximale.

## Architecture en Couches

### 1. Couche Codec (codec/)
**Rôle**: Interface principale et configuration des codes d'effacement

#### Classes principales:
- **`ErasureCodec`** (classe abstraite): Interface de base pour tous les codes d'effacement
- **`XORErasureCodec`**: Implémentation XOR simple (k+1, k)
- **`HHXORErasureCodec`**: Implémentation Hitchhiker-XOR avancée
- **`DummyErasureCodec`**: Codec de test sans calcul réel
- **`RSErasureCodec`**: Codec Reed-Solomon (exclu du projet)

#### Fonctionnalités:
- Définit le schéma d'effacement (nombre de blocs de données et de parité)
- Crée les encodeurs et décodeurs appropriés
- Gère les options de configuration spécifiques au codec

### 2. Couche Coder (coder/)
**Rôle**: Préparation des données et orchestration du processus d'encodage/décodage

#### Classes principales:
- **`ErasureEncoder`** (classe abstraite): Interface pour l'encodage
- **`ErasureDecoder`** (classe abstraite): Interface pour le décodage
- **`ErasureCodingStep`**: Interface pour les étapes de codage
- **`ErasureEncodingStep`**: Étape d'encodage concrète
- **`ErasureDecodingStep`**: Étape de décodage concrète

#### Fonctionnalités:
- Prépare les blocs d'entrée et de sortie
- Calcule les étapes de codage nécessaires
- Gère la logique métier spécifique à chaque codec
- Interface entre la couche haute et les raw coders

### 3. Couche RawCoder (rawcoder/)
**Rôle**: Implémentation des algorithmes mathématiques purs

#### Classes principales:
- **`RawErasureEncoder`** (classe abstraite): Interface pour l'encodage brut
- **`RawErasureDecoder`** (classe abstraite): Interface pour le décodage brut
- **`RawErasureCoderFactory`**: Factory pour créer les raw coders
- **`XORRawEncoder/XORRawDecoder`**: Implémentation XOR pure
- **`DummyRawEncoder/DummyRawDecoder`**: Implémentation factice

#### Fonctionnalités:
- Effectue les calculs mathématiques réels (XOR, RS, etc.)
- Gère les ByteBuffer et byte[] directement
- Optimisé pour les performances
- Stateless et thread-safe

## Système de Registration

### CodecRegistry
- Utilise **ServiceLoader** pour découvrir automatiquement les factories
- Mappe les noms de codecs aux factories disponibles
- Gère les conflits de noms
- Priorise les implémentations natives

### Configuration
- Les codecs sont configurés via `CodecUtil`
- Support des codecs personnalisés via configuration
- Fallback automatique entre différentes implémentations

## Analyse des Implémentations Existantes

### 1. XOR Codec
**Caractéristiques:**
- **Simplicité**: Algorithme XOR pur
- **Limitation**: Un seul bloc de parité (k+1, k)
- **Performance**: Très rapide
- **Usage**: Principalement comme primitive pour d'autres codes

**Algorithme:**
```java
// Encodage: P = D1 ⊕ D2 ⊕ ... ⊕ Dk
// Décodage: Di = P ⊕ D1 ⊕ D2 ⊕ ... ⊕ Di-1 ⊕ Di+1 ⊕ ... ⊕ Dk
```

### 2. HHXOR (Hitchhiker-XOR)
**Caractéristiques:**
- **Complexité**: Combine RS et XOR
- **Avantage**: Réduction de 25-45% du trafic réseau
- **Recherche**: Développé à UC Berkeley
- **Usage**: Code avancé pour environnements de production

**Architecture:**
- Utilise un encodeur RS et un encodeur XOR
- Sub-packet size de 2
- Optimise la reconstruction des données

### 3. Dummy Codec
**Caractéristiques:**
- **Usage**: Tests et benchmarks
- **Performance**: Aucun calcul réel
- **Utilité**: Isolation des problèmes de performance

## Flux de Données

### Encodage:
1. **ECBlockGroup** → **ErasureEncoder** → **ErasureEncodingStep**
2. **ErasureEncodingStep** → **RawErasureEncoder** → **Calculs mathématiques**
3. Résultat: Blocs de parité générés

### Décodage:
1. **ECBlockGroup** (avec blocs effacés) → **ErasureDecoder** → **ErasureDecodingStep**
2. **ErasureDecodingStep** → **RawErasureDecoder** → **Reconstruction**
3. Résultat: Blocs effacés reconstruits

## Structures de Données Clés

### ECSchema
- Définit le schéma d'effacement (k, m)
- Nom du codec
- Options supplémentaires

### ErasureCoderOptions
- Nombre de blocs de données et de parité
- Options de performance (allowChangeInputs, allowVerboseDump)

### ECBlock/ECBlockGroup
- Représentation des blocs de données et de parité
- Gestion des états (effacé, disponible)

## Points d'Extension pour Nouveau Codec

### 1. Créer le Codec Principal
```java
public class MonCodec extends ErasureCodec {
    @Override
    public ErasureEncoder createEncoder() {
        return new MonEncodeur(getCoderOptions());
    }
    
    @Override
    public ErasureDecoder createDecoder() {
        return new MonDecodeur(getCoderOptions());
    }
}
```

### 2. Créer les Encodeurs/Décodeurs
```java
public class MonEncodeur extends ErasureEncoder {
    @Override
    protected ErasureCodingStep prepareEncodingStep(ECBlockGroup blockGroup) {
        RawErasureEncoder rawEncoder = CodecUtil.createRawEncoder(
            getConf(), "mon_codec", getOptions());
        return new ErasureEncodingStep(
            getInputBlocks(blockGroup),
            getOutputBlocks(blockGroup),
            rawEncoder);
    }
}
```

### 3. Créer les Raw Coders
```java
public class MonRawEncoder extends RawErasureEncoder {
    @Override
    protected void doEncode(ByteArrayEncodingState encodingState) {
        // Implémentation de l'algorithme mathématique
    }
    
    @Override
    protected void doEncode(ByteBufferEncodingState encodingState) {
        // Version ByteBuffer optimisée
    }
}
```

### 4. Créer la Factory
```java
public class MonRawErasureCoderFactory implements RawErasureCoderFactory {
    @Override
    public RawErasureEncoder createEncoder(ErasureCoderOptions coderOptions) {
        return new MonRawEncoder(coderOptions);
    }
    
    @Override
    public RawErasureDecoder createDecoder(ErasureCoderOptions coderOptions) {
        return new MonRawDecoder(coderOptions);
    }
    
    @Override
    public String getCoderName() {
        return "mon_codec_java";
    }
    
    @Override
    public String getCodecName() {
        return "mon_codec";
    }
}
```

### 5. Registration
- Ajouter le nom du codec dans `ErasureCodeConstants`
- Configurer dans `CodecUtil.getCodecClassName()`
- Créer le fichier META-INF/services pour ServiceLoader

## Recommandations pour Nouveau Codec

### 1. Choix d'Algorithme Simple
- **Cauchy Reed-Solomon**: Bon compromis performance/capacité
- **LDPC**: Efficace pour grandes tailles de blocs
- **RaptorQ**: Optimisé pour streaming

### 2. Considérations de Performance
- Implémenter les deux versions (ByteBuffer et byte[])
- Optimiser pour les cas courants
- Gérer la mémoire efficacement

### 3. Tests et Validation
- Tests unitaires complets
- Benchmarks de performance
- Validation mathématique

## Conclusion

L'architecture Hadoop des codes d'effacement est bien conçue avec une séparation claire des responsabilités. Elle permet l'ajout facile de nouveaux algorithmes tout en maintenant la compatibilité et les performances. Le système de registration automatique via ServiceLoader simplifie grandement l'intégration de nouveaux codecs.

## Points d'Intégration Concrets dans ce dépôt

- `erasurecode/ErasureCodeConstants.java`
  - Ajouter un nom de codec (ex: `raptorq`) et des schémas pratiques (k,m) pour tests.
- `erasurecode/CodecUtil.java`
  - Ajouter le mapping du nom de codec vers la classe `ErasureCodec` concrète (clé `io.erasurecode.codec.raptorq`).
  - Permettre la configuration des rawcoders via `io.erasurecode.codec.raptorq.rawcoders`.
- `erasurecode/codec/` (nouvelle classe)
  - `RaptorQErasureCodec` étend `ErasureCodec` et crée `RaptorQEncoder` / `RaptorQDecoder`.
- `erasurecode/coder/` (nouvelles classes)
  - `RaptorQEncoder` et `RaptorQDecoder` étendent respectivement `ErasureEncoder` / `ErasureDecoder` et délèguent aux rawcoders.
- `erasurecode/rawcoder/` (nouvelles classes)
  - `RaptorQRawEncoder` / `RaptorQRawDecoder` utilisent OpenRQ pour encoder/décoder des symboles à partir de ByteBuffer/byte[].
  - `RaptorQRawErasureCoderFactory` pour ServiceLoader.
- `erasurecode/META-INF/services/org.apache.hadoop.io.erasurecode.rawcoder.RawErasureCoderFactory`
  - Ajouter `org.apache.hadoop.io.erasurecode.rawcoder.RaptorQRawErasureCoderFactory`.

## Différences conceptuelles RS vs RaptorQ à considérer

- **Système de symboles**: RS travaille sur k données fixes et m parités fixes; RaptorQ génère un nombre arbitraire de symboles de réparation. Pour Hadoop (qui attend m parités fixes), on configurera m symboles de réparation déterministes (m premiers ESIs) lors de l’encodage.
- **Décodage**: Hadoop fournit une liste d’unités effacées à reconstruire; RaptorQ requiert ≥ k symboles quelconques pour résoudre le système. Nous fournirons au décodeur un ensemble minimal de symboles disponibles (k) + reconstruirons précisément les indices effacés.
- **Taille d’unité**: Harmoniser la taille de chunk Hadoop avec la `symbolSize` OpenRQ. Ajouter un padding si nécessaire et retirer le padding après décodage.
- **États & buffers**: Supporter `byte[]` et `ByteBuffer` dans `RawErasure{En,De}coder` pour performance.

## Implémentation RaptorQ ajoutée dans ce projet

- `erasurecode/ErasureCodeConstants.java`
  - Ajout du codec `raptorq` et de schémas d’exemple (6,3) et (10,4).
- `erasurecode/CodecUtil.java`
  - Résolution de `io.erasurecode.codec.raptorq` vers `codec.RaptorQErasureCodec`.
- `erasurecode/codec/RaptorQErasureCodec.java`
  - Crée `coder.RaptorQEncoder` / `coder.RaptorQDecoder`.
- `erasurecode/coder/RaptorQEncoder.java` / `RaptorQDecoder.java`
  - Adaptateurs qui délèguent vers les raw coders via `CodecUtil.createRawEncoder/Decoder`.
- `erasurecode/rawcoder/RaptorQRawErasureCoderFactory.java`
  - ServiceLoader: `coderName=raptorq_java`, `codecName=raptorq`.
- `erasurecode/rawcoder/RaptorQRawEncoder.java`
  - Concatène k inputs (taille T), `FECParameters(F=k*T, T, Z=1)`, génère m symboles de réparation ESI `K..K+m-1` et les écrit dans les sorties.
- `erasurecode/rawcoder/RaptorQRawDecoder.java`
  - Alimente OpenRQ avec tous les symboles disponibles; si décodé, copie les données reconstruites sur les sorties data; parités effacées régénérées en re‑encodant.
- `erasurecode/META-INF/services/...RawErasureCoderFactory`
  - Enregistrement de la factory RaptorQ.

### Points d’attention / risques
- Hypothèse de taille égale T pour toutes les unités d’un groupe. Si non vrai, prévoir un padding explicite.
- RaptorQ peut échouer à N=K (probabiliste). Gestion d’erreur incluse côté raw decoder (IOException si échec) ; amélioration possible: marge d’overhead configurable.
- La régénération de parité en décodage se fait par re‑encodage des données reconstruites pour ESI `K+index`.
- Mémoire: implémentation actuelle fait une concaténation `k*T`; possible optimisation par streaming.