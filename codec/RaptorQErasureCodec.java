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
package org.apache.hadoop.io.erasurecode.codec;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.erasurecode.ErasureCodecOptions;
import org.apache.hadoop.io.erasurecode.coder.ErasureDecoder;
import org.apache.hadoop.io.erasurecode.coder.ErasureEncoder;
import org.apache.hadoop.io.erasurecode.coder.RaptorQErasureDecoder;
import org.apache.hadoop.io.erasurecode.coder.RaptorQErasureEncoder;

/**
 * A RaptorQ erasure codec.
 * 
 * RaptorQ is a fountain code that can generate unlimited parity symbols
 * and recover data from any subset of symbols. It's particularly efficient
 * for distributed storage systems like Hadoop.
 * 
 * This implementation uses the OpenRQ library for the core RaptorQ algorithm.
 */
@InterfaceAudience.Private
public class RaptorQErasureCodec extends ErasureCodec {

  public RaptorQErasureCodec(Configuration conf, ErasureCodecOptions options) {
    super(conf, options);
    // RaptorQ can handle flexible k and m values
    // The actual parameters will be determined by the OpenRQ library
  }

  @Override
  public ErasureEncoder createEncoder() {
    return new RaptorQErasureEncoder(getCoderOptions());
  }

  @Override
  public ErasureDecoder createDecoder() {
    return new RaptorQErasureDecoder(getCoderOptions());
  }
}
