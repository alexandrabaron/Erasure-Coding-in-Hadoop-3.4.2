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

import net.fec.openrq.OpenRQ;
import net.fec.openrq.encoder.DataEncoder;
import net.fec.openrq.encoder.SourceBlockEncoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.IOException;
import java.nio.ByteBuffer;

@InterfaceAudience.Private
public class RaptorQRawEncoder extends RawErasureEncoder {

  public RaptorQRawEncoder(ErasureCoderOptions coderOptions) {
    super(coderOptions);
  }

  @Override
  protected void doEncode(ByteBufferEncodingState encodingState) throws IOException {
    int k = getNumDataUnits();
    int m = getNumParityUnits();
    int T = encodingState.encodeLength;

    // Gather input into a contiguous byte[] for OpenRQ
    byte[] data = new byte[k * T];
    int pos = 0;
    for (int i = 0; i < k; i++) {
      ByteBuffer in = encodingState.inputs[i];
      int oldPos = in.position();
      in.get(data, pos, T);
      in.position(oldPos); // don't consume; RawErasureEncoder will advance after return
      pos += T;
    }

    // Configure OpenRQ (single source block Z=1, symbol size T)
    FECParameters fecParams = FECParameters.newParameters((long) data.length, T, 1);
    DataEncoder enc = OpenRQ.newEncoder(data, fecParams);
    SourceBlockEncoder sbe = enc.sourceBlock(0);

    // Produce m repair symbols with deterministic ESIs K..K+m-1
    for (int p = 0; p < m; p++) {
      int esi = k + p;
      ByteBuffer sym = sbe.repairPacket(esi).symbols();
      // Write into output parity buffers
      ByteBuffer out = encodingState.outputs[p];
      int outPos = out.position();
      out.put(sym);
      out.position(outPos); // don't finalize here; framework adjusts positions
    }
  }

  @Override
  protected void doEncode(ByteArrayEncodingState encodingState) throws IOException {
    int k = getNumDataUnits();
    int m = getNumParityUnits();
    int T = encodingState.encodeLength;

    // Concatenate inputs
    byte[] data = new byte[k * T];
    int pos = 0;
    for (int i = 0; i < k; i++) {
      System.arraycopy(encodingState.inputs[i], encodingState.inputOffsets[i], data, pos, T);
      pos += T;
    }

    FECParameters fecParams = FECParameters.newParameters((long) data.length, T, 1);
    DataEncoder enc = OpenRQ.newEncoder(data, fecParams);
    SourceBlockEncoder sbe = enc.sourceBlock(0);

    for (int p = 0; p < m; p++) {
      int esi = k + p;
      ByteBuffer sym = sbe.repairPacket(esi).symbols();
      sym.get(encodingState.outputs[p], encodingState.outputOffsets[p], T);
    }
  }
}
