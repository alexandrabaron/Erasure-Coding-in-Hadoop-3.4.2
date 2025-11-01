# Mermaid Diagrams for RaptorQ Implementation Report

This directory contains Mermaid diagrams to be converted to PNG images for inclusion in the LaTeX report.

## Diagram Files

### 1. hadoop_architecture.md
Shows the three-layer Hadoop erasure coding architecture with RaptorQ integration:
- Codec layer (RaptorQErasureCodec)
- Coder layer (RaptorQEncoder/Decoder)
- RawCoder layer (RaptorQRawEncoder/Decoder with OpenRQ)
- Relationship to mathematical libraries

**Usage**: Figure \ref{fig:architecture} in Section "Hadoop Erasure Coding Architecture"

### 2. raptorq_encoding.md
Sequence diagram showing the encoding flow:
- Data chunk concatenation
- OpenRQ encoder initialization
- Repair symbol generation
- Parity block output

**Usage**: Figure \ref{fig:encoding} in Section "Implementation"

### 3. raptorq_decoding.md
Sequence diagram showing the decoding flow:
- Symbol feeding to decoder
- Source data reconstruction
- Parity regeneration via re-encoding

**Usage**: Figure \ref{fig:decoding} in Section "Implementation"

### 4. esi_mapping.md
Visual mapping between Hadoop block model and RaptorQ symbol model:
- Data blocks (D0-D5) → Source symbols (ESI 0-5)
- Parity blocks (P0-P2) → Repair symbols (ESI 6-8)
- Unused additional repair symbols

**Usage**: Figure \ref{fig:esi-mapping} in Section "RaptorQ and OpenRQ Library"

### 5. test_scenarios.md
Test strategy visualization:
- Byte array vs ByteBuffer paths
- Test scenarios (data erasure, parity erasure, mixed)
- Validation strategies

**Usage**: Figure \ref{fig:testing} in Section "Testing and Validation"

## How to Generate Images

### Using Mermaid CLI
```bash
# Install Mermaid CLI
npm install -g @mermaid-js/mermaid-cli

# Convert to PNG
mmdc -i hadoop_architecture.md -o hadoop_architecture.png -t neutral -b white
mmdc -i raptorq_encoding.md -o raptorq_encoding.png -t neutral -b white
mmdc -i raptorq_decoding.md -o raptorq_decoding.png -t neutral -b white
mmdc -i esi_mapping.md -o esi_mapping.png -t neutral -b white
mmdc -i test_scenarios.md -o test_scenarios.png -t neutral -b white
```

### Using Online Tools
1. Open [Mermaid Live Editor](https://mermaid.live)
2. Copy the Mermaid code from each .md file
3. Export as PNG
4. Save with the corresponding name

### Using VS Code Extension
1. Install "Markdown Preview Mermaid Support" extension
2. Preview the .md files
3. Right-click and export as image

## Image Requirements for LaTeX

- Format: PNG
- Background: White
- Theme: neutral (or similar light theme)
- Size: The LaTeX includes them with appropriate width settings
  - Architecture: 0.9\textwidth
  - ESI mapping: 0.8\textwidth
  - Encoding/Decoding: \textwidth
  - Testing: 0.9\textwidth

## Notes

- The diagrams use Mermaid syntax which is supported by many tools
- Colors are used to distinguish between different components and layers
- All diagrams are self-contained in their .md files
- RaptorQ-specific elements are highlighted in green (#90EE90)
- OpenRQ library is highlighted in pink (#FFB6C1)

