package ir;

import java.util.*;

public class NEC {
   
    private final long IRmask = 0x00800000;
    private long irPatterns[];
    private byte irData[];
    private final int tolerance = 50;

    public NEC() {
    }

    public NEC(long irPatterns[], byte irData[]) {
        this.irPatterns = irPatterns;
        this.irData = irData;
    }

    private int checksum(int data[], int length) {
        int chk = 0;
        for (int i = 0; i < length; i++) {
            chk += data[i];    
        }
        return (chk & 0xff);
    }

    public String toString(long irPatterns[], byte irData[]) {
        int high = 0;
        int low = 0;
        
        // First byte is NEC IR header. Data start from second byte.
        long one = irPatterns[irData[1] & 0xf];
        long zero = 0;
        int nibbleH = 0;
        int nibbleL = 0;
        int irCode = 0;
        int shift = 31;
        
        short customCode = 0x0000;
        short dataCode = 0x00;

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
 
        for (int i = 1; ; i++, shift--) {

            nibbleL = (irData[i] >> 4) & 0xf;
            nibbleH = irData[i] & 0xf;

            if ((nibbleH == 0xf) || (nibbleL == 0xf)) {
                break;
            }

            if ((irPatterns[nibbleL] & IRmask) == 0) {
                System.out.println("Data format wrong: " + irData[i]);
                //return;
            }

            if (one > (irPatterns[nibbleH] - tolerance) && 
                one < (irPatterns[nibbleH] + tolerance)) {
                System.out.print("1 ");
                irCode |= 1 << shift;
            } else {
                System.out.print("0 ");
            }
        }
        System.out.println("");

        customCode = (short)(irCode >> 16);
        dataCode = (short)((irCode >> 8) & 0xff);
/*
        if (~(dataCode & 0xff) != (irCode & 0xff)) {
            System.out.println("NEC IR data code error.");
        }
 */
        System.out.println("Custome code: " + String.format("%#06x", customCode));
        System.out.println("Data code:    " + String.format("%#04x", dataCode));
        
        return "";
    }

    public String toString(int custom_code, int data_code) {
        return "";       
    }

    public String toString() {
        return "NEC IR format.";
    }

    private void byteToIrLength(byte b, int[] ir, int toIndex) {
        boolean bit;
        int idx = toIndex;
        
        for (int i = 7; i >= 0; i--) {
            bit = ((b >>> i) & 0x1) == 0x1 ? true : false;
            ir[idx++] = Parameters.NEC_carrier_waveform;
            if (bit) {                
                ir[idx++] = Parameters.NEC_logical1_low;
            } else {
                ir[idx++] = Parameters.NEC_logical0_low;
            }
        }
    }
            
    public int[] codeToIrLength(short customCode, byte dataCode) {
        int[] ir = new int[(16 + 16 + 1) * 2]; // 33 bit
        byte[] b = new byte[4];
        int j = 0;
        
        Arrays.fill(ir, 0x00000000);
        b[0] = (byte)(customCode >>> 8);
        b[1] = (byte)(customCode & 0xff);
        b[2] = dataCode;
        b[3] = (byte)~dataCode;
        ir[j++] = Parameters.NEC_header;
        ir[j++] = Parameters.NEC_header_low;        
        
        for (int i = 0; i < b.length; i++, j+=16) {
            byteToIrLength(b[i], ir, j);
        }
        return ir;
    }
    
    public byte[] codeToCommand(short customCode, byte dataCode) {

        byte ir[] = new byte[512];
        byte[] patterns = new byte[64];
        int[] d = new int[5];

        int headerH;
        int headerL;
        int pulsesWidth;
        int idx = 0;
        int lowWidthL, lowWidthS;

        Arrays.fill(ir, (byte)0x00);

        d[0] = (int)(Parameters.NEC_header / Parameters.coefficient) | Parameters.mask;
        d[1] = (int)(Parameters.NEC_header_low / Parameters.coefficient);
        d[2] = (int)(Parameters.NEC_carrier_waveform / Parameters.coefficient) | Parameters.mask;
        d[3] = (int)((Parameters.NEC_logical1_low) / Parameters.coefficient);
        d[4] = (int)((Parameters.NEC_logical0_low) / Parameters.coefficient);

        ir[idx++] = (byte)Parameters.cmdStart;
        ir[idx++] = (byte)Parameters.cmdStart;
        ir[idx++] = (byte)0x00;  // len
        ir[idx++] = (byte)0x69;  // len
        ir[idx++] = (byte)Parameters.irSend;
        ir[idx++] = (byte)Parameters.freqNEC;

        // IR pattern
        Utils.toIrPatterns(d, patterns);
        System.arraycopy(patterns, 0, ir, idx, patterns.length);
        idx += patterns.length;
       
        ir[idx++] = 0x01; // header

        boolean[] bit = new boolean[16 + 8 + 8]; //  (customCode[short] + dataCode[byte] + ~dataCode)
        byte[] data = new byte[4];
        data[0] = (byte)((customCode >>> 8) & 0xff);
        data[1] = (byte)(customCode & 0xff);
        data[2] = dataCode;
        data[3] = (byte)(~dataCode);

        int j = 0;
        for (byte b: data) {
	    for (int i = 7; i > 0; i--, j++) {
                bit[j] = (((b >>> i) & 0x1) == 0x1);
	    }
        }

        Utils.dataToIndexNEC(bit, ir, idx);
        
        idx += bit.length;

        ir[idx++] = Parameters.keyRelease;

        CRC16CCITT crc = new CRC16CCITT();
        for (int i = 0; i < idx; i++) {
            crc.update(ir[i]);
        }
        long crc16 = crc.getValue();

        ir[idx++] = (byte)((crc16 >>> 8) & 0xff);
        ir[idx++] = (byte)(crc16 & 0xff);

        //int b[] = new int[idx];
        //System.arraycopy(ir, 0, b, 0, idx); // Another array copy method

        return Arrays.copyOf(ir, idx);
    }
}

