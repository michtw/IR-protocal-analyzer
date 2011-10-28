package ir;

public class Parameters {

    static final int cmdStart = 0xD8;
    static final int irSend = 0x31;
    static final int freqNEC = 38; // 38KHz
    static final int freqSONY = 40;
    static final int freqAEHA = 38;
    static final int mask = 0x00800000;

    static final int keyRelease = 0;
    static final float coefficient = 0.48f;
    static public enum IRformat {NEC, SONY, AEHA};
   
    static final int NEC_header = 9000;
    static final int NEC_header_low = 4500;
    static final int NEC_carrier_waveform = 560; // microsecond
    static final int NEC_logical1_low = 2250 - 560;
    static final int NEC_logical0_low = 1120 - 560;

    static final int SONY_header = 2400;

    static final int AEHA_header = 3600;
    static final int AEHA_header_low = 1690;
    static final int AEHA_carrier_waveform = 420;
    static final int AEHA_low_1 = 840 - AEHA_carrier_waveform;
    static final int AEHA_low_2 = 1590 - AEHA_carrier_waveform;

    static final int tolerance = 100;

    static final int MAX_PACKET_LEN = 128;
}

