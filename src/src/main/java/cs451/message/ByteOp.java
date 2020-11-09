package cs451.message;

/**
 * Class implementing some byte operations (done for serialization purposes at
 * the packet level).
 */
class ByteOp {

    private ByteOp() {
    }

    /**
     * Transform an integer into bytes and save them at the right offset in the
     * given array.
     *
     * @param integer The integer to serialize into bytes.
     * @param array   The array in which to save the serialization.
     * @param offset  The offset, or index, at which to start saving the bytes.
     */
    public static void intToByte(int integer, byte[] array, int offset) {
        int shift = Integer.BYTES << 3;
        for (int i = offset; i < offset + Integer.BYTES; ++i) {
            shift -= 8;
            array[i] = (byte) (integer >> shift);
        }
    }

    /**
     * Transform bytes into an integer, given an array of bytes and an offset.
     *
     * @param array  The bytes from which to extract the integer.
     * @param offset The offset, or index, at which to start extracting the bytes.
     * @return The deserialized integer.
     */
    public static int byteToInt(byte[] array, int offset) {
        int ret = 0;
        for (int i = offset; i < offset + Integer.BYTES; ++i) {
            ret <<= 8;
            ret |= (int) array[i] & 0xFF;
        }
        return ret;
    }
}
