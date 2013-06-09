package monpackage;

import javacard.framework.APDU;

/**
 * This class is responsible for computing fingerprint matching score
 * @author Jonathan Cheseaux (jonathan.cheseaux@epfl.ch)
 *
 */
public class LSSMatcher {

	/** Lookup table */
	private final static short[] NP_LOOKUP = new short[]{3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,4,4,4,5,5,6,7,7,8,8,9,9,9,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10};
	
	/** Empirical value used to get rid of the absence of float values **/
	private static final short PRECISION = 1550;

	/**
	 * Computes the angular difference between two given angles t1 and t2
	 * @param t1
	 * @param t2 angles whose difference is required.
	 * @return diff angular difference of t1 and t2 according to (9) in reference paper.
	 */
	public short angularDiff(short a, short b){
		short d = (short) (Util.abs((short) (a - b)) % 360);
		short r = (short) (d > 180 ? 360 - d : d);
		return r;
	}

	/**
	 * Computes the global similarity score between two MCC templates based on LSS
	 * @param linTemplate1 (resp. 2) ArrayList containing the cell values of 
		 all linearized cylinders in template 1 (resp. 2)
	 * @param external_template 
	 * @param validities1 (resp. 2) ArrayList containing the validity bits of 
		 the base cells of all cylinders in template 1 (resp. 2)
	 * @param external_minutiae (resp. 2) ArrayList of all minutiae involved in template 1 (resp. 2)
	 * @param external_minutiae2 
	 * @param DeltaTheta the maximum directional difference between two minutiae 
		 whose corresponding cylinders are considered "matchable".
	 * @param apdu 
	 * @param minME minimum number of matchable elements needed to exist in two matchable cylinders.
	 * @param minNP minimum number of pairs to be selected for calculating the global score.
	 * @param maxNP maximum number of pairs to be selected for calculating the global score.
	 * @return double containing the global score in the range [0,1]; 1 means maximum similarity.
	 */		
	public short matchTemplates_LSS(
			byte[] template, byte[] external_template, short[] minutiae, short[] external_minutiae, short DeltaTheta, APDU apdu){

		short score = 0;
		short linSize1 = (short) (external_template.length / 128);
		short linSize2 = (short) (template.length / 128);

		short[] sorted_Gamma = new short[linSize1 * linSize2];
		short l = 0; //counter index for sorted_Gamma

		if(linSize1 >0 && linSize2> 0){
			for (short i = 0; i < linSize1; i++){
				for (short j = 0; j < linSize2; j++){

					byte[] temp1 = new byte[128];
					byte[] temp2 = new byte[128];

					short norma = 0;
					short normb = 0;

					for (short m = 0, k = (short) (i * 128); k < (short) (i * 128) + 128; k++, m++) {
						temp1[m] = external_template[k];
					}
					for (short m = 0, k = (short) (j * 128); k < (short) (j * 128) + 128; k++, m++) {
						temp2[m] = template[k];
					}
					
					norma = norm(Util.byteArrayToBitsArray(temp1));
					normb = norm(Util.byteArrayToBitsArray(temp2));

					short denom1 = (short) (norma + normb);
					short localSim1 = 0;

					if( Util.abs(angularDiff(external_minutiae[i], minutiae[j])) <= DeltaTheta && denom1 != 0){
						short temp = (short) (PRECISION / denom1);
						short val = (short) (temp * norm(diff(temp1, temp2)));
						localSim1 = (short) (PRECISION - val);
					}

					sorted_Gamma[l] = localSim1;
					//					bubble sorting the local similarity scores of all possible pairs
					for (short k = l; k > 0; k--){	
						if (sorted_Gamma[k] > sorted_Gamma[k-1]){
							short temp = sorted_Gamma[k];
							sorted_Gamma[k] = sorted_Gamma[k-1];
							sorted_Gamma[k-1] = (short) temp;

						}
						else{
							break;
						}
					}
					l++;
				}
			}

			short Z = Util.min(linSize1, linSize2);
			short nP = NP_LOOKUP[Z];
			short sum = 0;		

			for (short i=0; i < nP; i++){
				sum += sorted_Gamma[i];
			}		
			score = (short) (sum/nP);
		}
		return score; //global score based on LSS
	}

	/**
	 * Computes the norm of a byte array
	 * @param tab
	 * @return
	 */
	private short norm(byte[] tab) {
		short count = 0;
		for (byte b : tab) {
			count += b;
		}
		return count;
	}

	/**
	 * Initiate the matching process
	 * @param template the user's template
	 * @param external_template the challenger's template
	 * @param minutiae the user minutiae
	 * @param external_minutiae the challenger minutiae 
	 * @param apdu the APDU response to be sent
	 * @return the matching score
	 */
	public short match(byte[] template, byte[] external_template, short[] minutiae, short[] external_minutiae, APDU apdu) {
		short DeltaTheta = 135;
		return matchTemplates_LSS(template, external_template, minutiae, external_minutiae, DeltaTheta, apdu);
	}

	/**
	 * Computes the bit-wise difference between 2 byte arrays
	 * @param linCyl1
	 * @param linCyl2
	 * @return
	 */
	public byte[] diff(byte[] linCyl1, byte[] linCyl2){
		byte[] bitsA = Util.byteArrayToBitsArray(linCyl1);
		byte[] bitsB = Util.byteArrayToBitsArray(linCyl2);
		byte[] res = new byte[bitsA.length];
		
		for (int i = 0; i < bitsA.length; i++) {
			res[i] = (byte) (bitsA[i] != bitsB[i] ? 1 : 0);
		}
		return res;
	}

}