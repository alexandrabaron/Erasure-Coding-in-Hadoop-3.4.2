# RaptorQ Encoding Flow in Hadoop

```mermaid
sequenceDiagram
    participant HDFS as HDFS Storage
    participant EC as ErasureCodec
    participant Enc as RaptorQEncoder
    participant RawEnc as RaptorQRawEncoder
    participant OpenRQ as OpenRQ Library
    participant Par as Parity Blocks
    
    HDFS->>EC: Encode k data blocks
    EC->>Enc: createEncoder()
    Enc->>RawEnc: createRawEncoder()
    
    Note over RawEnc: Concatenate k inputs<br/>Size: k × T bytes
    
    RawEnc->>OpenRQ: FECParameters(k×T, T, 1)
    RawEnc->>OpenRQ: newEncoder(data, params)
    RawEnc->>OpenRQ: sourceBlock(0)
    
    loop For m parity blocks
        RawEnc->>OpenRQ: repairPacket(ESI = k+i)
        OpenRQ-->>RawEnc: EncodingPacket with symbols
        RawEnc->>Par: Write T bytes to parity[i]
    end
    
    RawEnc-->>Enc: Encoding complete
    Enc-->>EC: Return parity blocks
    EC-->>HDFS: Store k data + m parity blocks
```

