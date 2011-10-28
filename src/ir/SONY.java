/**
   Features:
     12-bit, 15-bit and 20-bit versions of the protocol exist (12-bit described here)
     5-bit address and 7-bit command length (12-bit protocol)
     Pulse width modulation
     Carrier frequency of 40kHz
     Bit time of 1.2ms or 0.6ms
  */

package ir;

import java.util.*;

public class SONY {

    private final long IRmask = 0x00800000;
    private long irPatterns[];
    private byte irData[];
    private final int tolerance = 50;

    public SONY() {
    }

    public SONY(long irPatterns[], byte irData[]) {
        this.irPatterns = irPatterns;
        this.irData = irData;
    }

    public String toString(long irPatterns[], byte irData[]) {
        int high = 0;
        int low = 0;
        
        // First byte is SONY IR header. Data start from second byte.
        long one = irPatterns[(irData[1] >> 4) & 0xf];  // logic one
        long zero = 0;
        int nibbleH = 0;
        int nibbleL = 0;
        int irCode = 0;
        int shift = 11; // Sony remote control have 12-bit, 15-bit and 20-bit versions.
        
        int command = 0x00;
        int address = 0x00;

        for (int i = 2; ; i++) {

            nibbleL = (irData[i] >> 4) & 0xf;

            if (one > (irPatterns[nibbleL] - tolerance) && 
                one < (irPatterns[nibbleL] + tolerance)) {
                // equal
                continue; 
            } else {
                if (one > irPatterns[nibbleL]) {
                    zero = irPatterns[nibbleL];
                } else {
                    zero = one;
                    one = irPatterns[nibbleL];
                }
                break;          
            }
        }

        System.out.println("one: " + String.format("%#x", one) + ", zero: " + String.format("%#x", zero));
 
        for (int i = 1; i <= 12; i++, shift--) {

            nibbleL = (irData[i] >> 4) & 0xf;
            nibbleH = irData[i] & 0xf;

            if ((nibbleH == 0xf) || (nibbleL == 0xf)) {
                break;
            }

            if ((irPatterns[nibbleL] & IRmask) == 0) {
                System.out.println("Data format wrong: " + irData[i]);
                //return;
            }

            if (one > (irPatterns[nibbleL] - tolerance) && 
                one < (irPatterns[nibbleL] + tolerance)) {
                System.out.print("1 ");
                irCode |= 1 << shift;
            } else {
                System.out.print("0 ");
            }
        }
        System.out.println("");

        // SONY IR code start from LSB
        // 0 1 0 0 1 0 0     1 0 0 0 0 
 
        address = irCode & 0x0000001f;
        address = (address << 4) | ((address << 2) & 0x08) | (address & 0x08) |  
                  ((address >> 2) & 0x02) | (address >> 4);
        address &= 0xff;

        command = (irCode >> 5) & 0xff;

        command = (command << 6) | ((command << 4) & 0x20) | ((command << 2) & 0x10) | (command & 0x08) |
                  ((command >> 2) & 0x04) | ((command >> 4) & 0x02) | (command >> 6);
   
        command &= 0xff;

        System.out.println("Command: " + String.format("%#04x", command));
        System.out.println("Address  " + String.format("%#04x", address));
        
        return "";
    }

    public String toString() {
        return "SONY IR format.";
    }

    public byte[] codeToCommand(byte command, byte address) {

        byte ir[] = new byte[512];
        byte[] patterns = new byte[64];
        int data[] = new int[4];

        byte buff[];
        int headerH;
        int pulsesWidthLogical1;
        int pulsesWidthLogical0;
        int idx = 0;
        int lowWidth;

        Arrays.fill(ir, (byte)0x00);

        headerH = (int)(Parameters.SONY_header / Parameters.coefficient) | Parameters.mask;
        data[0] = headerH;

        pulsesWidthLogical0 = (int)(600 / Parameters.coefficient) | Parameters.mask;
        data[1] = pulsesWidthLogical0;

        pulsesWidthLogical1 = (int)(1200 / Parameters.coefficient) | Parameters.mask;
        data[2] = pulsesWidthLogical1;

        lowWidth = (int)(600 / Parameters.coefficient);
        data[3] = lowWidth;

        ir[idx++] = (byte)Parameters.cmdStart;
        ir[idx++] = (byte)Parameters.cmdStart;
        ir[idx++] = (byte)0x00;  // length
        ir[idx++] = (byte)(6 + 64 + 13 + 1 + 2);  // length
        ir[idx++] = (byte)Parameters.irSend;
        ir[idx++] = (byte)Parameters.freqSONY;

        // IR pattern
        Arrays.fill(patterns, (byte)0x00);
        Utils.toIrPatterns(data, patterns);
        System.arraycopy(patterns, 0, ir, idx, patterns.length);
        idx += patterns.length;

        // 7-bit command and 5-bit address length (12-bit protocol)        
        boolean[] bit = new boolean[12]; // Now support 12-bit version of the protocol.
        int j = 0;
        for (int i = 6; i > 0; i--, j++) {
            bit[j] = ((command >>> i) & 1) == 1;
        }

        for (int i = 4; i > 0; i--, j++) {
            bit[j] = ((address >>> i) & 1) == 1;
        }

        ir[idx++] = (byte)0x01;

        Utils.dataToIndexSONY(bit, ir, idx);
        idx += 12;        

        ir[idx++] = (byte)0x00;  // Key event

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

