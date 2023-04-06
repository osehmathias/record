package com.llfbandit.record.record.processor;

import androidx.annotation.NonNull;

import com.llfbandit.record.record.RecordConfig;

import java.io.IOException;

public class WavProcessor extends PcmProcessor {
  private int audioDataLength = 0;

  public WavProcessor(@NonNull OnAudioProcessorListener listener) {
    super(listener);
  }

  @Override
  public void onBegin(RecordConfig config) throws Exception {
    if (config.path == null) {
      throw new Exception("Wav must be recorded into file. Given path is null.");
    }

    // Prepares WAV header & avoids data overwrites.
    writeHeader();
  }

  @Override
  public void onEnd() throws Exception {
    writeHeader();
  }

  @Override
  protected void onAudioChunk(byte[] chunk, int length) throws Exception {
    audioDataLength += length;
    // Write to file
    assert out != null;
    out.write(chunk, 0, length);
  }

  private void writeHeader() throws IOException {
// Offset  Size  Name             Description

// The canonical WAVE format starts with the RIFF header:

// 0         4   ChunkID          Contains the letters "RIFF" in ASCII form
//                                (0x52494646 big-endian form).
// 4         4   ChunkSize        36 + SubChunk2Size, or more precisely:
//                                4 + (8 + SubChunk1Size) + (8 + SubChunk2Size)
//                                This is the size of the rest of the chunk
//                                following this number.  This is the size of the
//                                entire file in bytes minus 8 bytes for the
//                                two fields not included in this count:
//                                ChunkID and ChunkSize.
// 8         4   Format           Contains the letters "WAVE"
//                                (0x57415645 big-endian form).

// The "WAVE" format consists of two subchunks: "fmt " and "data":
// The "fmt " subchunk describes the sound data's format:

// 12        4   Subchunk1ID      Contains the letters "fmt "
//                                (0x666d7420 big-endian form).
// 16        4   Subchunk1Size    16 for PCM.  This is the size of the
//                                rest of the Subchunk which follows this number.
// 20        2   AudioFormat      PCM = 1 (i.e. Linear quantization)
//                                Values other than 1 indicate some
//                                form of compression.
// 22        2   NumChannels      Mono = 1, Stereo = 2, etc.
// 24        4   SampleRate       8000, 44100, etc.
// 28        4   ByteRate         == SampleRate * NumChannels * BitsPerSample/8
// 32        2   BlockAlign       == NumChannels * BitsPerSample/8
//                                The number of bytes for one sample including
//                                all channels. I wonder what happens when
//                                this number isn't an integer?
// 34        2   BitsPerSample    8 bits = 8, 16 bits = 16, etc.
//           2   ExtraParamSize   if PCM, then doesn't exist
//           X   ExtraParams      space for extra parameters

// The "data" subchunk contains the size of the data and the actual sound:

// 36        4   Subchunk2ID      Contains the letters "data"
//                                (0x64617461 big-endian form).
// 40        4   Subchunk2Size    == NumSamples * NumChannels * BitsPerSample/8
//                                This is the number of bytes in the data.
//                                You can also think of this as the size
//                                of the read of the subchunk following this
//                                number.
// 44        *   Data             The actual sound data.

    short bitsPerSample = config.encoder.equals("pcm16bit") ? (short) 16 : (short) 8;

    assert out != null;
    out.seek(0);
    out.writeBytes("RIFF"); // ChunkID
    out.writeInt(Integer.reverseBytes(36 + audioDataLength)); // ChunkSize
    out.writeBytes("WAVE"); // Format
    out.writeBytes("fmt "); // Subchunk1ID
    out.writeInt(Integer.reverseBytes(16)); // Subchunk1Size
    out.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
    out.writeShort(Short.reverseBytes((short) config.numChannels)); // NumChannels
    out.writeInt(Integer.reverseBytes(config.samplingRate)); // SampleRate
    out.writeInt(Integer.reverseBytes(config.samplingRate * config.numChannels * bitsPerSample / 8)); // ByteRate
    out.writeShort(Short.reverseBytes((short) (config.numChannels * bitsPerSample / 8))); // BlockAlign
    out.writeShort(Short.reverseBytes(bitsPerSample)); // BitsPerSample
    out.writeBytes("data"); // Subchunk2ID
    out.writeInt(Integer.reverseBytes(audioDataLength)); // Subchunk2Size
  }
}