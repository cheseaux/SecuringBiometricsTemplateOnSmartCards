package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

import biometrics.MCCBase;
import biometrics.Minutia;
import biometrics.ReadMinutiaFromISOFile;

/**
 * This class analyze different scenario of genuine/impostors score
 * using LSS matching algorithm 
 * @author Jonathan Cheseaux (cheseauxjonathan@gmail.com)
 *
 */
public class ScoreTest {


	public static final short PRECISION = 1550; //Empirical value
	private ArrayList<File> templates = new ArrayList<File>();
	private final static int ns = 16; // number of cells in the spatial dimension (base of cylinder)
	private final static int nd = 8; // number of cells in the directional dimension (height of cylinder)

	private final static ArrayList<Integer> uniqueKey = genRandomKey(ns*ns*nd);

	/**
	 * Extract miniutiae from a IST finger impression file
	 * @param file the IST file
	 * @return A list of {@link Minutia} objects.
	 */
	private ArrayList<Minutia> getMinutiaFromFile(File file) {
		ReadMinutiaFromISOFile parser1 = new ReadMinutiaFromISOFile(file.getAbsolutePath());
		parser1.process();
		return parser1.getMinutiae();
	}

	public short[] getMinutiaDirFromFile(File file) {
		ReadMinutiaFromISOFile parser1 = new ReadMinutiaFromISOFile(file.getAbsolutePath());
		parser1.process();
		return parser1.getMinutiaeDir();
	}


	/**
	 * This method compute the MCC and transform the result into a secured template
	 * @param key, the key for the transformation
	 * @param file the IST file of the user's finger impression
	 * @param flag if 1, use binary quantization, else use real numbers
	 * @return a list containing the floating point values of the template
	 */
	public byte[] generateRawTemplate(ArrayList<Integer> key, File file, int flag) {

		int r = 75;	 // radius of cylinder
		double sigmaS = 6; // sigma for Gaussian used to smooth spatial contributions
		double sigmaD = 0.43633231299858238; // sigma for Gaussian used to smooth directional contributions

		ArrayList<Minutia> minutiae1 = getMinutiaFromFile(file);
		MCCBase engine = new MCCBase(ns, nd, r, minutiae1, sigmaS, sigmaD);
		ArrayList<double[]> linTemplate1 = engine.computeTemplate();

		byte[] result = engine.transform(linTemplate1, key, flag, 5000, 1000000);

		return result;
	}

	/**
	 * Generate a random key
	 * @return list of integer representing the key
	 */
	public static ArrayList<Integer> randomKey(boolean different) {
		return different ? genRandomKey(ns*ns*nd) : uniqueKey;
	}

	/**
	 * This function generates an array containing a random re-arrangement of the cylinder indexes
	 * @param orgLength original indexes
	 * @param redLength final indexes (same if only re-arrangement is required.)
	 * @return random array
	 */
	public static ArrayList<Integer> genRandomKey(int redLength){

		ArrayList<Integer> numList = new ArrayList<Integer>();
		for (int i = 0; i < redLength; i++) {
			numList.add(i);
		}

		Collections.shuffle(numList);

		return numList;
	}

	/**
	 * Print the scores in a text file
	 * @param scores a list of scores
	 * @param out the output file to be written
	 */
	private void printScoreToFile(ArrayList<Double> scores, File out) {
		//		DataOutputStream dos = new DataOutputStream(new FileOutputStream(out));
		PrintStream pStr;
		try {
			pStr = new PrintStream(new FileOutputStream(out));

			for(double score : scores) {
				pStr.print(score + ",");
			}
			pStr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method browse a folder and its subfolders and gather
	 * every .ist files
	 * @param path the path to the folder
	 */
	public void walk( String path ) {

		File root = new File( path );
		File[] list = root.listFiles();

		for ( File f : list ) {
			if ( f.isDirectory() ) {
				walk( f.getAbsolutePath() );
			}
			else {
				if (f.length() > 42 && f.length() < 50000) {
					templates.add(f);
				} 
			}
		}
	}

	/**
	 * This method browse a folder and its subfolders and gather
	 * every .ist files containing the same finger impression (1 up to 8)
	 * @param path the path to the folder
	 * @param fingerNumber the number of the finger impression we want
	 */
	public void walkSameFinger( String path, int fingerNumber) {

		File root = new File( path );
		File[] list = root.listFiles();

		for ( File f : list ) {
			if ( f.isDirectory() ) {
				walkSameFinger( f.getAbsolutePath(), fingerNumber);
			}
			else {
				if (templates.size() < 100 && f.length() > 42 && f.length() < 50000 
						&& f.getName().endsWith(fingerNumber + ".ist")) {
					templates.add(f);
				}
			}
		}
	}


	public ArrayList<File> getTemplates() {
		return templates;
	}

	/**
	 * This method browse a folder and its subfolders and gather
	 * every .ist files for a specific user
	 * @param path the path to the folder
	 * @param fingerPrintNumber the user we consider
	 */
	public void walkSameFingerprint(String path, int fingerPrintNumber) {

		File root = new File( path );
		File[] list = root.listFiles();

		for ( File f : list ) {
			if ( f.isDirectory() ) {
				walkSameFingerprint(f.getAbsolutePath(), fingerPrintNumber);
			} else {
				if (f.length() > 42 && f.length() < 50000 
						&& f.getAbsolutePath().toLowerCase()
						.contains(fingerPrintNumber + "_")) {
					templates.add(f);
					//					System.out.println(f.getAbsolutePath());
				} else {
					//					System.out.println(f.getAbsolutePath());
				}

			}
		}
	}

}