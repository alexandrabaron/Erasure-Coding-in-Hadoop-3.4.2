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
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for RaptorQ erasure code implementation.
 */
public class TestRaptorQErasureCode {

  @Test
  public void testRaptorQCodecCreation() {
    Configuration conf = new Configuration();
    ECSchema schema = new ECSchema(ErasureCodeConstants.RAPTORQ_CODEC_NAME, 6, 3);
    ErasureCodecOptions options = new ErasureCodecOptions(schema);
    
    RaptorQErasureCodec codec = new RaptorQErasureCodec(conf, options);
    
    assertEquals(ErasureCodeConstants.RAPTORQ_CODEC_NAME, codec.getName());
    assertEquals(6, codec.getSchema().getNumDataUnits());
    assertEquals(3, codec.getSchema().getNumParityUnits());
  }

  @Test
  public void testRaptorQEncoderCreation() {
    ErasureCoderOptions options = new ErasureCoderOptions(6, 3);
    RaptorQErasureEncoder encoder = new RaptorQErasureEncoder(options);
    
    assertEquals(6, encoder.getNumDataUnits());
    assertEquals(3, encoder.getNumParityUnits());
  }

  @Test
  public void testRaptorQDecoderCreation() {
    ErasureCoderOptions options = new ErasureCoderOptions(6, 3);
    RaptorQErasureDecoder decoder = new RaptorQErasureDecoder(options);
    
    assertEquals(6, decoder.getNumDataUnits());
    assertEquals(3, decoder.getNumParityUnits());
  }

  @Test
  public void testRaptorQRawEncoderCreation() {
    ErasureCoderOptions options = new ErasureCoderOptions(6, 3);
    RaptorQRawEncoder encoder = new RaptorQRawEncoder(options);
    
    assertEquals(6, encoder.getNumDataUnits());
    assertEquals(3, encoder.getNumParityUnits());
  }

  @Test
  public void testRaptorQRawDecoderCreation() {
    ErasureCoderOptions options = new ErasureCoderOptions(6, 3);
    RaptorQRawDecoder decoder = new RaptorQRawDecoder(options);
    
    assertEquals(6, decoder.getNumDataUnits());
    assertEquals(3, decoder.getNumParityUnits());
  }

  @Test
  public void testRaptorQRawErasureCoderFactory() {
    RaptorQRawErasureCoderFactory factory = new RaptorQRawErasureCoderFactory();
    
    assertEquals("raptorq_java", factory.getCoderName());
    assertEquals(ErasureCodeConstants.RAPTORQ_CODEC_NAME, factory.getCodecName());
    
    ErasureCoderOptions options = new ErasureCoderOptions(6, 3);
    assertNotNull(factory.createEncoder(options));
    assertNotNull(factory.createDecoder(options));
  }

  @Test
  public void testRaptorQConstants() {
    assertEquals("raptorq", ErasureCodeConstants.RAPTORQ_CODEC_NAME);
    assertNotNull(ErasureCodeConstants.RAPTORQ_6_3_SCHEMA);
    assertEquals(ErasureCodeConstants.RAPTORQ_CODEC_NAME, 
                 ErasureCodeConstants.RAPTORQ_6_3_SCHEMA.getCodecName());
    assertEquals(6, ErasureCodeConstants.RAPTORQ_6_3_SCHEMA.getNumDataUnits());
    assertEquals(3, ErasureCodeConstants.RAPTORQ_6_3_SCHEMA.getNumParityUnits());
  }
}
