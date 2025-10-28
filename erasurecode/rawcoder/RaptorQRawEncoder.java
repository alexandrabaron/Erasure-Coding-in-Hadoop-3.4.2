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

import java.io.IOException;

@InterfaceAudience.Private
public class RaptorQRawEncoder extends RawErasureEncoder {

  public RaptorQRawEncoder(ErasureCoderOptions coderOptions) {
    super(coderOptions);
  }

  @Override
  protected void doEncode(ByteBufferEncodingState encodingState) throws IOException {
    throw new UnsupportedOperationException("RaptorQRawEncoder ByteBuffer path not implemented yet");
  }

  @Override
  protected void doEncode(ByteArrayEncodingState encodingState) throws IOException {
    throw new UnsupportedOperationException("RaptorQRawEncoder byte[] path not implemented yet");
  }
}
