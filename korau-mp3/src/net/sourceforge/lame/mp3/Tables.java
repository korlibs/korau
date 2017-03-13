package net.sourceforge.lame.mp3;

public interface Tables {
    int bitrate_table[][] = {
            {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1}, /* MPEG 2 */
            {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, -1}, /* MPEG 1 */
            {0, 8, 16, 24, 32, 40, 48, 56, 64, -1, -1, -1, -1, -1, -1, -1}, /* MPEG 2.5 */
    };

    int samplerate_table[][] = {
            {22050, 24000, 16000, -1},
            {44100, 48000, 32000, -1},
            {11025, 12000, 8000, -1},
    };
}
