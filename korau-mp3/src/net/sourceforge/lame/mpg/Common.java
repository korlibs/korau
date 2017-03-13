/**
 * Some common bitstream operations
 * <p>
 * Copyright (C) 1999-2010 The L.A.M.E. project
 * <p>
 * Initially written by Michael Hipp, see also AUTHORS and README.
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * @author Ken H�ndel
 */
package net.sourceforge.lame.mpg;

import net.sourceforge.lame.mpg.MPGLib.mpstr_tag;

public class Common {

    public static final int tabsel_123[][][] = {
            {
                    {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448,},
                    {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384,},
                    {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320,}
            },
            {
                    {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256,},
                    {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160,},
                    {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160,}
            }
    };

    public static final int freqs[] = {44100, 48000, 32000, 22050, 24000, 16000, 11025, 12000, 8000};
    private static final int MAX_INPUT_FRAMESIZE = 4096;
    public float muls[][] = new float[27][64];

    public final boolean head_check(final long head, final int check_layer) {
		/* bits 13-14 = layer 3 */
        int nLayer = (int) (4 - ((head >> 17) & 3));

        if ((head & 0xffe00000L) != 0xffe00000L) return false; /* syncword */
        if (nLayer == 4) return false;
        if (check_layer > 0 && nLayer != check_layer) return false;
        if (((head >> 12) & 0xf) == 0xf) return false;/* bits 16,17,18,19 = 1111 invalid bitrate */
        if (((head >> 10) & 0x3) == 0x3) return false; /* bits 20,21 = 11 invalid sampling freq */
        if ((head & 0x3) == 0x2) return false;/* invalid emphasis */

        return true;
    }

    /**
     * decode a header and write the information into the frame structure
     */
    public final int decode_header(final Frame fr, final long newhead) {

        if ((newhead & (1 << 20)) != 0) {
            fr.lsf = (newhead & (1 << 19)) != 0 ? 0x0 : 0x1;
            fr.mpeg25 = false;
        } else {
            fr.lsf = 1;
            fr.mpeg25 = true;
        }

        fr.lay = (int) (4 - ((newhead >> 17) & 3));
        if (((newhead >> 10) & 0x3) == 0x3) {
            throw new RuntimeException("Stream error");
        }
        if (fr.mpeg25) {
            fr.sampling_frequency = (int) (6 + ((newhead >> 10) & 0x3));
        } else {
            fr.sampling_frequency = (int) (((newhead >> 10) & 0x3) + (fr.lsf * 3));
        }

        fr.error_protection = ((newhead >> 16) & 0x1) == 0;

        if (fr.mpeg25) fr.bitrate_index = (int) ((newhead >> 12) & 0xf); /* allow Bitrate change for 2.5 ... */

        fr.bitrate_index = (int) ((newhead >> 12) & 0xf);
        fr.padding = (int) ((newhead >> 9) & 0x1);
        fr.extension = (int) ((newhead >> 8) & 0x1);
        fr.mode = (int) ((newhead >> 6) & 0x3);
        fr.mode_ext = (int) ((newhead >> 4) & 0x3);
        fr.copyright = (int) ((newhead >> 3) & 0x1);
        fr.original = (int) ((newhead >> 2) & 0x1);
        fr.emphasis = (int) (newhead & 0x3);

        fr.stereo = (fr.mode == MPG123.MPG_MD_MONO) ? 1 : 2;

        switch (fr.lay) {
            case 1:
                fr.framesize = tabsel_123[fr.lsf][0][fr.bitrate_index] * 12000;
                fr.framesize /= freqs[fr.sampling_frequency];
                fr.framesize = ((fr.framesize + fr.padding) << 2) - 4;
                fr.down_sample = 0;
                fr.down_sample_sblimit = MPG123.SBLIMIT >> (fr.down_sample);
                break;

            case 2:
                fr.framesize = tabsel_123[fr.lsf][1][fr.bitrate_index] * 144000;
                fr.framesize /= freqs[fr.sampling_frequency];
                fr.framesize += fr.padding - 4;
                fr.down_sample = 0;
                fr.down_sample_sblimit = MPG123.SBLIMIT >> (fr.down_sample);
                break;

            case 3:
                if (fr.framesize > MAX_INPUT_FRAMESIZE) {
                    System.err.printf("Frame size too big.\n");
                    fr.framesize = MAX_INPUT_FRAMESIZE;
                    return (0);
                }

                if (fr.bitrate_index == 0)
                    fr.framesize = 0;
                else {
                    fr.framesize = tabsel_123[fr.lsf][2][fr.bitrate_index] * 144000;
                    fr.framesize /= freqs[fr.sampling_frequency] << (fr.lsf);
                    fr.framesize = fr.framesize + fr.padding - 4;
                }
                break;
            default:
                System.err.printf("Sorry, layer %d not supported\n", fr.lay);
                return (0);
        }
        /* print_header(fr); */

        return 1;
    }

    public final int getbits(final mpstr_tag mp, final int number_of_bits) {
        long rval;

        if (number_of_bits <= 0 || null == mp.wordpointer) return 0;


        rval = mp.wordpointer[mp.wordpointerPos + 0] & 0xff;
        rval <<= 8;
        rval |= mp.wordpointer[mp.wordpointerPos + 1] & 0xff;
        rval <<= 8;
        rval |= mp.wordpointer[mp.wordpointerPos + 2] & 0xff;
        rval <<= mp.bitindex;
        rval &= 0xffffffL;

        mp.bitindex += number_of_bits;

        rval >>= (24 - number_of_bits);

        mp.wordpointerPos += (mp.bitindex >> 3);
        mp.bitindex &= 7;

        return (int) rval;
    }

    public final int getbits_fast(final mpstr_tag mp, final int number_of_bits) {
        long rval;

        rval = mp.wordpointer[mp.wordpointerPos + 0] & 0xff;
        rval <<= 8;
        rval |= mp.wordpointer[mp.wordpointerPos + 1] & 0xff;
        rval <<= mp.bitindex;
        rval &= 0xffffL;
        mp.bitindex += number_of_bits;

        rval >>= (16 - number_of_bits);

        mp.wordpointerPos += (mp.bitindex >> 3);
        mp.bitindex &= 7;

        return (int) rval;
    }

    public final int set_pointer(final mpstr_tag mp, final int backstep) {
        if (mp.fsizeold < 0 && backstep > 0) {
            System.err.printf("hip: Can't step back %d bytes!\n", backstep);
            return MPGLib.MP3_ERR;
        }
        byte[] bsbufold = mp.bsspace[1 - mp.bsnum];
        int bsbufoldPos = 512;
        mp.wordpointerPos -= backstep;
        if (backstep != 0)
            System.arraycopy(bsbufold, bsbufoldPos + mp.fsizeold - backstep,
                    mp.wordpointer, mp.wordpointerPos, backstep);
        mp.bitindex = 0;
        return MPGLib.MP3_OK;
    }
}