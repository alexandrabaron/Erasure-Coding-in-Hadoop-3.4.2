## OpenRQ (RaptorQ) – Architecture and Usage

### Key concepts
- RFC 6330 (RaptorQ): Systematic fountain code; can generate an unlimited number of repair symbols.
- Source block / sub-block: OpenRQ partitions data into blocks; sub-block interleaving is not implemented (max 1).
- Symbols: Each block is split into symbols of fixed size `symbolSize`.
- FEC parameters: Encapsulates `dataLength`, `symbolSize`, `numberOfSourceBlocks`.

### Main API (key files)
- `net.fec.openrq.OpenRQ`: Entry point; creates encoders/decoders via `FECParameters`.
- `parameters.FECParameters`: FEC parameters (data length, symbol size, number of source blocks).
- `encoder.DataEncoder` / `encoder.SourceBlockEncoder`: Encoding; produces `EncodingPacket` (source or repair).
- `decoder.DataDecoder` / `decoder.SourceBlockDecoder`: Decoding; consumes `EncodingPacket` in any order; yields block bytes.
- `EncodingPacket` / `SerializablePacket`: Transmittable packets.

### Encoding (typical usage)
1. Build `FECParameters` with `dataLength`, `symbolSize`, `numberOfSourceBlocks`.
2. Create `DataEncoder` via `OpenRQ.newEncoder(data, fecParams)`.
3. For each `SourceBlockEncoder`:
   - Emit `k` source packets and, if needed, `m` deterministic repair packets (e.g., ESI = 0..m-1).

### Decoding (typical usage)
1. Create `DataDecoder` via `OpenRQ.newDecoder(fecParams, symbolOverhead)`.
2. Feed received packets (source and/or repair) to the `SourceBlockDecoder`.
3. When the number of independent symbols ≥ `k`, reconstruct the block and obtain the bytes.

### Hadoop integration (adaptation)
- Hadoop expects fixed `k` data + `m` parity units. With OpenRQ, choose the first `m` repair symbols to form the `m` “parities”.
- On Hadoop encoding, `RaptorQRawEncoder` generates these `m` repair symbols for each group of `k` chunks.
- On Hadoop decoding, `RaptorQRawDecoder` collects ≥ `k` available symbols (data+repair) and reconstructs missing chunks.
- Align Hadoop `chunkSize` and OpenRQ `symbolSize`; if needed, pad trailing symbols and remove padding after reconstruction.

### Implementation details (in this project)
- Encoding (byte[] and ByteBuffer):
  - Concatenate `k` inputs of size `T`; `FECParameters(F=k*T, T, Z=1)`; generate `m` repair packets with ESI `K..K+m-1`; write `T` bytes into parity outputs.
- Decoding (byte[] and ByteBuffer):
  - Instantiate a decoder with `FECParameters(F=k*T, T, Z=1)`; feed the `SourceBlockDecoder` with all available symbols (source ESI `0..K-1`, repair ESI `K..K+m-1`).
  - If the source block decodes, copy the `T` recovered bytes for each erased data unit. For erased parities, regenerate via re-encoding of the recovered data with ESI `K+index`.

### Pitfalls/limitations to track
- ESI mapping: inputs `i<k` map to ESI `i`; parities to `K..K+m-1`. Changing this breaks compatibility of outputs.
- Constant size `T`: assumes each Hadoop chunk in a group has the same length; validate up front. For partial last symbols, add zero padding (future enhancement).
- Probabilistic: RaptorQ can fail at `N=K`; we use overhead=0 and require `K` independent symbols. On rare failure, handle error and retry with an extra repair symbol.
- Memory: current path concatenates `k` inputs into one buffer; for very large `T`, consider chunked processing.

### References
- RFC 6330 — RaptorQ Fountain Code
- Code: `OpenRQ-master/src/main/net/fec/openrq/*`


