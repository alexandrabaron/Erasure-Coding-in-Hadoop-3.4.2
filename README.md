## Hadoop Erasure Coding – RaptorQ Integration

### Goal
Integrate a non-RS erasure code into Hadoop’s erasure coding framework using RaptorQ via the OpenRQ Java library.

### Context
- Hadoop EC layers (codec/coder/rawcoder) are present in `erasurecode/` and mirror upstream Hadoop 3.4.x.
- We will add a new codec named `raptorq` that delegates math to OpenRQ.

### High-level Plan
1. Add a new codec family `raptorq` alongside existing RS/XOR.
2. Implement `RaptorQRawEncoder`/`RaptorQRawDecoder` using OpenRQ.
3. Wire factory via ServiceLoader and register codec in `CodecUtil`/`ErasureCodeConstants`.
4. Provide `RaptorQErasureCodec` and `RaptorQ{Encoder,Decoder}` adapters.
5. Validate with unit tests and simple end‑to‑end encode/decode checks.

### Current Status
- Docs completed:
  - Hadoop EC architecture analysis updated (see `Hadoop_ErasureCode_Architecture_Analysis.md`).
  - OpenRQ architecture/usage overview added (see `OpenRQ_Architecture_Diagram.md`).
- Implementation: pending.

### Living TODO (kept updated)
1. Design integration plan: where RaptorQ plugs into Hadoop EC layers [completed]
2. Create codec scaffolding:
   - Add `raptorq` constants and config mapping in `ErasureCodeConstants.java` and `CodecUtil.java` [done]
   - Create `codec/RaptorQErasureCodec.java` [done]
   - Create `coder/RaptorQEncoder.java`, `coder/RaptorQDecoder.java` [done]
3. Implement raw coders:
   - `rawcoder/RaptorQRawEncoder.java` (byte[] and ByteBuffer paths) [done]
   - `rawcoder/RaptorQRawDecoder.java` (byte[] and ByteBuffer paths) [done]
   - `rawcoder/RaptorQRawErasureCoderFactory.java` [done]
   - Register in `META-INF/services/org.apache.hadoop.io.erasurecode.rawcoder.RawErasureCoderFactory` [pending]
4. Bridge with OpenRQ:
   - Decide `symbolSize == chunkSize` policy; implement padding/removal [pending]
   - Deterministic selection of m repair symbols (ESI 0..m-1) [pending]
   - Efficient packetization to/from contiguous buffers [pending]
5. Configuration:
   - Allow `io.erasurecode.codec.raptorq.rawcoders` override [pending]
   - Optional flags: allowChangeInputs/verboseDump passthrough [pending]
6. Tests:
   - Unit tests for encode/decode parity regeneration, multiple erasures ≤ m [done]
   - ByteBuffer vs byte[] coverage [done]
   - Random data fuzzing; padding edge cases [pending]
7. Benchmarks (optional): compare throughput vs XOR/RS on sample sizes [pending]

### Repository Map
- Hadoop EC sources: `erasurecode/`
- OpenRQ library sources: `OpenRQ-master/src/main/net/fec/openrq`

### Configuration
- Enable the codec class mapping (optional, defaults provided):
  - `io.erasurecode.codec.raptorq=org.apache.hadoop.io.erasurecode.codec.RaptorQErasureCodec`
- Choose raw coder fallback order (first wins):
  - `io.erasurecode.codec.raptorq.rawcoders=raptorq_java`
- Toggle native usage (not applicable to RaptorQ here, kept for parity):
  - `io.erasurecode.codec.native.enabled=true`

Notes:
- Data units map to ESIs `0..K-1`; parity units map to ESIs `K..K+m-1`. Changing this mapping will break compatibility.
- All K data chunks in a group must have equal size T. If not, introduce padding (future enhancement).
- ServiceLoader registration requires the file to be present on the runtime classpath at `META-INF/services/org.apache.hadoop.io.erasurecode.rawcoder.RawErasureCoderFactory`. Ensure this path is packaged in the JAR under resources when building.
- Make sure OpenRQ (`net.fec.openrq.*`) classes are on the compile/runtime classpath (include the OpenRQ sources or JAR).

### Notes
- OpenRQ does not support sub-block interleaving (>1) but is RFC 6330 compliant.
- Decoding throughput is acceptable for a prototype; focus on correctness and adapter fidelity first.
- Assumptions: fixed chunk length T across k data units; ESI mapping data=0..K-1, parity=K..K+m-1.


