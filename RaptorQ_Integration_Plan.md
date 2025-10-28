## RaptorQ ↔ Hadoop Erasure Coding Integration Plan

### Codec identity
- Codec name: `raptorq`
- Default raw coder name: `raptorq_java`

### New classes to add
1) `erasurecode/codec/RaptorQErasureCodec.java`
- Extends `org.apache.hadoop.io.erasurecode.codec.ErasureCodec`
- `createEncoder()` → returns `coder.RaptorQEncoder`
- `createDecoder()` → returns `coder.RaptorQDecoder`

2) `erasurecode/coder/RaptorQEncoder.java`
- Extends `org.apache.hadoop.io.erasurecode.coder.ErasureEncoder`
- In `prepareEncodingStep`:
  - Build `RawErasureEncoder` with `CodecUtil.createRawEncoder(conf, "raptorq", getOptions())`
  - Return `ErasureEncodingStep(getInputBlocks(bg), getOutputBlocks(bg), raw)`

3) `erasurecode/coder/RaptorQDecoder.java`
- Extends `org.apache.hadoop.io.erasurecode.coder.ErasureDecoder`
- In `prepareDecodingStep`:
  - Build `RawErasureDecoder` with `CodecUtil.createRawDecoder(conf, "raptorq", getOptions())`
  - Return `ErasureDecodingStep(getInputBlocks(bg), getOutputBlocks(bg), raw)`

4) `erasurecode/rawcoder/RaptorQRawEncoder.java`
- Extends `org.apache.hadoop.io.erasurecode.rawcoder.RawErasureEncoder`
- Implements both `doEncode(ByteBufferEncodingState)` and `doEncode(ByteArrayEncodingState)`
- Uses OpenRQ:
  - Symbol size S := chunkLength (encodeLength); if misaligned, pad last symbol and remember pad length
  - Create `FECParameters`, `DataEncoder`, `SourceBlockEncoder`
  - Emit k source packets and the first m repair packets (ESI 0..m-1) to fill parity outputs deterministically

5) `erasurecode/rawcoder/RaptorQRawDecoder.java`
- Extends `org.apache.hadoop.io.erasurecode.rawcoder.RawErasureDecoder`
- Implements both `doDecode(ByteBufferDecodingState)` and `doDecode(ByteArrayDecodingState)`
- Uses OpenRQ:
  - Build `FECParameters` and `DataDecoder`
  - Construct a `SourceBlockDecoder`; feed all available inputs (data+parity) as packets
  - Once ≥ k independent symbols are present, decode missing outputs according to `erasedIndexes`
  - Remove padding from trailing bytes if applied during encoding

6) `erasurecode/rawcoder/RaptorQRawErasureCoderFactory.java`
- Implements `RawErasureCoderFactory`
- `getCoderName() => "raptorq_java"`, `getCodecName() => "raptorq"`
- Creates the raw encoder/decoder above

### Registration and configuration
- `erasurecode/ErasureCodeConstants.java`
  - Add `public static final String RAPTORQ_CODEC_NAME = "raptorq";`
  - Optionally, add sample schemas: `new ECSchema(RAPTORQ_CODEC_NAME, k, m)`
- `erasurecode/CodecUtil.java`
  - In `getCodecClassName`: handle `RAPTORQ_CODEC_NAME` via `io.erasurecode.codec.raptorq` → `RaptorQErasureCodec`
  - Support raw coder list key: `io.erasurecode.codec.raptorq.rawcoders` (default: `raptorq_java`)
- `META-INF/services/org.apache.hadoop.io.erasurecode.rawcoder.RawErasureCoderFactory`
  - Append: `org.apache.hadoop.io.erasurecode.rawcoder.RaptorQRawErasureCoderFactory`

### OpenRQ mapping details
- Parameters
  - `symbolSize` = Hadoop encode segment length (bytes) passed in encoding state
  - `sourceSymbols` = k, `repairNeeded` = m
  - Data length per block = `k * symbolSize` (pad last partial chunk if any)
- Deterministic parity
  - Use repair ESIs starting at 0 for m parity outputs to ensure stable mapping
- Packetization
  - For byte[] path, copy views avoiding extra allocations when possible
  - For ByteBuffer path, fall back to byte[] path when not direct or when random access is needed
- Padding strategy
  - If the last symbol is shorter than `symbolSize`, right-pad with zeros
  - Store pad length locally during encode and strip on decode when reconstructing the final bytes of the last block

### Encoding flow (per EC chunk group)
- Inputs: k data chunks (equal remaining length L), outputs: m parity chunks
- Compute `symbolSize = L` (or negotiated size); build OpenRQ encoder
- Produce m repair symbols deterministically and write into parity outputs

### Decoding flow (per EC chunk group)
- Inputs: data+parity, with nulls on erased positions, plus `erasedIndexes`
- Select any k available symbols among the inputs; build OpenRQ decoder
- Decode and write only the requested erased symbols into outputs
- Trim padding on the final symbol if present

### Config keys (examples)
- `io.erasurecode.codec.raptorq` → `org.apache.hadoop.io.erasurecode.codec.RaptorQErasureCodec`
- `io.erasurecode.codec.raptorq.rawcoders` → `raptorq_java`
- `io.erasurecode.codec.native.enabled` respected (no native RaptorQ here)

### Testing plan
- Unit tests
  - Single erasure ≤ m recovery (data or parity)
  - Multiple erasures up to m
  - ByteBuffer vs byte[] code paths
  - Edge case: partial last symbol with padding
- Integration smoke test
  - Encode group → corrupt some units → decode → byte equality with original

### Risks / mitigations
- Performance: OpenRQ decode may be slower → optimize buffer reuse and avoid copies
- Determinism: Fix ESI selection for parity mapping to ensure stable layout
- Memory: Large buffers → prefer streaming within Hadoop chunking; avoid large temporary arrays
