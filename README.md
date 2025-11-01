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

### Living TODO 
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
- HDFS policy wiring: we added a built-in policy `RAPTORQ_6_3` (id=6) in `SystemErasureCodingPolicies.java` using schema `(k=6, m=3, cellSize=1MB)`. Enable it via HDFS tooling like any built-in policy.

## Build and Test

### Prerequisites
- JDK 8+ installed and on PATH
- Windows PowerShell (commands below use PowerShell syntax)

### Run tests
Tests live under `tests/org/apache/hadoop/io/erasurecode/` and provide a simple main runner.

PowerShell:

```powershell
$cp = "out;OpenRQ-master/src/main"  # compiled classes + OpenRQ sources on classpath
java -cp $cp org.apache.hadoop.io.erasurecode.RaptorQRawCoderTest
```

Expected output:

```
OK: RaptorQRawCoder tests passed
```

### Troubleshooting

#### Classpath issues
- **Class not found for OpenRQ (`net.fec.openrq.*`)**: ensure `OpenRQ-master/src/main` (or its JAR) is on `-cp` both at compile-time and runtime.
- **ServiceLoader factory not discovered**: verify `erasurecode/META-INF/services/org.apache.hadoop.io.erasurecode.rawcoder.RawErasureCoderFactory` is on the runtime classpath (packaged in resources when building a JAR). For raw `javac/java` runs, classes are loaded directly and ServiceLoader registration is used by higher-level Hadoop flows; the tests instantiate the raw coders directly.

#### HDFS cluster requirements
- **"The number of DataNodes is only 0"** or **"N DataNodes are required for the erasure coding policies. The number of DataNodes is only M"**:
  - This is expected: HDFS enforces that you have at least `k+m` DataNodes to enable an EC policy.
  - For RAPTORQ-6-3, you need at least 9 DataNodes (6 data + 3 parity).
  - **To test the code logic without HDFS**, use the unit test (`RaptorQRawCoderTest`) which doesn't require a cluster.
  - **To test with HDFS**: start a mini-cluster with 9+ DataNodes, or temporarily use a smaller schema like (k=2, m=1) that only needs 3 DataNodes.

#### Performance
- **OutOfMemoryError for very large chunk sizes (T)**: current implementation concatenates `k*T` into a buffer; reduce T during testing or refactor to streaming.

### Notes
- OpenRQ does not support sub-block interleaving (>1) but is RFC 6330 compliant.
- Decoding throughput is acceptable for a prototype; focus on correctness and adapter fidelity first.
- Assumptions: fixed chunk length T across k data units; ESI mapping data=0..K-1, parity=K..K+m-1.


