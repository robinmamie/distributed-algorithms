package cs451.message;

public class ByteOp {
    
    private void intToByte(int integer, byte[] array, int offset) {
        int shift = Integer.BYTES << 3;
        for (int i = offset; i < offset+Integer.BYTES; ++i) {
            shift -= 8;
            array[i] = (byte)((integer >> shift));
        }
    }

    public static int byteToInt(byte[] array, int offset) {
        int ret = 0;
        for (int i = offset; i < offset+Integer.BYTES; ++i) {
            ret <<= 8;
            ret |= (int)array[i] & 0xFF;
        }
        return ret;
    }

}
