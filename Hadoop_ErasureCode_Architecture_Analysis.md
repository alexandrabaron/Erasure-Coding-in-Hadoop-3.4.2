# Detailed Analysis of Hadoop Erasure Coding Architecture

## Overview

Hadoop’s erasure code system is organized into several distinct architectural layers that enable clear separation of concerns and maximum extensibility.

## Layered Architecture

### 1. Codec Layer (codec/)
Role: Primary interface and configuration for erasure codes

#### Main classes:
- `ErasureCodec` (abstract): Base interface for all erasure codes
- `XORErasureCodec`: Simple XOR implementation (k+1, k)
- `HHXORErasureCodec`: Advanced Hitchhiker-XOR implementation
- `DummyErasureCodec`: Test codec with no real computation
- `RSErasureCodec`: Reed-Solomon codec (excluded from this project)

#### Features:
- Defines the erasure schema (number of data and parity blocks)
- Creates the appropriate encoders and decoders
- Manages codec-specific configuration options

### 2. Coder Layer (coder/)
Role: Data preparation and orchestration of the encode/decode process

#### Main classes:
- `ErasureEncoder` (abstract): Interface for encoding
- `ErasureDecoder` (abstract): Interface for decoding
- `ErasureCodingStep`: Interface for coding steps
- `ErasureEncodingStep`: Concrete encoding step
- `ErasureDecodingStep`: Concrete decoding step

#### Features:
- Prepares input and output blocks
- Computes the necessary coding steps
- Encapsulates codec-specific business logic
- Bridges higher-level logic to raw coders

### 3. RawCoder Layer (rawcoder/)
Role: Implementation of the pure math algorithms

#### Main classes:
- `RawErasureEncoder` (abstract): Interface for raw encoding
- `RawErasureDecoder` (abstract): Interface for raw decoding
- `RawErasureCoderFactory`: Factory to create raw coders
- `XORRawEncoder/XORRawDecoder`: Pure XOR implementation
- `DummyRawEncoder/DummyRawDecoder`: Dummy implementation

#### Features:
- Performs the actual math (XOR, RS, etc.)
- Works directly with ByteBuffers and byte[]
- Performance oriented
- Stateless and thread-safe

## Registration System

### CodecRegistry
- Uses ServiceLoader to auto-discover factories
- Maps codec names to available factories
- Handles name conflicts
- Prioritizes native implementations

### Configuration
- Codecs are configured via `CodecUtil`
- Supports custom codecs via configuration
- Automatic fallback across implementations

## Analysis of Existing Implementations

### 1. XOR Codec
Characteristics:
- Simplicity: pure XOR algorithm
- Limitation: single parity block (k+1, k)
- Performance: very fast
- Usage: mostly as a primitive for other codes

Algorithm:
```java
// Encoding: P = D1 ⊕ D2 ⊕ ... ⊕ Dk
// Decoding: Di = P ⊕ D1 ⊕ D2 ⊕ ... ⊕ Di-1 ⊕ Di+1 ⊕ ... ⊕ Dk
```

### 2. HHXOR (Hitchhiker-XOR)
Characteristics:
- Complexity: combines RS and XOR
- Advantage: reduces network traffic by 25-45%
- Research: developed at UC Berkeley
- Usage: advanced code for production environments

Architecture:
- Uses an RS encoder and an XOR encoder
- Sub-packet size of 2
- Optimizes data reconstruction

### 3. Dummy Codec
Characteristics:
- Usage: tests and benchmarks
- Performance: no real computation
- Utility: isolates performance issues

## Data Flow

### Encoding:
1. ECBlockGroup → ErasureEncoder → ErasureEncodingStep
2. ErasureEncodingStep → RawErasureEncoder → Math computations
3. Result: parity blocks generated

### Decoding:
1. ECBlockGroup (with erased blocks) → ErasureDecoder → ErasureDecodingStep
2. ErasureDecodingStep → RawErasureDecoder → Reconstruction
3. Result: erased blocks reconstructed

## Key Data Structures

### ECSchema
- Defines the erasure schema (k, m)
- Codec name
- Extra options

### ErasureCoderOptions
- Number of data and parity blocks
- Performance options (allowChangeInputs, allowVerboseDump)

### ECBlock/ECBlockGroup
- Representation of data and parity blocks
- State management (erased, available)

## Extension Points for a New Codec

### 1. Create the main codec
```java
public class MyCodec extends ErasureCodec {
    @Override
    public ErasureEncoder createEncoder() {
        return new MyEncoder(getCoderOptions());
    }
    
    @Override
    public ErasureDecoder createDecoder() {
        return new MyDecoder(getCoderOptions());
    }
}
```

### 2. Create encoders/decoders
```java
public class MyEncoder extends ErasureEncoder {
    @Override
    protected ErasureCodingStep prepareEncodingStep(ECBlockGroup blockGroup) {
        RawErasureEncoder rawEncoder = CodecUtil.createRawEncoder(
            getConf(), "my_codec", getOptions());
        return new ErasureEncodingStep(
            getInputBlocks(blockGroup),
            getOutputBlocks(blockGroup),
            rawEncoder);
    }
}
```

### 3. Create raw coders
```java
public class MyRawEncoder extends RawErasureEncoder {
    @Override
    protected void doEncode(ByteArrayEncodingState encodingState) {
        // Implement the math algorithm here
    }
    
    @Override
    protected void doEncode(ByteBufferEncodingState encodingState) {
        // Optimized ByteBuffer version
    }
}
```

### 4. Create the factory
```java
public class MyRawErasureCoderFactory implements RawErasureCoderFactory {
    @Override
    public RawErasureEncoder createEncoder(ErasureCoderOptions coderOptions) {
        return new MyRawEncoder(coderOptions);
    }
    
    @Override
    public RawErasureDecoder createDecoder(ErasureCoderOptions coderOptions) {
        return new MyRawDecoder(coderOptions);
    }
    
    @Override
    public String getCoderName() {
        return "my_codec_java";
    }
    
    @Override
    public String getCodecName() {
        return "my_codec";
    }
}
```

### 5. Registration
- Add codec name in `ErasureCodeConstants`
- Configure in `CodecUtil.getCodecClassName()`
- Create the META-INF/services file for ServiceLoader

## Recommendations for a New Codec

### 1. Algorithm choices
- Cauchy Reed-Solomon: good performance/efficiency tradeoff
- LDPC: effective for large block sizes
- RaptorQ: optimized for streaming

### 2. Performance considerations
- Implement both ByteBuffer and byte[] versions
- Optimize for common cases
- Manage memory efficiently

### 3. Tests and validation
- Comprehensive unit tests
- Performance benchmarks
- Mathematical validation

## Conclusion

Hadoop’s erasure coding architecture is well designed with clear separation of concerns. It allows easy addition of new algorithms while maintaining compatibility and performance. The automatic registration system via ServiceLoader greatly simplifies integration of new codecs.

## Concrete Integration Points in this repository

- `erasurecode/ErasureCodeConstants.java`
  - Add a codec name (e.g., `raptorq`) and practical schemas (k, m) for tests.
- `erasurecode/CodecUtil.java`
  - Add the mapping from codec name to concrete `ErasureCodec` class (`io.erasurecode.codec.raptorq`).
  - Allow configuring raw coders via `io.erasurecode.codec.raptorq.rawcoders`.
- `erasurecode/codec/` (new class)
  - `RaptorQErasureCodec` extends `ErasureCodec` and creates `RaptorQEncoder` / `RaptorQDecoder`.
- `erasurecode/coder/` (new classes)
  - `RaptorQEncoder` and `RaptorQDecoder` extend `ErasureEncoder` / `ErasureDecoder` and delegate to raw coders.
- `erasurecode/rawcoder/` (new classes)
  - `RaptorQRawEncoder` / `RaptorQRawDecoder` use OpenRQ to encode/decode symbols from ByteBuffer/byte[].
  - `RaptorQRawErasureCoderFactory` for ServiceLoader.
- `erasurecode/META-INF/services/org.apache.hadoop.io.erasurecode.rawcoder.RawErasureCoderFactory`
  - Add `org.apache.hadoop.io.erasurecode.rawcoder.RaptorQRawErasureCoderFactory`.

## Conceptual differences RS vs RaptorQ to consider

- Symbol system: RS works on fixed k data and m parity; RaptorQ generates an arbitrary number of repair symbols. For Hadoop (expects fixed m parities), we configure m deterministic repair symbols (first m ESIs) during encoding.
- Decoding: Hadoop provides a list of erased units to reconstruct; RaptorQ requires ≥ k arbitrary symbols to solve. We feed the decoder a minimal set of available symbols (k) and reconstruct the precise erased indices.
- Unit size: Align Hadoop chunk size with OpenRQ `symbolSize`. Add padding if needed and strip it after decoding.
- State/buffers: Support both `byte[]` and `ByteBuffer` in `RawErasure{En,De}coder` for performance.

## RaptorQ implementation added in this project

- `erasurecode/ErasureCodeConstants.java`
  - Added `raptorq` codec and example schemas (6,3) and (10,4).
- `erasurecode/CodecUtil.java`
  - Resolves `io.erasurecode.codec.raptorq` to `codec.RaptorQErasureCodec`.
- `erasurecode/codec/RaptorQErasureCodec.java`
  - Creates `coder.RaptorQEncoder` / `coder.RaptorQDecoder`.
- `erasurecode/coder/RaptorQEncoder.java` / `RaptorQDecoder.java`
  - Adapters delegating to raw coders via `CodecUtil.createRawEncoder/Decoder`.
- `erasurecode/rawcoder/RaptorQRawErasureCoderFactory.java`
  - ServiceLoader: `coderName=raptorq_java`, `codecName=raptorq`.
- `erasurecode/rawcoder/RaptorQRawEncoder.java`
  - Concatenates k inputs (size T), `FECParameters(F=k*T, T, Z=1)`, generates m repair symbols ESI `K..K+m-1`, writes to outputs.
- `erasurecode/rawcoder/RaptorQRawDecoder.java`
  - Feeds OpenRQ with all available symbols; on decode success, copies recovered data to outputs; erased parities regenerated by re-encoding.
- `erasurecode/META-INF/services/...RawErasureCoderFactory`
  - Registers the RaptorQ factory.

### Points of attention / risks
- Assumes equal size T across all units in a group. If not, add explicit padding.
- RaptorQ may fail at N=K (probabilistic). Error handling included in raw decoder (IOException on failure); improvement: configurable overhead margin.
- Parity regeneration in decoding is performed by re-encoding the reconstructed data for ESI `K+index`.
- Memory: current implementation concatenates `k*T`; possible optimization by streaming.