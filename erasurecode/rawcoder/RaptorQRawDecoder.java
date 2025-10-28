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
import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.Parsed;
import net.fec.openrq.decoder.DataDecoder;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.IOException;
import java.nio.ByteBuffer;

@InterfaceAudience.Private
public class RaptorQRawDecoder extends RawErasureDecoder {

  public RaptorQRawDecoder(ErasureCoderOptions coderOptions) {
    super(coderOptions);
  }

  @Override
  protected void doDecode(ByteBufferDecodingState decodingState) throws IOException {
    int k = getNumDataUnits();
    int m = getNumParityUnits();
    int T = decodingState.decodeLength;
    if (T == 0) {
      return;
    }

    // Build concatenated buffer for available symbols (exactly k symbols suffice)
    byte[] data = new byte[k * T];
    int sbn = 0; // single block

    // Prepare OpenRQ decoder
    FECParameters fecParams = FECParameters.newParameters((long) data.length, T, 1);
    DataDecoder dec = OpenRQ.newDecoderWithZeroOverhead(fecParams);
    SourceBlockDecoder sbd = dec.sourceBlock(sbn);

    // Feed available source symbols and repair symbols
    for (int i = 0; i < k + m; i++) {
      ByteBuffer buf = decodingState.inputs[i];
      if (buf == null) continue;
      int esi = (i < k) ? i : i; // data ESIs 0..K-1, repair ESIs K..K+M-1
      int oldPos = buf.position();
      ByteBuffer slice = buf.slice();
      slice.limit(T);
      Parsed<EncodingPacket> parsed = (i < k)
          ? dec.parsePacket(sbn, esi, slice, false)
          : dec.parsePacket(sbn, esi, slice, false);
      if (parsed.isValid()) {
        sbd.putEncodingPacket(parsed.value());
      }
      buf.position(oldPos);
    }

    // Reconstruct erased outputs: after successful decode, fetch source symbols
    if (sbd.isSourceBlockDecoded()) {
      // Recovered data is internally in ArrayDataDecoder's data array
      byte[] recovered = ((ArrayDataDecoder) dec).dataArray();
      // Write only requested erased indexes
      for (int j = 0; j < decodingState.erasedIndexes.length; j++) {
        int erased = decodingState.erasedIndexes[j];
        ByteBuffer out = decodingState.outputs[j];
        int outPos = out.position();
        if (erased < k) {
          out.put(recovered, erased * T, T);
        } else {
          // For parity erasures, regenerate by re-encoding via encoder semantics is non-trivial here;
          // shortcut: parity reconstruction can be done by re-encoding recovered data
          ByteBuffer parity = regenerateParity(erased - k, recovered, T, k, m);
          out.put(parity);
        }
        out.position(outPos);
      }
    } else {
      throw new IOException("RaptorQ decoding failed: insufficient symbols");
    }
  }

  @Override
  protected void doDecode(ByteArrayDecodingState decodingState) throws IOException {
    int k = getNumDataUnits();
    int m = getNumParityUnits();
    int T = decodingState.decodeLength;
    if (T == 0) {
      return;
    }

    byte[] data = new byte[k * T];
    int sbn = 0;

    FECParameters fecParams = FECParameters.newParameters((long) data.length, T, 1);
    DataDecoder dec = OpenRQ.newDecoderWithZeroOverhead(fecParams);
    SourceBlockDecoder sbd = dec.sourceBlock(sbn);

    for (int i = 0; i < k + m; i++) {
      byte[] in = decodingState.inputs[i];
      if (in == null) continue;
      int off = decodingState.inputOffsets[i];
      int esi = i; // same mapping as above
      Parsed<EncodingPacket> parsed = dec.parsePacket(sbn, esi, in, off, T, false);
      if (parsed.isValid()) {
        sbd.putEncodingPacket(parsed.value());
      }
    }

    if (sbd.isSourceBlockDecoded()) {
      byte[] recovered = ((ArrayDataDecoder) dec).dataArray();
      for (int j = 0; j < decodingState.erasedIndexes.length; j++) {
        int erased = decodingState.erasedIndexes[j];
        if (erased < k) {
          System.arraycopy(recovered, erased * T, decodingState.outputs[j], decodingState.outputOffsets[j], T);
        } else {
          ByteBuffer parity = regenerateParity(erased - k, recovered, T, k, m);
          parity.get(decodingState.outputs[j], decodingState.outputOffsets[j], T);
        }
      }
    } else {
      throw new IOException("RaptorQ decoding failed: insufficient symbols");
    }
  }

  private ByteBuffer regenerateParity(int parityIndex, byte[] recoveredData, int T, int k, int m) {
    FECParameters fecParams = FECParameters.newParameters((long) recoveredData.length, T, 1);
    DataEncoder enc = OpenRQ.newEncoder(recoveredData, fecParams);
    SourceBlockEncoder sbe = enc.sourceBlock(0);
    int esi = k + parityIndex;
    return sbe.repairPacket(esi).symbols();
  }
}
