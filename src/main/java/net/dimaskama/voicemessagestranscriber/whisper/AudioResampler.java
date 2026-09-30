package net.dimaskama.voicemessagestranscriber.whisper;

import java.util.List;

public final class AudioResampler {

    private static final int DECIMATION = 3;
    private static final int OUTPUT_SAMPLE_RATE = 16000;
    private static final int MIN_OUTPUT_SAMPLES = OUTPUT_SAMPLE_RATE + OUTPUT_SAMPLE_RATE / 10;
    private static final float[] FILTER = createLowPassFilter(47, 7200.0 / 48000.0);

    private AudioResampler() {
    }

    public static float[] toWhisperInput(List<short[]> frames) {
        int inputLength = 0;
        for (short[] frame : frames) {
            inputLength += frame.length;
        }
        float[] input = new float[inputLength];
        int pos = 0;
        for (short[] frame : frames) {
            for (short sample : frame) {
                input[pos++] = sample / 32768.0F;
            }
        }

        int outputLength = inputLength / DECIMATION;
        float[] output = new float[Math.max(outputLength, MIN_OUTPUT_SAMPLES)];
        int half = FILTER.length / 2;
        for (int i = 0; i < outputLength; i++) {
            int center = i * DECIMATION;
            float acc = 0.0F;
            for (int k = 0; k < FILTER.length; k++) {
                int index = center + k - half;
                if (index >= 0 && index < inputLength) {
                    acc += FILTER[k] * input[index];
                }
            }
            output[i] = acc;
        }
        return output;
    }

    private static float[] createLowPassFilter(int taps, double cutoff) {
        float[] filter = new float[taps];
        int m = taps - 1;
        double sum = 0.0;
        for (int i = 0; i < taps; i++) {
            double x = i - m / 2.0;
            double sinc = x == 0.0 ? 2.0 * cutoff : Math.sin(2.0 * Math.PI * cutoff * x) / (Math.PI * x);
            double window = 0.42 - 0.5 * Math.cos(2.0 * Math.PI * i / m) + 0.08 * Math.cos(4.0 * Math.PI * i / m);
            filter[i] = (float) (sinc * window);
            sum += filter[i];
        }
        for (int i = 0; i < taps; i++) {
            filter[i] /= (float) sum;
        }
        return filter;
    }

}
