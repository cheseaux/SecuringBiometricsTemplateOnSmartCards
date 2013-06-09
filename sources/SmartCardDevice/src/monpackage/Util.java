package monpackage;

/**
 * Utility class for handling bytes array and
 * Short mathematical computations.
 * @author Jonathan Cheseaux (cheseauxjonathan@gmail.com)
 *
 */
public class Util {

	/**
	 * Returns the absolute value of a short value
	 * @param x
	 * @return
	 */
	public static short abs(short x) {
		x *= x < 0 ? -1 : 1;
		return x ;
	}

	/**
	 * Returns the minimum between a and b
	 * @param a
	 * @param b
	 * @return
	 */
	public static short min(short a, short b) {
		return a <= b ? a : b;
	}

	/**
	 * This method convert a 8-bytes array to a single byte.
	 * It assume that the array contains only 1's or 0's
	 * @param bits the 8-bytes array to convert
	 * @return a single byte representing the "bits" array
	 */
	public static byte bitsArrayToByte(byte[] bits) {
		byte temp = 0;
		for (int i = 0; i < bits.length; i++) {
			temp |= bits[i] << (7 -i) ; 
		}
		return (byte) (temp + 128);
	}
	
	/**
	 * This method convert a single byte to a 8-bytes array.
	 * @param b the byte to be converted
	 * @return a 8-bytes array representing the binary value of parameter b
	 */
	public static byte[] byteToBitsArray(byte b) {
		byte[] result = new byte[8];
		short val = (short) (b + 128);
		for (int i = 0; i < 8; i++) {
			result[7-i] = (byte) (val % 2);
			val /= 2;
		}
		return result;
	}
	
	/**
	 * Hamming distance of a byte with 0
	 * @param b the byte on which we compute the hamming distance with 0
	 * @return the number of 1's in the binary representation of the byte
	 */
	public static short bitCount(byte b) {
        short temp = (short) (b + 128);
        short count = 0;
		for (int i = 0; i < 8; i++) {
			count += (temp >> (7-i)) % 2;
		}
		return count;
     }

	/**
	 * Computes the number of 1's in the byte array
	 * @param tab
	 * @return
	 */
	public static short norm(byte[] tab) {
		short count = 0;
		for(byte b : tab) {
			count += bitCount(b);
		}
		return count;
	}

	/**
	 * Convert an entire byte array (we assume its length is a multiple
	 * of 8) to a compressed byte array
	 * @param temp1
	 * @return
	 */
	public static byte[] byteArrayToBitsArray(byte[] temp1) {
		byte[] result = new byte[temp1.length * 8];
		short index = 0;
		for (byte b : temp1) {
			byte[] array = Util.byteToBitsArray(b);
			for (int i = 0; i < 8; i++) {
				result[index++] = array[i];
			}
		}
		return result;
	}
}
