package ir;

import java.util.*;

public class Utils {

    static public byte[] toByteArray(int i) {
        byte bytes[] = new byte[4];
        bytes[0] = (byte)((i >>> 24) & 0xff);
        bytes[1] = (byte)((i >>> 16) & 0xff);
        bytes[2] = (byte)((i >>> 8) & 0xff);
        bytes[3] = (byte)(i & 0xff);
        return bytes;
    }

    static public byte[] toByteArray(short i) {
        byte bytes[] = new byte[2];
        bytes[0] = (byte)((i >>> 8) & 0xff);
        bytes[1] = (byte)(i & 0xff);
        return bytes;
    }

    static public void toByteArrayAndAppend(int data, byte[] src, int toIndex) {
        byte dest[] = toByteArray(data);
        System.arraycopy(dest, 0, src, toIndex, dest.length);
    }

    static public void toIrPatterns(int[] irData, byte[] patterns) {
        if (irData == null) {
            throw new NullPointerException();
        }

        int toIndex = 0;

        for (int data: irData) {
            toByteArrayAndAppend(data, patterns, toIndex);
            toIndex += 4;
        }
    }

    /*
        Index:
        0:  header 2.4ms (Wave Carrier)
        1:  600us  (Wave Carrier)
        2:  1.2ms  (Wave Carrier)
        3:  600us  (Low)
     */
    static public void dataToIndexSONY(boolean[] a, byte[] data, int toIndex) {
        int i = toIndex;

        for (boolean b: a) {
            if (b) {
                data[i] = (2 << 4) | 3;
            } else {
                data[i] = (1 << 4) | 3;
            }
            i++;
        }   
    }

    /*
        Index:
        0:  header (Carrier waveform	)
        1:  1690us  (Header low)
        2:  420us  (Carrier waveform)
        3:  0.84ms-0.42ms  (Low)
        4:  1.59ms-0.42ms  (Low)
     */
    static public void dataToIndexAEHA(boolean[] a, byte[] data, int toIndex) {
        int i = toIndex;

        for (boolean b: a) {
            if (b) {
                data[i] = (2 << 4) | 4;
            } else {
                data[i] = (2 << 4) | 3;
            }
            i++;
        }   
    }

    /*
      Index:
       0: 9000us header (Waveform)
       1: 4500us header low
       2: 560us  Waveform
       3: (2250-560)us logical-1 low
       4: (1120-560)us logical-0 low
     */
    static public void dataToIndexNEC(boolean[] a, byte[] data, int toIndex) {
        int i = toIndex;

        for (boolean b: a) {
            if (b) {
                data[i] = (2 << 4) | 3;
            } else {
                data[i] = (2 << 4) | 4;
            }
            i++;
        }   
    }

}
