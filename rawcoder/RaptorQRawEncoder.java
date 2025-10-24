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
package org.apache.hadoop.io.erasurecode.rawcoder;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.io.erasurecode.ErasureCoderOptions;
import org.apache.hadoop.io.erasurecode.util.RaptorQUtil;

import net.fec.openrq.ArrayDataEncoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.encoder.SourceBlockEncoder;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * A raw encoder in RaptorQ code scheme using OpenRQ library.
 *
 * RaptorQ is a fountain code that can generate unlimited parity symbols.
 * This implementation uses the OpenRQ library for the core RaptorQ algorithm.
 * 
 * Note: This implementation stores FEC parameters as metadata to enable
 * proper decoding. In a production system, these parameters should be
 * persisted with the encoded data.
 */
@InterfaceAudience.Private
public class RaptorQRawEncoder extends RawErasureEncoder {

  public RaptorQRawEncoder(ErasureCoderOptions coderOptions) {
    super(coderOptions);
  }

  @Override
  protected void doEncode(ByteBufferEncodingState encodingState) {
    try {
      // Reset output buffers
      CoderUtil.resetOutputBuffers(encodingState.outputs,
          encodingState.encodeLength);
      
      // Convert input ByteBuffers to byte arrays
      byte[][] inputArrays = new byte[encodingState.inputs.length][];
      int totalDataLength = 0;
      
      for (int i = 0; i < encodingState.inputs.length; i++) {
        if (encodingState.inputs[i] != null) {
          inputArrays[i] = RaptorQUtil.byteBufferToArray(encodingState.inputs[i]);
          totalDataLength += inputArrays[i].length;
        }
      }
      
      // Concatenate all input data
      byte[] combinedData = new byte[totalDataLength];
      int offset = 0;
      for (byte[] inputArray : inputArrays) {
        if (inputArray != null) {
          System.arraycopy(inputArray, 0, combinedData, offset, inputArray.length);
          offset += inputArray.length;
        }
      }
      
      // Create FEC parameters for RaptorQ
      FECParameters fecParams = RaptorQUtil.createHadoopFECParameters(
          totalDataLength, getNumDataUnits(), getNumParityUnits());
      
      // Create OpenRQ encoder
      ArrayDataEncoder encoder = RaptorQUtil.createEncoder(combinedData, fecParams);
      
      // Get source block encoder (assuming single source block)
      SourceBlockEncoder sourceBlockEncoder = encoder.sourceBlock(0);
      
      // Generate source packets (data packets) - these go to first few outputs
      int numSourcePackets = Math.min(getNumDataUnits(), sourceBlockEncoder.numberOfSourceSymbols());
      for (int i = 0; i < numSourcePackets && i < encodingState.outputs.length; i++) {
        EncodingPacket sourcePacket = sourceBlockEncoder.sourcePacket(i);
        ByteBuffer outputBuffer = encodingState.outputs[i];
        
        // Copy packet data to output buffer
        byte[] packetData = sourcePacket.asArray();
        outputBuffer.put(packetData);
      }
      
      // Generate repair packets (parity packets) - these go to remaining outputs
      int numRepairPackets = Math.min(getNumParityUnits(), 
          encodingState.outputs.length - numSourcePackets);
      for (int i = 0; i < numRepairPackets; i++) {
        int outputIndex = numSourcePackets + i;
        if (outputIndex < encodingState.outputs.length) {
          EncodingPacket repairPacket = sourceBlockEncoder.repairPacket(i);
          ByteBuffer outputBuffer = encodingState.outputs[outputIndex];
          
          // Copy packet data to output buffer
          byte[] packetData = repairPacket.asArray();
          outputBuffer.put(packetData);
        }
      }
      
      // Store FEC parameters as metadata in the last output buffer
      // This is a simplified approach - in production, metadata should be stored separately
      if (encodingState.outputs.length > 0) {
        storeFECParametersAsMetadata(fecParams, encodingState.outputs[encodingState.outputs.length - 1]);
      }
      
    } catch (Exception e) {
      throw new RuntimeException("RaptorQ encoding failed", e);
    }
  }

  @Override
  protected void doEncode(ByteArrayEncodingState encodingState) {
    try {
      int dataLen = encodingState.encodeLength;
      CoderUtil.resetOutputBuffers(encodingState.outputs,
          encodingState.outputOffsets, dataLen);
      
      // Concatenate all input data
      int totalDataLength = 0;
      for (int i = 0; i < encodingState.inputs.length; i++) {
        if (encodingState.inputs[i] != null) {
          totalDataLength += encodingState.inputs[i].length - encodingState.inputOffsets[i];
        }
      }
      
      byte[] combinedData = new byte[totalDataLength];
      int offset = 0;
      for (int i = 0; i < encodingState.inputs.length; i++) {
        if (encodingState.inputs[i] != null) {
          int inputLength = encodingState.inputs[i].length - encodingState.inputOffsets[i];
          System.arraycopy(encodingState.inputs[i], encodingState.inputOffsets[i], 
                          combinedData, offset, inputLength);
          offset += inputLength;
        }
      }
      
      // Create FEC parameters for RaptorQ
      FECParameters fecParams = RaptorQUtil.createHadoopFECParameters(
          totalDataLength, getNumDataUnits(), getNumParityUnits());
      
      // Create OpenRQ encoder
      ArrayDataEncoder encoder = RaptorQUtil.createEncoder(combinedData, fecParams);
      
      // Get source block encoder (assuming single source block)
      SourceBlockEncoder sourceBlockEncoder = encoder.sourceBlock(0);
      
      // Generate source packets (data packets)
      int numSourcePackets = Math.min(getNumDataUnits(), sourceBlockEncoder.numberOfSourceSymbols());
      for (int i = 0; i < numSourcePackets && i < encodingState.outputs.length; i++) {
        EncodingPacket sourcePacket = sourceBlockEncoder.sourcePacket(i);
        byte[] packetData = sourcePacket.asArray();
        
        // Copy packet data to output
        System.arraycopy(packetData, 0, encodingState.outputs[i], 
                        encodingState.outputOffsets[i], 
                        Math.min(packetData.length, dataLen));
      }
      
      // Generate repair packets (parity packets)
      int numRepairPackets = Math.min(getNumParityUnits(), 
          encodingState.outputs.length - numSourcePackets);
      for (int i = 0; i < numRepairPackets; i++) {
        int outputIndex = numSourcePackets + i;
        if (outputIndex < encodingState.outputs.length) {
          EncodingPacket repairPacket = sourceBlockEncoder.repairPacket(i);
          byte[] packetData = repairPacket.asArray();
          
          // Copy packet data to output
          System.arraycopy(packetData, 0, encodingState.outputs[outputIndex], 
                          encodingState.outputOffsets[outputIndex], 
                          Math.min(packetData.length, dataLen));
        }
      }
      
      // Store FEC parameters as metadata in the last output
      if (encodingState.outputs.length > 0) {
        storeFECParametersAsMetadata(fecParams, encodingState.outputs[encodingState.outputs.length - 1]);
      }
      
    } catch (Exception e) {
      throw new RuntimeException("RaptorQ encoding failed", e);
    }
  }
  
  /**
   * Store FEC parameters as metadata for later decoding.
   * This is a simplified approach - in production, metadata should be stored separately.
   */
  private void storeFECParametersAsMetadata(FECParameters fecParams, ByteBuffer outputBuffer) {
    // For simplicity, we'll store a marker and basic info
    // In a real implementation, this should be stored separately
    try {
      // Store a simple marker to indicate FEC parameters
      outputBuffer.put((byte) 0xFF); // Marker
      outputBuffer.putLong(fecParams.dataLength());
      outputBuffer.putInt(fecParams.symbolSize());
      outputBuffer.putInt(fecParams.numberOfSourceBlocks());
    } catch (Exception e) {
      // If we can't store metadata, continue without it
      // The decoder will need to estimate parameters
    }
  }
  
  /**
   * Store FEC parameters as metadata for later decoding (byte array version).
   */
  private void storeFECParametersAsMetadata(FECParameters fecParams, byte[] outputArray) {
    // For simplicity, we'll store a marker and basic info
    // In a real implementation, this should be stored separately
    try {
      if (outputArray.length >= 17) { // Need space for marker + 8 + 4 + 4 bytes
        int offset = outputArray.length - 17;
        outputArray[offset] = (byte) 0xFF; // Marker
        // Store data length (8 bytes)
        long dataLength = fecParams.dataLength();
        for (int i = 0; i < 8; i++) {
          outputArray[offset + 1 + i] = (byte) (dataLength >>> (8 * i));
        }
        // Store symbol size (4 bytes)
        int symbolSize = fecParams.symbolSize();
        for (int i = 0; i < 4; i++) {
          outputArray[offset + 9 + i] = (byte) (symbolSize >>> (8 * i));
        }
        // Store number of source blocks (4 bytes)
        int numSourceBlocks = fecParams.numberOfSourceBlocks();
        for (int i = 0; i < 4; i++) {
          outputArray[offset + 13 + i] = (byte) (numSourceBlocks >>> (8 * i));
        }
      }
    } catch (Exception e) {
      // If we can't store metadata, continue without it
    }
  }
}
