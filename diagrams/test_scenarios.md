# Testing Strategy and Scenarios

```mermaid
graph TB
    subgraph TestSuite[RaptorQRawCoderTest]
        TS1[Unit Test Suite]
        
        subgraph ByteArray[byte[] API Tests]
            BA1[Test 1: k=6, m=3, T=1024<br/>2 erasures<br/>Random data]
            BA2[Encode k data blocks]
            BA3[Generate m parity blocks]
            BA4[Erase 2 random blocks]
            BA5[Decode and verify<br/>Byte-level equality]
        end
        
        subgraph ByteBuffer[ByteBuffer API Tests]
            BB1[Test 2: k=6, m=3, T=2048<br/>3 erasures<br/>Random data]
            BB2[Encode k data blocks]
            BB3[Generate m parity blocks]
            BB4[Erase 3 random blocks]
            BB5[Decode and verify<br/>Byte-level equality]
        end
        
        subgraph Scenarios[Test Scenarios]
            S1[Data block erasure<br/>Verify recovery]
            S2[Parity block erasure<br/>Verify regeneration]
            S3[Mixed erasures<br/>Data + Parity]
            S4[Multiple erasures<br/>Up to m blocks]
        end
        
        subgraph Validation[Verification]
            V1[Parity correctness<br/>Regenerate and compare]
            V2[Data recovery<br/>Original vs recovered]
            V3[ByteBuffer compatibility]
            V4[Random data fuzzing<br/>Secure random generator]
        end
    end
    
    TS1 --> ByteArray
    TS1 --> ByteBuffer
    TS1 --> Scenarios
    
    ByteArray --> Validation
    ByteBuffer --> Validation
    Scenarios --> Validation
    
    BA1 --> BA2 --> BA3 --> BA4 --> BA5
    BB1 --> BB2 --> BB3 --> BB4 --> BB5
    
    Validation --> Pass[All Tests Pass ✓]
    
    style Pass fill:#4CAF50,color:#fff
    style BA1 fill:#E3F2FD
    style BB1 fill:#E3F2FD
    style S1 fill:#FFF9C4
    style S2 fill:#FFF9C4
    style S3 fill:#FFF9C4
    style S4 fill:#FFF9C4
```

