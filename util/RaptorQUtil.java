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
package org.apache.hadoop.io.erasurecode.util;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.io.erasurecode.ErasureCoderOptions;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.ArrayDataEncoder;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.encoder.SourceBlockEncoder;
import net.fec.openrq.decoder.SourceBlockDecoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for RaptorQ operations using OpenRQ library.
 * 
 * This class provides helper methods to integrate OpenRQ library
 * with Hadoop's erasure coding framework.
 */
@InterfaceAudience.Private
public class RaptorQUtil {
  
  // Default symbol size for Hadoop blocks (typically 64KB)
  private static final int DEFAULT_SYMBOL_SIZE = 64 * 1024;
  
  // Default interleaver length (no interleaving for simplicity)
  private static final int DEFAULT_INTERLEAVER_LENGTH = 1;
  
  /**
   * Create FEC parameters for RaptorQ encoding/decoding.
   * 
   * @param dataLength total length of data to encode
   * @param symbolSize size of each symbol in bytes
   * @param numSourceBlocks number of source blocks
   * @return FEC parameters for OpenRQ
   */
  public static FECParameters createFECParameters(long dataLength, int symbolSize, int numSourceBlocks) {
    return FECParameters.newParameters(dataLength, symbolSize, numSourceBlocks, DEFAULT_INTERLEAVER_LENGTH);
  }
  
  /**
   * Create FEC parameters optimized for Hadoop erasure coding.
   * 
   * @param dataLength total length of data to encode
   * @param numDataUnits number of data units (k)
   * @param numParityUnits number of parity units (m)
   * @return FEC parameters for OpenRQ
   */
  public static FECParameters createHadoopFECParameters(long dataLength, int numDataUnits, int numParityUnits) {
    // Calculate optimal symbol size based on data length and number of units
    int symbolSize = calculateOptimalSymbolSize(dataLength, numDataUnits + numParityUnits);
    
    // For Hadoop, we typically use one source block
    int numSourceBlocks = 1;
    
    return createFECParameters(dataLength, symbolSize, numSourceBlocks);
  }
  
  /**
   * Calculate optimal symbol size for given data length and total units.
   * 
   * @param dataLength total data length
   * @param totalUnits total number of units (data + parity)
   * @return optimal symbol size
   */
  private static int calculateOptimalSymbolSize(long dataLength, int totalUnits) {
    // Calculate base symbol size
    long baseSymbolSize = dataLength / totalUnits;
    
    // Ensure minimum symbol size
    if (baseSymbolSize < 1024) {
      baseSymbolSize = 1024;
    }
    
    // Ensure maximum symbol size (not too large for memory efficiency)
    if (baseSymbolSize > DEFAULT_SYMBOL_SIZE) {
      baseSymbolSize = DEFAULT_SYMBOL_SIZE;
    }
    
    // Round to nearest power of 2 for efficiency
    return (int) roundToPowerOfTwo(baseSymbolSize);
  }
  
  /**
   * Round a number to the nearest power of 2.
   */
  private static long roundToPowerOfTwo(long n) {
    if (n <= 0) return 1;
    
    long power = 1;
    while (power < n) {
      power <<= 1;
    }
    
    // Return the smaller power if it's closer
    long lower = power >> 1;
    return (n - lower) < (power - n) ? lower : power;
  }
  
  /**
   * Create OpenRQ encoder for given data and FEC parameters.
   * 
   * @param data input data to encode
   * @param fecParams FEC parameters
   * @return OpenRQ data encoder
   */
  public static ArrayDataEncoder createEncoder(byte[] data, FECParameters fecParams) {
    return OpenRQ.newEncoder(data, fecParams);
  }
  
  /**
   * Create OpenRQ decoder for given FEC parameters.
   * 
   * @param fecParams FEC parameters
   * @param symbolOverhead symbol overhead for decoding
   * @return OpenRQ data decoder
   */
  public static ArrayDataDecoder createDecoder(FECParameters fecParams, int symbolOverhead) {
    return OpenRQ.newDecoder(fecParams, symbolOverhead);
  }
  
  /**
   * Encode data using RaptorQ and return encoding packets.
   * 
   * @param data input data
   * @param fecParams FEC parameters
   * @param numParityPackets number of parity packets to generate
   * @return list of encoding packets
   */
  public static List<EncodingPacket> encodeData(byte[] data, FECParameters fecParams, int numParityPackets) {
    ArrayDataEncoder encoder = createEncoder(data, fecParams);
    List<EncodingPacket> packets = new ArrayList<>();
    
    // Get source block encoder (assuming single source block)
    SourceBlockEncoder sourceBlockEncoder = encoder.sourceBlock(0);
    
    // Generate source packets (data packets)
    int numSourcePackets = sourceBlockEncoder.numberOfSourceSymbols();
    for (int i = 0; i < numSourcePackets; i++) {
      packets.add(sourceBlockEncoder.sourcePacket(i));
    }
    
    // Generate repair packets (parity packets)
    for (int i = 0; i < numParityPackets; i++) {
      packets.add(sourceBlockEncoder.repairPacket(i));
    }
    
    return packets;
  }
  
  /**
   * Decode data from encoding packets.
   * 
   * @param packets encoding packets (source + repair)
   * @param fecParams FEC parameters
   * @param symbolOverhead symbol overhead for decoding
   * @return decoded data
   */
  public static byte[] decodeData(List<EncodingPacket> packets, FECParameters fecParams, int symbolOverhead) {
    ArrayDataDecoder decoder = createDecoder(fecParams, symbolOverhead);
    
    // Get source block decoder (assuming single source block)
    SourceBlockDecoder sourceBlockDecoder = decoder.sourceBlock(0);
    
    // Feed packets to decoder
    for (EncodingPacket packet : packets) {
      sourceBlockDecoder.putEncodingPacket(packet);
    }
    
    // Check if decoding is possible
    if (!sourceBlockDecoder.isDataDecoded()) {
      throw new RuntimeException("Insufficient packets for decoding");
    }
    
    // Extract decoded data
    return sourceBlockDecoder.dataArray();
  }
  
  /**
   * Convert Hadoop ByteBuffer to byte array for OpenRQ.
   * 
   * @param buffer ByteBuffer to convert
   * @return byte array
   */
  public static byte[] byteBufferToArray(ByteBuffer buffer) {
    if (buffer.hasArray()) {
      byte[] array = buffer.array();
      int offset = buffer.arrayOffset() + buffer.position();
      int length = buffer.remaining();
      
      if (offset == 0 && length == array.length) {
        return array;
      } else {
        byte[] result = new byte[length];
        System.arraycopy(array, offset, result, 0, length);
        return result;
      }
    } else {
      byte[] result = new byte[buffer.remaining()];
      buffer.duplicate().get(result);
      return result;
    }
  }
  
  /**
   * Convert byte array to Hadoop ByteBuffer.
   * 
   * @param data byte array to convert
   * @return ByteBuffer
   */
  public static ByteBuffer arrayToByteBuffer(byte[] data) {
    return ByteBuffer.wrap(data);
  }
  
  /**
   * Calculate the number of source symbols for given FEC parameters.
   * 
   * @param fecParams FEC parameters
   * @return number of source symbols
   */
  public static int getNumberOfSourceSymbols(FECParameters fecParams) {
    return (int) Math.ceil((double) fecParams.dataLength() / fecParams.symbolSize());
  }
  
  /**
   * Calculate the number of repair symbols needed for given overhead.
   * 
   * @param fecParams FEC parameters
   * @param overhead symbol overhead (typically 0-10%)
   * @return number of repair symbols
   */
  public static int getNumberOfRepairSymbols(FECParameters fecParams, int overhead) {
    int numSourceSymbols = getNumberOfSourceSymbols(fecParams);
    return (int) Math.ceil((double) numSourceSymbols * overhead / 100.0);
  }
}
