/*************************************************************************
 *  Compilation:  javac CRC16CCITT.java
 *  Execution:    java CRC16CCITT s
 *  Dependencies: 
 *  
 *  Reads in a sequence of bytes and prints out its 16 bit
 *  Cylcic Redundancy Check (CRC-CCIIT 0xFFFF).
 *
 *  1 + x + x^5 + x^12 + x^16 is irreducible polynomial.
 *
 *  % java CRC16-CCITT 123456789
 *  CRC16-CCITT = 29b1
 *
 *************************************************************************/

package ir;

import java.util.zip.*;

public class CRC16CCITT implements Checksum { 

    private int crc = 0xFFFF;                // initial value
    private final int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12) 
/*
    public static void main(String[] args) { 
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12) 

        // byte[] testBytes = "123456789".getBytes("ASCII");

        byte[] bytes = args[0].getBytes();

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
             }
        }

        crc &= 0xffff;
        System.out.println("CRC16-CCITT = " + Integer.toHexString(crc));
    }
 */
    public void update(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }   
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }   
       
	for (int i = off; i < len; i++) {
            update(b[i]);
        }
    }

    public void update(byte b) {
	for (int i = 0; i < 8; i++) {
	    boolean bit = ((b   >> (7-i) & 1) == 1);
	    boolean c15 = ((crc >> 15    & 1) == 1);
	    crc <<= 1;
	    if (c15 ^ bit) crc ^= polynomial;
	}
    }

    /**
     * Updates the CRC-32 checksum with the specified array of bytes.
     *
     * @param b the array of bytes to update the checksum with
     */
/*
    public void update(byte[] b) {
        crc = updateBytes(crc, b, 0, b.length);
    }
 */
    public void update(int b) {
	for (int i = 0; i < 8; i++) {
	    boolean bit = ((b   >> (7-i) & 1) == 1);
	    boolean c15 = ((crc >> 15    & 1) == 1);
	    crc <<= 1;
	    if (c15 ^ bit) crc ^= polynomial;
	}
    }

    /**
         Resets CRC-16 to initial value.
      */
    public void reset() {
        crc = 0xFFFF;
    }

    /**
         Returns CRC-16 value.
      */
    public long getValue() {
        return (long)crc & 0xffffffffL;
    }

}

