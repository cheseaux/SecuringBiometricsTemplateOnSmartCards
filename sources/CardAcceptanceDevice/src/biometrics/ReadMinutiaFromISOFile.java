package biometrics;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class provides utility methods for extracting minutiae from FVC files
 * @author Leila Mirmohamadsadeghi
 */
public class ReadMinutiaFromISOFile {

	private File fFile;
	private ArrayList <String> binaryData;
	private ArrayList <Short> minutiae;
	private ArrayList <Minutia> minutiae2;
	private int numMinutiae;


	/**
   Constructor.
   @param fFile full name of an existing, readable file.
	 */
	public ReadMinutiaFromISOFile(String fFile){
		this.fFile = new File(fFile);  
		this.minutiae = new ArrayList<Short>();
		this.minutiae2 = new ArrayList<Minutia>();
		this.binaryData = new ArrayList<String>();
	}


	/**
	 * returns the minutiae list
	 * @return
	 */
	public ArrayList<Minutia> getMinutiae() {
		return this.minutiae2;
	}


	/**
	 * Returns the minutiae direction
	 * @return
	 */
	public short[] getMinutiaeDir() {
		short[] tab = new short[minutiae.size()];
		for (int i = 0; i <minutiae.size(); i++) {
			tab[i] = minutiae.get(i);
		}
		return tab;
	}

	/**
	 * computes the binary equivalent of the hex data contained in the template for further processing. Internal use only.
	 * @throws FileNotFoundException
	 */
	private final void computeBinData() throws FileNotFoundException {
		FileInputStream fileIn;

		try {
			fileIn = new FileInputStream(this.fFile);
			int hexByte;
			try {
				while((hexByte = fileIn.read()) != -1){
					String by = Integer.toBinaryString(hexByte); 
					this.binaryData.add(by);			    

				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	/**
	 * Uses previously converted binary data of the template file to process the file heather. Internal use only.
	 */
	private final void processHeather() {
		String buffer1 = new String(); 
		buffer1 = this.binaryData.get(14);
		buffer1 = this.binaryData.get(16);
		buffer1 = this.binaryData.get(18);
		buffer1 = this.binaryData.get(20);

		// number of minutiae
		buffer1 = this.binaryData.get(27);
		this.numMinutiae = Integer.parseInt(buffer1, 2);
	}

	/**
	 * processMinutiae
	 * to process the remainder of the file and to extract minutiae information. For outside use.
	 */
	public final void process() {
		try {
			this.computeBinData();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.processHeather();
		String bufferX1 = new String();
		String bufferY1 = new String();
		String bufferX2 = new String();
		String bufferY2 = new String();
		String bufferX = new String();
		String bufferY = new String();
		String bufferDir = new String();
		int minX; 
		int minY;
		double minDir; 
		int bufferQ;
		String zero = new String("0");
		int minIx = 28; // index of the beginning of the information about the first minutia
		int minCt = 0;
		while(minIx < binaryData.size() && minCt < this.numMinutiae){
			bufferX1 = this.binaryData.get(minIx);
			while(bufferX1.length()<8){
				bufferX1 = zero.concat(bufferX1);
			}
			minIx += 1;
			bufferX2 = this.binaryData.get(minIx);
			while(bufferX2.length()<8){
				bufferX2 = zero.concat(bufferX2);
			}
			bufferX = bufferX1.concat(bufferX2);
			minX = Integer.parseInt(bufferX.subSequence(3, 16).toString(), 2);
			minIx += 1; 
			bufferY1 = this.binaryData.get(minIx);
			while(bufferY1.length()<8){
				bufferY1 = zero.concat(bufferY1);
			}
			minIx += 1;
			bufferY2 = this.binaryData.get(minIx);
			while(bufferY2.length()<8){
				bufferY2 = zero.concat(bufferY2);
			}
			bufferY = bufferY1.concat(bufferY2);
			minY = Integer.parseInt(bufferY.subSequence(3, 16).toString(), 2);
			minIx += 1;
			bufferDir = this.binaryData.get(minIx);
			minDir = Integer.parseInt(bufferDir, 2)*2* Math.PI/256;
			minIx += 1; // skip quality
			bufferQ = Integer.parseInt(this.binaryData.get(minIx),2);
			minIx +=1;
			//		System.out.println("minutia x, y, dir: "+bufferX+" "+ bufferX.subSequence(3, 16) +" "+ bufferY+" "+ bufferY.subSequence(3, 16) + " "+ bufferDir);
			//		System.out.println("minutia x, y, dir: "+ minX + " "+ minY + " "+ minDir+ " Quality "+ bufferQ);
			Minutia bufferM = new Minutia(minX, minY, minDir, bufferQ);
			this.minutiae.add((short) (minDir * 180 / Math.PI));
			minutiae2.add(bufferM);
			Collections.sort(minutiae2);
			if (minutiae2.size() >= 30) {
				return;
			}
			minCt +=1;

		}
	}


} 
