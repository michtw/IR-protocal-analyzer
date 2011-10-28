package ir;

import java.util.*;

public class AEHA {

    private long irPatterns[];
    private byte irData[];
    private final int tolerance = 50;

    public AEHA() {
    }

    public AEHA(long irPatterns[], byte irData[]) {
        this.irPatterns = irPatterns;
        this.irData = irData;
    }

    public String toString(long irPatterns[], byte irData[]) {
        int high = 0;
        int low = 0;
        
        // First byte is AEHA IR header. Data start from second byte.
        long one = irPatterns[irData[1] & 0xf];
        long zero = 0;
        int nibbleH = 0;
        int nibbleL = 0;
        int irCode = 0;
        int shift = 31;
        
        short customCode = 0x0000;
        short dataCode = 0x00;
        BitSet bits = new BitSet(); 
        byte ir[];
        int parity = 0;
        int data = 0;
        int irLen = 0;

        for (int i = 2; ; i++) {

            nibbleH = irData[i] & 0xf;

            if (one > (irPatterns[nibbleH] - tolerance) && 
                one < (irPatterns[nibbleH] + tolerance)) {
                // equal
                continue; 
            } else {
                if (one > irPatterns[nibbleH]) {
                    zero = irPatterns[nibbleH];
                } else {
                    zero = one;
                    one = irPatterns[nibbleH];
                }
                break;          
            }
        }

        System.out.println("one: " + String.format("%#x", one) + ", zero: " + String.format("%#x", zero));
 
        for (int i = 1; ; i++) {

            nibbleL = (irData[i] >> 4) & 0xf;
            nibbleH = irData[i] & 0xf;

            if ((nibbleH == 0xf) || (nibbleL == 0xf)) {
                break;
            }

            if ((irPatterns[nibbleL] & Parameters.mask) == 0) {
                System.out.println("Data format wrong: " + irData[i]);
                //return;
            }

            if (one > (irPatterns[nibbleH] - tolerance) && 
                one < (irPatterns[nibbleH] + tolerance)) {
                System.out.print("1 ");
                bits.set(i-1, true);
            } else {
                bits.set(i-1, false);
                System.out.print("0 ");
            }
            irLen++;
        }
        irLen++;
 
        System.out.println("");
 
        //ir = bits.toByteArray();
 
        int j = 15;
        int i;

        for (i = 0; i < 16; i++, j--) {
            if (bits.get(i)) {
                customCode |= 1 << j;
            }
        }
        //dataCode = (short)((irCode >> 8) & 0xff);

        System.out.println("Custome code: " + String.format("%#06x", customCode));

        j = 3;
        for (i = 0; i < 4; i++, j--) {
            if (bits.get(i + 16)) {
                parity |= 1 << j;
            }
        }

        System.out.println("Parity code:  " + String.format("0x%x", parity));

        j = 3;
        for (i = 0; i < 4; i++, j--) {
            if (bits.get(i + 20)) {
                data |= 1 << j;
            }
        }
        System.out.println("Data-0 code:  " + String.format("0x%x", data));
        
        irLen -= (1 + 16 + 8); // Minus header, customer code, parity and Data0

        j = 7;
        data = 0;

        for (i = 0; i < irLen; i++, j--) {
            if (bits.get(i + 24)) {
                data |= 1 << j;
            }
            if (((i + 1) % 8) == 0) {
                System.out.println("Data-"+ ((i + 1) / 8) +" code:  " + 
                                    String.format("%#04x", data & 0xff));
                data = 0x0;
                j = 8;
            }
        }
        
        return "";
    }

    public String toString() {
        return "AEHA IR format.";
    }

    public byte[] codeToCommand(short customCode, byte[] data) {

        byte ir[] = new byte[512];
        byte[] patterns = new byte[64];
        int d[] = new int[5];

        int idx = 0;

        Arrays.fill(ir, (byte)0x00);

        d[0] = (int)(Parameters.AEHA_header / Parameters.coefficient) | Parameters.mask;
        d[1] = (int)(Parameters.AEHA_header_low / Parameters.coefficient);
        d[2] =  (int)(Parameters.AEHA_carrier_waveform / Parameters.coefficient) | Parameters.mask;
        d[3] =  (int)(Parameters.AEHA_low_1 / Parameters.coefficient);
        d[4] =  (int)(Parameters.AEHA_low_2 / Parameters.coefficient);

        ir[idx++] = (byte)Parameters.cmdStart;
        ir[idx++] = (byte)Parameters.cmdStart;
        ir[idx++] = (byte)0x00;  // length
        ir[idx++] = (byte)(6 + 64 + 13 + 1 + 2);  // length
        ir[idx++] = (byte)Parameters.irSend;
        ir[idx++] = (byte)Parameters.freqAEHA;

        Arrays.fill(patterns, (byte)0x00);
        Utils.toIrPatterns(d, patterns);
        System.arraycopy(patterns, 0, ir, idx, patterns.length);
        idx += patterns.length;
    
        ir[idx++] = 0x01;  // header

        boolean[] bit = new boolean[data.length * 8];
        int j = 0;
        for (byte b: data) {
	    for (int i = 7; i > 0; i--, j++) {
                bit[j] = (((b >>> i) & 0x1) == 0x1);
	    }
        }

        Utils.dataToIndexAEHA(bit, ir, idx);

        idx += data.length * 8;

        ir[idx++] = (byte)Parameters.keyRelease;

        CRC16CCITT crc = new CRC16CCITT();
        for (int i = 0; i < idx; i++) {
            crc.update(ir[i]);
        }
        long crc16 = crc.getValue();

        ir[idx++] = (byte)((crc16 >>> 8) & 0xff);
        ir[idx++] = (byte)(crc16 & 0xff);

        return Arrays.copyOf(ir, idx);
    }
}

