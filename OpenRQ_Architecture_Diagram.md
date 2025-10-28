## OpenRQ (RaptorQ) – Architecture et Usage

### Concepts clés
- **RFC 6330 (RaptorQ)**: Code fountain systématique; peut générer un nombre illimité de symboles de réparation.
- **Source block / sub-block**: OpenRQ partitionne les données en blocs; interleaving de sous-blocs non implémenté (max 1).
- **Symboles**: Chaque bloc est découpé en symboles de taille fixe `symbolSize`.
- **FEC parameters**: Encapsule `dataLength`, `symbolSize`, `symbolsPerBlock`.

### API principale (fichiers clés)
- `net.fec.openrq.OpenRQ`: Point d’entrée; création d’encodeurs/décodeurs via `FECParameters`.
- `parameters.FECParameters`: Paramètres de FEC (taille des données, taille symbole, nb symboles par bloc).
- `encoder.DataEncoder` / `encoder.SourceBlockEncoder`: Encodage; produit des `EncodingPacket` (source ou réparation).
- `decoder.DataDecoder` / `decoder.SourceBlockDecoder`: Décodage; consomme des `EncodingPacket` en ordre quelconque; restitue les octets du bloc.
- `EncodingPacket` / `SerializablePacket`: Paquets transmissibles.

### Encodage (schéma d’utilisation typique)
1. Construire `FECParameters` avec `dataLength`, `symbolSize`, `symbolsPerBlock`.
2. Créer `DataEncoder` via `OpenRQ.newEncoder(data, fecParams)`.
3. Pour chaque `SourceBlockEncoder`:
   - Émettre `k` paquets source et au besoin `m` paquets de réparation déterministes (ex: ESI = 0..m-1).

### Décodage (schéma d’utilisation typique)
1. Créer `DataDecoder` via `OpenRQ.newDecoder(fecParams, dataLen)`.
2. Remettre les paquets reçus (source et/ou réparation) au `SourceBlockDecoder`.
3. Quand le nombre de symboles indépendants ≥ k, reconstruire le bloc et obtenir les octets.

### Intégration Hadoop (adaptation)
- Hadoop attend k données + m parités fixes. Avec OpenRQ, on choisit les m premiers symboles de réparation pour former les m « parités ».
- À l’encodage Hadoop, `RaptorQRawEncoder` génère ces m symboles de réparation pour chaque groupe de k chunks.
- Au décodage Hadoop, `RaptorQRawDecoder` collecte un ensemble de ≥ k symboles disponibles (données+réparation) et reconstruit les chunks manquants.
- Harmoniser `chunkSize` Hadoop et `symbolSize` OpenRQ; si besoin, padding des derniers symboles et dépadding après reconstruction.

### Limitations / Attention
- Pas d’interleaving (sub-blocks > 1) dans OpenRQ.
- Performance de décodage plus lente que certaines implémentations propriétaires (cf. README OpenRQ).
- Gestion mémoire: privilégier ByteBuffer directs côté Hadoop quand possible; sinon basculer vers byte[].

### Références
- RFC 6330 — RaptorQ Fountain Code
- Code: `OpenRQ-master/src/main/net/fec/openrq/*`


