package utils;

import java.util.Arrays;

/**
 * Utility class for handling bytes array and
 * Short mathematical computations.
 * @author Jonathan Cheseaux (cheseauxjonathan@gmail.com)
 *
 */
public class Utils {
	
	public static void main(String[] args) {
		byte b = 125;
		
		byte[] tab = byteToBitsArray(b);
		System.out.println("Compare : " + Arrays.toString(tab));
		System.out.println("To : ");displayByte(b);
		
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
	static short bitCount(byte b) {
        short temp = (short) (b + 128);
        short count = 0;
		for (int i = 0; i < 8; i++) {
			count += (temp >> (7-i)) % 2;
		}
		return count;
     }
	
	/**
	 * Debug method for printing a binary representation of a byte
	 * @param b
	 */
	public static void displayByte(byte b) {
		
		short temp = (short) (b + 128);
		for (int i = 0; i < 8; i++) {
			System.out.print(((temp >> (7-i)) % 2)  + "; ");
		}

	}
	
	/**
	 * Square root of Short values. Using Euler's approximation
	 * @param a
	 * @return the square root of a
	 */
	public static short sqrt(short a) {
		short square = 1; // x=1: 1st Integer Square Root
		short delta = 3; // (2x+1), for x=1
		while(square<=a) {
			square+=delta; // (x+1)^2 = x^2 + (2x+1)
			delta +=2; // Next value for (2x+1)
		}
		return (short) (delta/2 - 1); // square is now > a, so find
		// previous value of x
	}

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


}
