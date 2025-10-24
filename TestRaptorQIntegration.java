/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.io.erasurecode;

import org.apache.hadoop.io.erasurecode.codec.RaptorQErasureCodec;
import org.apache.hadoop.io.erasurecode.coder.RaptorQErasureDecoder;
import org.apache.hadoop.io.erasurecode.coder.RaptorQErasureEncoder;
import org.apache.hadoop.io.erasurecode.rawcoder.RaptorQRawDecoder;
import org.apache.hadoop.io.erasurecode.rawcoder.RaptorQRawEncoder;
import org.apache.hadoop.io.erasurecode.rawcoder.RaptorQRawErasureCoderFactory;
import org.apache.hadoop.io.erasurecode.util.RaptorQUtil;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;
import static org.junit.Assert.*;

import net.fec.openrq.parameters.FECParameters;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Tests d'intégration avancés pour l'implémentation RaptorQ.
 */
public class TestRaptorQIntegration {

  @Test
  public void testRaptorQUtilFECParameters() {
    // Test de création des paramètres FEC
    long dataLength = 1024 * 1024; // 1MB
    int numDataUnits = 6;
    int numParityUnits = 3;
    
    FECParameters fecParams = RaptorQUtil.createHadoopFECParameters(
        dataLength, numDataUnits, numParityUnits);
    
    assertNotNull("FEC parameters should not be null", fecParams);
    assertEquals("Data length should match", dataLength, fecParams.dataLength());
    assertTrue("Symbol size should be positive", fecParams.symbolSize() > 0);
    assertEquals("Number of source blocks should be 1", 1, fecParams.numberOfSourceBlocks());
  }

  @Test
  public void testRaptorQUtilSymbolSizeCalculation() {
    // Test du calcul de la taille de symbole optimale
    long dataLength = 1024 * 1024; // 1MB
    int totalUnits = 9; // 6 data + 3 parity
    
    FECParameters fecParams = RaptorQUtil.createHadoopFECParameters(
        dataLength, 6, 3);
    
    int symbolSize = fecParams.symbolSize();
    assertTrue("Symbol size should be reasonable", symbolSize >= 1024);
    assertTrue("Symbol size should not be too large", symbolSize <= 64 * 1024);
    
    // Vérifier que la taille est une puissance de 2
    assertTrue("Symbol size should be power of 2", isPowerOfTwo(symbolSize));
  }

  @Test
  public void testRaptorQRawEncoderBasicFunctionality() {
    ErasureCoderOptions options = new ErasureCoderOptions(6, 3);
    RaptorQRawEncoder encoder = new RaptorQRawEncoder(options);
    
    assertEquals("Number of data units should match", 6, encoder.getNumDataUnits());
    assertEquals("Number of parity units should match", 3, encoder.getNumParityUnits());
    assertEquals("Total units should match", 9, encoder.getNumAllUnits());
  }

  @Test
  public void testRaptorQRawDecoderBasicFunctionality() {
    ErasureCoderOptions options = new ErasureCoderOptions(6, 3);
    RaptorQRawDecoder decoder = new RaptorQRawDecoder(options);
    
    assertEquals("Number of data units should match", 6, decoder.getNumDataUnits());
    assertEquals("Number of parity units should match", 3, decoder.getNumParityUnits());
    assertEquals("Total units should match", 9, decoder.getNumAllUnits());
  }

  @Test
  public void testRaptorQFactoryCreation() {
    RaptorQRawErasureCoderFactory factory = new RaptorQRawErasureCoderFactory();
    
    assertEquals("Coder name should be correct", "raptorq_java", factory.getCoderName());
    assertEquals("Codec name should be correct", "raptorq", factory.getCodecName());
    
    ErasureCoderOptions options = new ErasureCoderOptions(6, 3);
    assertNotNull("Encoder should be created", factory.createEncoder(options));
    assertNotNull("Decoder should be created", factory.createDecoder(options));
  }

  @Test
  public void testRaptorQCodecIntegration() {
    Configuration conf = new Configuration();
    ECSchema schema = new ECSchema(ErasureCodeConstants.RAPTORQ_CODEC_NAME, 6, 3);
    ErasureCodecOptions options = new ErasureCodecOptions(schema);
    
    RaptorQErasureCodec codec = new RaptorQErasureCodec(conf, options);
    
    assertEquals("Codec name should match", ErasureCodeConstants.RAPTORQ_CODEC_NAME, codec.getName());
    assertEquals("Schema should match", schema, codec.getSchema());
    
    // Test de création des encodeurs/décodeurs
    assertNotNull("Encoder should be created", codec.createEncoder());
    assertNotNull("Decoder should be created", codec.createDecoder());
  }

  @Test
  public void testRaptorQEncoderDecoderIntegration() {
    ErasureCoderOptions options = new ErasureCoderOptions(6, 3);
    
    RaptorQErasureEncoder encoder = new RaptorQErasureEncoder(options);
    RaptorQErasureDecoder decoder = new RaptorQErasureDecoder(options);
    
    assertEquals("Encoder data units should match", 6, encoder.getNumDataUnits());
    assertEquals("Encoder parity units should match", 3, encoder.getNumParityUnits());
    assertEquals("Decoder data units should match", 6, decoder.getNumDataUnits());
    assertEquals("Decoder parity units should match", 3, decoder.getNumParityUnits());
  }

  @Test
  public void testRaptorQUtilByteBufferConversion() {
    // Test de conversion ByteBuffer ↔ byte array
    byte[] originalData = "Hello, RaptorQ!".getBytes();
    ByteBuffer buffer = RaptorQUtil.arrayToByteBuffer(originalData);
    
    byte[] convertedData = RaptorQUtil.byteBufferToArray(buffer);
    
    assertArrayEquals("Data should be preserved in conversion", 
                     originalData, convertedData);
  }

  @Test
  public void testRaptorQConstantsValidation() {
    // Test des constantes RaptorQ
    assertEquals("RaptorQ codec name should be correct", 
                 "raptorq", ErasureCodeConstants.RAPTORQ_CODEC_NAME);
    
    assertNotNull("RaptorQ schema should exist", 
                  ErasureCodeConstants.RAPTORQ_6_3_SCHEMA);
    
    assertEquals("RaptorQ schema codec name should match", 
                 ErasureCodeConstants.RAPTORQ_CODEC_NAME, 
                 ErasureCodeConstants.RAPTORQ_6_3_SCHEMA.getCodecName());
    
    assertEquals("RaptorQ schema data units should be 6", 
                 6, ErasureCodeConstants.RAPTORQ_6_3_SCHEMA.getNumDataUnits());
    
    assertEquals("RaptorQ schema parity units should be 3", 
                 3, ErasureCodeConstants.RAPTORQ_6_3_SCHEMA.getNumParityUnits());
  }

  @Test
  public void testRaptorQUtilSourceSymbolCalculation() {
    long dataLength = 1024 * 1024; // 1MB
    FECParameters fecParams = RaptorQUtil.createHadoopFECParameters(
        dataLength, 6, 3);
    
    int numSourceSymbols = RaptorQUtil.getNumberOfSourceSymbols(fecParams);
    assertTrue("Number of source symbols should be positive", numSourceSymbols > 0);
    
    // Vérifier que le calcul est cohérent
    long expectedSymbols = (long) Math.ceil((double) dataLength / fecParams.symbolSize());
    assertEquals("Source symbol calculation should be correct", 
                 expectedSymbols, numSourceSymbols);
  }

  @Test
  public void testRaptorQUtilRepairSymbolCalculation() {
    long dataLength = 1024 * 1024; // 1MB
    FECParameters fecParams = RaptorQUtil.createHadoopFECParameters(
        dataLength, 6, 3);
    
    int overhead = 10; // 10%
    int numRepairSymbols = RaptorQUtil.getNumberOfRepairSymbols(fecParams, overhead);
    
    assertTrue("Number of repair symbols should be positive", numRepairSymbols > 0);
    
    int numSourceSymbols = RaptorQUtil.getNumberOfSourceSymbols(fecParams);
    int expectedRepairSymbols = (int) Math.ceil((double) numSourceSymbols * overhead / 100.0);
    
    assertEquals("Repair symbol calculation should be correct", 
                 expectedRepairSymbols, numRepairSymbols);
  }

  /**
   * Vérifie si un nombre est une puissance de 2.
   */
  private boolean isPowerOfTwo(int n) {
    return n > 0 && (n & (n - 1)) == 0;
  }
}
