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


### Diagram (Mermaid)

```mermaid
flowchart TD
    subgraph Sender[Sender]
      A[Source Data (bytes)] -->|Partition| B[Source Blocks]
      B -->|Split into symbols of size T| C[Source Symbols (K)]
      C -->|OpenRQ::DataEncoder| D[SourceBlockEncoder]
      D -->|Emit K source packets (ESI 0..K-1)| E1[Source Packets]
      D -->|Emit m repair packets (ESI K..K+m-1)| E2[Repair Packets]
    end

    E1 -.->|Any order / any subset| Net((Network))
    E2 -.->|Any order / any subset| Net

    subgraph Receiver[Receiver]
      Net -.-> F[Packets Arrive]
      F -->|Parse & Validate| G[OpenRQ::DataDecoder]
      G --> H[SourceBlockDecoder]
      H -->|If ≥ K independent symbols| I[Decode (linear system)]
      I --> J[Recovered Source Symbols]
      J --> K[Reassembled Source Block]
      K --> L[Concatenate Blocks]
      L --> M[Reconstructed Source Data]
    end

    classDef comp fill:#eef,stroke:#446
    classDef data fill:#efe,stroke:#484
    class A,B,C,K,L,M comp
    class E1,E2,F data

    %% Hadoop adapter mapping
    subgraph HadoopAdapter[Hadoop Adapter]
      HA1[Inputs: k data chunks of size T] -->|Concatenate| HA2[k*T contiguous]
      HA2 -->|Encode m repair ESIs K..K+m-1| HA3[m parity chunks]
      HB1[Inputs: data+parity with erasures] -->|Feed available packets| HB2[Decode]
      HB2 -->|Recovered data| HB3[Fill erased data chunks]
      HB2 -->|Regenerate parity via re-encode| HB4[Fill erased parity chunks]
    end
```


