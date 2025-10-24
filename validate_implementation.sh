#!/bin/bash

# Script de validation de l'implémentation RaptorQ
# Ce script vérifie que tous les composants sont correctement implémentés

echo "=== Validation de l'implémentation RaptorQ ==="
echo

# Vérifier la compilation
echo "1. Vérification de la compilation..."
if mvn compile -q; then
    echo "   ✅ Compilation réussie"
else
    echo "   ❌ Erreur de compilation"
    exit 1
fi

# Vérifier les tests unitaires
echo "2. Exécution des tests unitaires..."
if mvn test -q; then
    echo "   ✅ Tests unitaires passés"
else
    echo "   ❌ Échec des tests unitaires"
    exit 1
fi

# Vérifier la structure des fichiers
echo "3. Vérification de la structure des fichiers..."

# Vérifier les fichiers principaux
files=(
    "codec/RaptorQErasureCodec.java"
    "coder/RaptorQErasureEncoder.java"
    "coder/RaptorQErasureDecoder.java"
    "rawcoder/RaptorQRawEncoder.java"
    "rawcoder/RaptorQRawDecoder.java"
    "rawcoder/RaptorQRawErasureCoderFactory.java"
    "util/RaptorQUtil.java"
    "META-INF/services/org.apache.hadoop.io.erasurecode.rawcoder.RawErasureCoderFactory"
    "pom.xml"
    "TestRaptorQErasureCode.java"
    "TestRaptorQIntegration.java"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $file"
    else
        echo "   ❌ $file manquant"
        exit 1
    fi
done

# Vérifier les constantes
echo "4. Vérification des constantes..."
if grep -q "RAPTORQ_CODEC_NAME" ErasureCodeConstants.java; then
    echo "   ✅ Constantes RaptorQ définies"
else
    echo "   ❌ Constantes RaptorQ manquantes"
    exit 1
fi

# Vérifier l'intégration CodecUtil
echo "5. Vérification de l'intégration CodecUtil..."
if grep -q "RAPTORQ_CODEC_NAME" CodecUtil.java; then
    echo "   ✅ Intégration CodecUtil correcte"
else
    echo "   ❌ Intégration CodecUtil manquante"
    exit 1
fi

# Vérifier les dépendances Maven
echo "6. Vérification des dépendances Maven..."
if grep -q "openrq" pom.xml; then
    echo "   ✅ Dépendance OpenRQ présente"
else
    echo "   ❌ Dépendance OpenRQ manquante"
    exit 1
fi

echo
echo "=== Résumé de la validation ==="
echo "✅ Compilation : OK"
echo "✅ Tests unitaires : OK"
echo "✅ Structure des fichiers : OK"
echo "✅ Constantes : OK"
echo "✅ Intégration : OK"
echo "✅ Dépendances : OK"
echo
echo "🎉 L'implémentation RaptorQ est validée avec succès !"
echo
echo "Prochaines étapes recommandées :"
echo "1. Tests de performance"
echo "2. Intégration HDFS"
echo "3. Tests de charge"
echo "4. Optimisations de production"
