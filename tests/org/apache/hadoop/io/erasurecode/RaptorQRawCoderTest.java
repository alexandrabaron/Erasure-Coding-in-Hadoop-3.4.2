package org.apache.hadoop.io.erasurecode;

import org.apache.hadoop.io.erasurecode.rawcoder.RaptorQRawEncoder;
import org.apache.hadoop.io.erasurecode.rawcoder.RaptorQRawDecoder;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

public final class RaptorQRawCoderTest {

  private static final SecureRandom RNG = new SecureRandom();

  public static void main(String[] args) throws Exception {
    testByteArrayPath(6, 3, 1024, 2);
    testByteBufferPath(6, 3, 2048, 3);
    System.out.println("OK: RaptorQRawCoder tests passed");
  }

  private static void testByteArrayPath(int k, int m, int T, int numErasures) throws Exception {
    ErasureCoderOptions opts = new ErasureCoderOptions(k, m);
    RaptorQRawEncoder enc = new RaptorQRawEncoder(opts);
    RaptorQRawDecoder dec = new RaptorQRawDecoder(opts);

    byte[][] data = new byte[k][T];
    for (int i = 0; i < k; i++) RNG.nextBytes(data[i]);

    byte[][] parity = new byte[m][T];
    int[] parityOff = new int[m];
    enc.encode(data, parity);

    // Build inputs (data + parity) for decode; erase numErasures random positions
    byte[][] inputs = new byte[k + m][];
    int[] inOff = new int[k + m];
    for (int i = 0; i < k; i++) inputs[i] = data[i];
    for (int i = 0; i < m; i++) inputs[k + i] = parity[i];

    boolean[] erased = new boolean[k + m];
    for (int e = 0; e < numErasures; e++) {
      int idx;
      do { idx = RNG.nextInt(k + m); } while (erased[idx]);
      erased[idx] = true;
      inputs[idx] = null; // indicate erased
    }

    int erasedCount = 0;
    for (boolean b : erased) if (b) erasedCount++;
    int[] erasedIndexes = new int[erasedCount];
    int w = 0;
    for (int i = 0; i < erased.length; i++) if (erased[i]) erasedIndexes[w++] = i;

    // Outputs correspond 1:1 to erased indexes
    byte[][] outputs = new byte[erasedCount][T];
    int[] outOff = new int[erasedCount];

    dec.decode(inputs, erasedIndexes, outputs);

    // Verify recovered
    for (int j = 0; j < erasedCount; j++) {
      int idx = erasedIndexes[j];
      if (idx < k) {
        assertArrayEq(data[idx], 0, outputs[j], 0, T, "byte[] data");
      } else {
        // parity: recompute from original to verify
        // reuse encoder to regenerate parity symbol
        byte[][] regenParity = new byte[m][T];
        enc.encode(data, regenParity);
        int p = idx - k;
        assertArrayEq(regenParity[p], 0, outputs[j], 0, T, "byte[] parity");
      }
    }
  }

  private static void testByteBufferPath(int k, int m, int T, int numErasures) throws Exception {
    ErasureCoderOptions opts = new ErasureCoderOptions(k, m);
    RaptorQRawEncoder enc = new RaptorQRawEncoder(opts);
    RaptorQRawDecoder dec = new RaptorQRawDecoder(opts);

    ByteBuffer[] data = new ByteBuffer[k];
    for (int i = 0; i < k; i++) {
      byte[] arr = new byte[T]; RNG.nextBytes(arr);
      data[i] = ByteBuffer.wrap(arr);
    }

    ByteBuffer[] parity = new ByteBuffer[m];
    for (int i = 0; i < m; i++) parity[i] = ByteBuffer.allocate(T);
    enc.encode(data, parity);

    // Build inputs (data + parity)
    ByteBuffer[] inputs = new ByteBuffer[k + m];
    System.arraycopy(data, 0, inputs, 0, k);
    System.arraycopy(parity, 0, inputs, k, m);

    boolean[] erased = new boolean[k + m];
    for (int e = 0; e < numErasures; e++) {
      int idx;
      do { idx = RNG.nextInt(k + m); } while (erased[idx]);
      erased[idx] = true;
      inputs[idx] = null;
    }

    int erasedCount = 0;
    for (boolean b : erased) if (b) erasedCount++;
    int[] erasedIndexes = new int[erasedCount];
    int w = 0;
    for (int i = 0; i < erased.length; i++) if (erased[i]) erasedIndexes[w++] = i;

    ByteBuffer[] outputs = new ByteBuffer[erasedCount];
    for (int j = 0; j < erasedCount; j++) outputs[j] = ByteBuffer.allocate(T);

    dec.decode(inputs, erasedIndexes, outputs);

    // Verify by regenerating parity and comparing for data/parity
    // Prepare contiguous original data for comparison
    byte[][] dataArr = new byte[k][T];
    for (int i = 0; i < k; i++) {
      data[i].position(0); data[i].get(dataArr[i]); data[i].position(0);
    }
    ByteBuffer[] regenParity = new ByteBuffer[m];
    for (int i = 0; i < m; i++) regenParity[i] = ByteBuffer.allocate(T);
    enc.encode(toBuffers(dataArr), regenParity);

    for (int j = 0; j < erasedCount; j++) {
      int idx = erasedIndexes[j];
      outputs[j].position(0);
      if (idx < k) {
        assertBufferEq(ByteBuffer.wrap(dataArr[idx]), outputs[j], T, "bytebuffer data");
      } else {
        assertBufferEq(regenParity[idx - k], outputs[j], T, "bytebuffer parity");
      }
    }
  }

  private static void assertArrayEq(byte[] a, int ao, byte[] b, int bo, int len, String msg) {
    for (int i = 0; i < len; i++) {
      if (a[ao + i] != b[bo + i]) throw new AssertionError("Mismatch in " + msg + " at byte " + i);
    }
  }

  private static void assertBufferEq(ByteBuffer a, ByteBuffer b, int len, String msg) {
    for (int i = 0; i < len; i++) {
      if (a.get(i) != b.get(i)) throw new AssertionError("Mismatch in " + msg + " at byte " + i);
    }
  }

  private static ByteBuffer[] toBuffers(byte[][] arr) {
    ByteBuffer[] bufs = new ByteBuffer[arr.length];
    for (int i = 0; i < arr.length; i++) bufs[i] = ByteBuffer.wrap(arr[i]);
    return bufs;
  }
}


