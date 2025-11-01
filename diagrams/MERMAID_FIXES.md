# Corrections appliquées aux diagrammes Mermaid

## Problèmes identifiés et résolus

### 1. Crochets dans les noms de sous-graphes
**Fichiers affectés** : `test_scenarios.md`

**Problème** : Les crochets `[]` dans `byte[]` étaient interprétés comme syntaxe Mermaid
```
subgraph ByteArray[byte[] API Tests]  ❌
```

**Solution** : Mettre le nom entre guillemets
```
subgraph ByteArray["byte[] API Tests"]  ✅
```

**Lignes corrigées** :
- Ligne 8 : `subgraph ByteArray["byte[] API Tests"]`
- Ligne 16 : `subgraph ByteBuffer["ByteBuffer API Tests"]`

---

### 2. Crochets dans les messages de séquence
**Fichiers affectés** : `raptorq_encoding.md`

**Problème** : `parity[i]` dans le message causait une erreur de parsing
```
RawEnc->>Par: Write T bytes to parity[i]  ❌
```

**Solution** : Mettre le message entre guillemets
```
RawEnc->>Par: "Write T bytes to parity[i]"  ✅
```

**Ligne corrigée** :
- Ligne 25

---

### 3. Symboles Unicode pour multiplication
**Fichiers affectés** : `raptorq_encoding.md`, `raptorq_decoding.md`

**Problème** : Le symbole `×` (multiplication) n'est pas toujours bien supporté
```
k × T bytes  ❌
```

**Solution** : Utiliser `*` à la place
```
k*T bytes  ✅
```

**Lignes corrigées** :
- `raptorq_encoding.md` lignes 16, 18
- `raptorq_decoding.md` ligne 18

---

### 4. Symbole epsilon grec
**Fichiers affectés** : `comparison_table.md`

**Problème** : Le caractère `ε` peut causer des problèmes de rendu
```
k+ε symbols  ❌
```

**Solution** : Remplacer par le mot complet
```
k+epsilon symbols  ✅
```

**Ligne corrigée** :
- Ligne 17

---

### 5. Checkmark Unicode
**Fichiers affectés** : `test_scenarios.md`

**Problème** : Le symbole ✓ peut ne pas être rendu correctement
```
All Tests Pass ✓  ❌
```

**Solution** : Mettre le noeud entre guillemets
```
Validation --> Pass["All Tests Pass ✓"]  ✅
```

**Ligne corrigée** :
- Ligne 50

---

## Règles générales pour Mermaid

Pour éviter les problèmes de parsing dans les diagrammes Mermaid :

1. **Texte avec caractères spéciaux** : Toujours mettre entre guillemets `"..."` quand le texte contient :
   - Crochets `[]`
   - Parenthèses `()`
   - Symboles mathématiques complexes
   - Caractères Unicode

2. **Multiplication** : Utiliser `*` plutôt que `×`

3. **Symboles grecs** : Utiliser le mot complet (epsilon, alpha, beta, etc.)

4. **Guillemets** : Si vous devez inclure des guillemets dans le texte, échappez-les : `\"`

---

## Fichiers corrigés

✅ `diagrams/test_scenarios.md` - 4 corrections  
✅ `diagrams/raptorq_encoding.md` - 3 corrections  
✅ `diagrams/raptorq_decoding.md` - 1 correction  
✅ `diagrams/comparison_table.md` - 1 correction  
✅ `diagrams/hadoop_architecture.md` - Aucune correction nécessaire  
✅ `diagrams/esi_mapping.md` - Aucune correction nécessaire  

---

## Test des diagrammes

Pour vérifier que les diagrammes sont valides, utilisez :
- [Mermaid Live Editor](https://mermaid.live) - Collez le code et vérifiez le rendu
- Mermaid CLI : `mmdc -i fichier.md -o fichier.png`
- VS Code avec extension "Markdown Preview Mermaid Support"

## Notes de rendu

Les diagrammes devraient maintenant se rendre correctement dans :
- GitHub (markdown preview)
- Mermaid Live Editor
- VS Code preview
- Documentation générée automatiquement
- LaTeX via images PNG

