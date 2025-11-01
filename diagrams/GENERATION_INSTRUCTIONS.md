# Instructions for Generating PNG Images from Mermaid Diagrams

Follow these steps to convert the Mermaid diagrams to PNG images for the LaTeX report.

## Option 1: Using Mermaid CLI (Recommended)

### Installation
```bash
npm install -g @mermaid-js/mermaid-cli
```

### Generate Images
```bash
# Navigate to the diagrams directory
cd diagrams

# Generate all images
mmdc -i hadoop_architecture.md -o hadoop_architecture.png -t neutral -b white -w 1440
mmdc -i raptorq_encoding.md -o raptorq_encoding.png -t neutral -b white -w 1440
mmdc -i raptorq_decoding.md -o raptorq_decoding.png -t neutral -b white -w 1440
mmdc -i esi_mapping.md -o esi_mapping.png -t neutral -b white -w 1440
mmdc -i test_scenarios.md -o test_scenarios.png -t neutral -b white -w 1440
```

**Parameters:**
- `-i`: Input Mermaid file
- `-o`: Output PNG file
- `-t neutral`: Neutral color theme
- `-b white`: White background
- `-w 1440`: Width for high-quality output

## Option 2: Using Online Mermaid Live Editor

1. Open [https://mermaid.live](https://mermaid.live)

2. For each diagram file:
   - Copy the Mermaid code from the `.md` file (the part between the three backticks)
   - Paste into the editor
   - Wait for rendering
   - Click "PNG" button to export
   - Save with the corresponding filename (e.g., `hadoop_architecture.png`)

## Option 3: Using VS Code

### Installation
1. Install the "Markdown Preview Mermaid Support" extension
2. Optional: Install "Mermaid Preview" extension

### Generate Images
1. Open a `.md` file (e.g., `hadoop_architecture.md`)
2. Right-click on the diagram in preview
3. Select "Export..." or "Save as Image"
4. Save as PNG

## Option 4: Using Docker

```bash
# Pull the Mermaid CLI Docker image
docker pull minlag/mermaid-cli

# Generate images
docker run -it -v $(pwd)/diagrams:/data minlag/mermaid-cli \
  mmdc -i /data/hadoop_architecture.md -o /data/hadoop_architecture.png -t neutral -b white

docker run -it -v $(pwd)/diagrams:/data minlag/mermaid-cli \
  mmdc -i /data/raptorq_encoding.md -o /data/raptorq_encoding.png -t neutral -b white

docker run -it -v $(pwd)/diagrams:/data minlag/mermaid-cli \
  mmdc -i /data/raptorq_decoding.md -o /data/raptorq_decoding.png -t neutral -b white

docker run -it -v $(pwd)/diagrams:/data minlag/mermaid-cli \
  mmdc -i /data/esi_mapping.md -o /data/esi_mapping.png -t neutral -b white

docker run -it -v $(pwd)/diagrams:/data minlag/mermaid-cli \
  mmdc -i /data/test_scenarios.md -o /data/test_scenarios.png -t neutral -b white
```

## Expected Output

After running the commands, you should have these files in the `diagrams/` directory:
- hadoop_architecture.png
- raptorq_encoding.png
- raptorq_decoding.png
- esi_mapping.png
- test_scenarios.png

## Troubleshooting

### Mermaid CLI not found
Make sure Node.js is installed and npm is in your PATH.

### Syntax errors in Mermaid
Check the `.md` files - the Mermaid code should be between triple backticks with language specified as "mermaid".

### Low-quality images
Increase the width with `-w` parameter (e.g., `-w 2880` for 2x quality).

### Colors not rendering correctly
Try different themes: `-t default`, `-t dark`, or `-t forest`.

## Quick Reference

Mermaid diagram files:
1. `hadoop_architecture.md` → Architecture overview
2. `raptorq_encoding.md` → Encoding flow
3. `raptorq_decoding.md` → Decoding flow
4. `esi_mapping.md` → Symbol mapping
5. `test_scenarios.md` → Test strategy

LaTeX expects images in:
- `rapport/diagrams/` or `rapport/diagrams/` (relative to main.tex)

Make sure the PNG files are in the correct location before compiling the LaTeX document.

