package cs451.message;

class ByteOp {
    
    public static void intToByte(int integer, byte[] array, int offset) {
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

    public static void longToByte(long longNumber, byte[] array, int offset) {
        int shift = Long.BYTES << 3;
        for (int i = offset; i < offset+Long.BYTES; ++i) {
            shift -= 8;
            array[i] = (byte)((longNumber >> shift));
        }
    }

    public static long byteToLong(byte[] array, int offset) {
        long ret = 0;
        for (int i = offset; i < offset+Long.BYTES; ++i) {
            ret <<= 8;
            ret |= (long)array[i] & 0xFFL;
        }
        return ret;
    }
}
