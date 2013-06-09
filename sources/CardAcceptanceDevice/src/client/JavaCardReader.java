package client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;

import utils.ScoreTest;

import javacard.framework.Util;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadT1Client;
import com.sun.javacard.apduio.CadTransportException;

/**
 * This class represent the card acceptance device and handle communications
 * with the smart cards (the simulator for now on)
 * @author Jonathan Cheseaux (jonathan.cheseaux@epfl.ch)
 *
 */
public class JavaCardReader {

	/** Applet instruction class **/
	public static final byte CLA_MONAPPLET = (byte) 0xB0;

	/** Length of data sent in one APDU request **/
	public static final short PACKET_SIZE = 100;

	/** Insruction command for setting challenger's template on the card */
	public static final byte INS_SET_TEMPLATE_CAP = 0x01;

	/** Insruction command for setting challenger's minutiae on the card */
	public static final byte INS_SET_MINUTIA_CAP = 0x02;

	/** Insruction command for enroll user's template on the card */
	private static final byte INS_ENROLL_TEMPLATE = 0x03;

	/** Insruction command for enroll user's minutiae on the card */
	private static final byte INS_ENROLL_MINUTIAE = 0x04;

	/** Insruction command for matching the fingerprints */
	private static final byte INS_MATCH = 0x05;

	/** Insruction command for resetting challenger's template and minutiae*/
	private static final byte RESET = 0x06;

	/** User ID to enroll on the card **/
	private static int USER = 14;

	/** Card Acceptance Device client **/
	private static CadT1Client cad;

	/** **/
	private static boolean differentKey = true;

	/**
	 * This methods load a transformed template on a smart card
	 * @param apdu, the APDU request to be sent
	 * @param enroll true if this is for enrollment, false if this is for loading
	 * challenger's fingeprint
	 * @param file Template location on the disk (will change by the data
	 * acquired by the real device)
	 * @throws IOException if there is an Input/Ouptut problem with the template file
	 * @throws CadTransportException if there is a communication error with the smart card
	 */
	private static void load_template_on_card(Apdu apdu, boolean enroll, File file, boolean randomizeKey) throws IOException, CadTransportException {
		
		System.out.println("Loading " + (enroll ? "enrolment " : "verification ") +  "template file : " + file.getName());
		apdu.command[Apdu.INS] = enroll ? INS_ENROLL_TEMPLATE : INS_SET_TEMPLATE_CAP;
		byte[] template1 = acquire_template(file,randomizeKey);

		
		
		
		int totalLength = template1.length;
		int toSend = template1.length;
		int offset = 0;
		int packetNumber = 0;
		while (toSend > 0) {
			++packetNumber;
			byte[] packet = getDataPacket(offset,  PACKET_SIZE, template1);
			toSend -=  PACKET_SIZE;
			offset +=  PACKET_SIZE;
			apdu.command[Apdu.P1] = (byte)((short) totalLength & 0xff);
			apdu.command[Apdu.P2] = (byte)(((short) totalLength >> 8) & 0xff);
			apdu.setDataIn(packet);
			cad.exchangeApdu(apdu);			
			//			if (apdu.getStatus() != 0x9000) {
			//				System.out.println("Failed to transmit packet " + packetNumber);
			//			}
		}
	}

	private static void printStatistics(byte[] tab) {
		int index = 0;
		while(index < tab.length) {
			int zeroCount = 0;
			int oneCount = 0;
			while (index < tab.length && tab[index++] == 0.0) {
				zeroCount++;
			}
			while (index < tab.length && tab[index++] == 1.0) {
				oneCount++;
			}
			if (zeroCount > 8) {
				System.out.println("Group of " + zeroCount + " zeros");
			}
			if (oneCount > 8) {
				System.out.println("Group of " + oneCount + " zeros");
			}
			zeroCount = 0;
			oneCount = 0;
		}
	}

	/**
	 * This methods load a minutiae array on a smart card
	 * @param apdu, the APDU request to be sent
	 * @param enroll true if this is for enrollment, false if this is for loading
	 * challenger's minutia
	 * @param file Fingerprint file location on the disk (will change by the data
	 * acquired by the real device)
	 * @throws IOException if there is an Input/Ouptut problem with the template file
	 * @throws CadTransportException if there is a communication error with the smart card
	 */
	private static void load_minutiae_on_card(Apdu apdu, boolean enroll, File file) throws IOException, CadTransportException {
		System.out.println("Loading " + (enroll ? "enrolment " : "verification ") +  "minutiae file : " + file.getName());
		apdu.command[Apdu.INS] = enroll ? INS_ENROLL_MINUTIAE : INS_SET_MINUTIA_CAP;
		short[] minutia = acquire_minutia(file);
		byte[] minutiaBytes = new byte[minutia.length * 2];

		int bytesIndex = 0;
		for(short s : minutia) {
			minutiaBytes[bytesIndex] = (byte)(s & 0xff);
			minutiaBytes[bytesIndex+1] = (byte) ((s >> 8) & 0xFF);
			bytesIndex += 2;
		}

		apdu.command[Apdu.P1] = (byte)(minutiaBytes.length & 0xff);
		apdu.command[Apdu.P2] = (byte)((minutiaBytes.length >> 8) & 0xff);

		apdu.setDataIn(minutiaBytes);
		cad.exchangeApdu(apdu);
	}

	private static long getDuration(long refTime) {
		return (System.currentTimeMillis() - refTime);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 5) {
			System.out.println("Usage : javacardreader <file1> <file2> <saveFolder> <port> <key> (0 = same key, 1 = different key) ");
			System.exit(-1);
		}

		File enrollTemplateFile = new File(args[0]);
		File challengerFile = new File(args[1]);
		String folderOut = args[2];
		int port = Integer.parseInt(args[3]);
		int keychoice = Integer.parseInt(args[4]);
		differentKey = keychoice == 1 ? true : false;

		/* Connexion a la Javacard */
		Socket sckCard;
		try {
			sckCard = new Socket("localhost", port);
			sckCard.setTcpNoDelay(true);
			BufferedInputStream input = new BufferedInputStream(sckCard.getInputStream());
			BufferedOutputStream output = new BufferedOutputStream(sckCard.getOutputStream());
			cad = new CadT1Client(input, output);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}		

		/* Mise sous tension de la carte */
		try {
			cad.powerUp();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		long startTime = System.currentTimeMillis();

		/* Sélection de l'applet */
		Apdu apdu = new Apdu();
		apdu.command[Apdu.CLA] = 0x00;
		apdu.command[Apdu.INS] = (byte) 0xA4;
		apdu.command[Apdu.P1] = 0x04;
		apdu.command[Apdu.P2] = 0x00;
		byte[] appletAID = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x00, 0x00 };
		apdu.setDataIn(appletAID);
		cad.exchangeApdu(apdu);
		if (apdu.getStatus() != 0x9000) {
			System.out.println("Erreur lors de la sélection de l'applet");
			System.exit(1);
		}

//		System.out.println("Selection time : " + getDuration(startTime));
		startTime = System.currentTimeMillis();

		//		while (!fin) {
		//			System.out.println("############################################################");
		//			System.out.print("# 1 - Enroll template | 2 - Enroll Minutiae | 3 - Send extern template | 4 - Send extern minutiae | 5 - Match | 6 - Exit ? ");
		//
		//			Scanner scanner = new Scanner(System.in);
		//
		//			int choix = scanner.nextInt();
		//			while (!(choix >= 1 && choix <= 6)) {
		//				choix = scanner.nextInt();
		//			}
		//			scanner.nextLine();

		apdu = new Apdu();
		apdu.command[Apdu.CLA] = CLA_MONAPPLET;
		apdu.command[Apdu.P1] = 0x00;
		apdu.command[Apdu.P2] = 0x00;
		apdu.setLe(0x7f);

		enrollUser(apdu, enrollTemplateFile, differentKey);
//		System.out.println("User enrolment : " + getDuration(startTime));
		startTime = System.currentTimeMillis();
		loadChallenger(apdu, challengerFile, differentKey);
//		System.out.println("challenger template : " + getDuration(startTime));
		startTime = System.currentTimeMillis();


		double score = matchFingerprint(apdu);
		System.out.println("Matchin score = " + score);
//		System.out.println("Matching time : " + getDuration(startTime));
		startTime = System.currentTimeMillis();
		
		File folder = new File("C:/Users/jonathan/Desktop/JavaCardReader/" + folderOut + "/" + enrollTemplateFile.getName());
		if (!folder.exists()) {
			folder.mkdirs();
		}
		
		File fileOut = new File(folder.getAbsolutePath() + "/" + challengerFile.getName() + ".csv");
		printScoreToFile(score, fileOut);


		//
		//		testDatabase(apdu);
		//
		/* Mise hors tension de la carte */
//		try {
//			//			
//			cad.powerDown();
//		} catch (Exception e) {
//			System.out.println("Erreur lors de l'envoi de la commande Powerdown a la Javacard");
//			return;
//		}		
	}

	/**
	 * Tests the whole FVC 2000-2002-2004 databases against imposters score
	 * @param apdu the APDU to be sent
	 * @throws IOException if there is an Input/Output problem with the template file
	 * @throws CadTransportException if there is a communication error with the smart card
	 */
	public static void testDatabase(Apdu apdu, boolean randomizeKey) throws IOException, CadTransportException {
		ScoreTest cWatch = new ScoreTest();
		boolean enrolled = false;
		System.out.println(new File("res/").getAbsoluteFile());
		for (int user = 4; user < 100; user++) {
			cWatch.walk("res/1/1/user" + user + "/");

			File enrollTemplateFile = cWatch.getTemplates().get(0);
			System.out.println("Enroll user : " + enrollTemplateFile.getPath());
			if (!enrolled) {enrollUser(apdu, enrollTemplateFile,randomizeKey);}
			enrolled = true;
			for (File file : cWatch.getTemplates()) {
				if (file == enrollTemplateFile) {
					continue;
				}
				File folder = new File("C:/Users/jonathan/Desktop/JavaCardReader/genuine2/" + enrollTemplateFile.getName());
				if (!folder.exists()) {
					folder.mkdirs();
				}
				File fileOut = new File(folder.getAbsolutePath() + "/" + file.getName() + ".csv");
				System.out.println("fileOUT = " + fileOut.getAbsolutePath());

				System.out.println("Challenger : " + file.getAbsolutePath());
				loadChallenger(apdu, file, randomizeKey);

				double score = matchFingerprint(apdu);
				printScoreToFile(score, fileOut);

				apdu.command[0] = RESET;
				cad.exchangeApdu(apdu);

				//				System.out.println("Reponse : " + apdu.getStatus());

			}

		}



	}


	/**
	 * Print the scores in a text file
	 * @param scores a list of scores
	 * @param out the output file to be written
	 */
	private static void printScoreToFile(double score, File out) {
		//		DataOutputStream dos = new DataOutputStream(new FileOutputStream(out));
		PrintStream pStr;
		try {
			pStr = new PrintStream(new FileOutputStream(out));

			pStr.print(score);

			pStr.close();
//			System.out.println("Score printed at : " + out.getAbsolutePath());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Sends a matching command to the smartcards and compute
	 * the final score.
	 * @param apdu The APDU to be sent to the card
	 * @return a matching score between 0.0 and 1.0
	 * @throws IOException if there is an Input/Ouptut problem with the template file
	 * @throws CadTransportException if there is a communication error with the smart card
	 */
	private static double matchFingerprint(Apdu apdu) throws IOException, CadTransportException {
		apdu.command[Apdu.INS] = INS_MATCH;
		cad.exchangeApdu(apdu);
		//		System.out.println("Received APDU : " + apdu);
		double score = 0;
		if (apdu.getStatus() == 0x9000) {
			//			System.out.println("\t# -- transaction completed --");
			score = Util.makeShort(apdu.dataOut[1],  apdu.dataOut[0]) / 1550.0;
//			System.err.println("\tMatch done, final score : " + score);
		} else {
//			System.out.println("\t# -- transaction failed --");
		}
		return score;
	}

	/**
	 * Send an insruction command to the card for setting challenger's template on the card 
	 * @param apdu, the APDU request to be sent
	 * @param file Fingerprint file location on the disk (will change by the data acquired from the CAD)
	 * @throws IOException if there is an Input/Ouptut problem with the template file
	 * @throws CadTransportException if there is a communication error with the smart card
	 */
	private static void loadChallenger(Apdu apdu, File file, boolean randomizeKey) throws IOException, CadTransportException {
		load_template_on_card(apdu, false, file, randomizeKey);
		if (apdu.getStatus() == 0x9000) {
			//			System.out.println("\t# -- challenger template loaded --");
		} else {
			System.out.println("\t# -- challenger template failed --");
		}

		load_minutiae_on_card(apdu, false, file);
		if (apdu.getStatus() == 0x9000) {
			//			System.out.println("\t# -- challenger minutiae loaded --");
		} else {
			System.out.println("\t# -- challenger minutiae failed --");
		}
	}

	/**
	 * This method enroll a user's minutiae and transformed template by sending
	 * an enrollment command to the smart card
	 * @param apdu, the APDU request to be sent
	 * @param file Fingerprint file location on the disk (will change by the data
	 * @throws IOException if there is an Input/Ouptut problem with the template file
	 * @throws CadTransportException if there is a communication error with the smart card
	 */
	private static void enrollUser(Apdu apdu, File file, boolean randomizeKey) throws IOException, CadTransportException {
		//Enroll user template
		load_template_on_card(apdu, true, file, randomizeKey);
		if (apdu.getStatus() == 0x9000) {
			//			System.out.println("\t# -- user template enrolled --");
		} else {
			System.out.println("\t# -- user template failed --");
		}
		//Enroll user minutiae
		load_minutiae_on_card(apdu, true, file);
		if (apdu.getStatus() == 0x9000) {
			//			System.out.println("\t# -- user minutiae enrolled --");
		} else {
			System.out.println("\t# -- user minutiae failed --");
		}
	}

	/**
	 * This method acquire the minutia from a FVC file on the disk.
	 * This will be replaced by a connection to the card reader.
	 * @param file the fingerprint file location
	 * @return a short array containing minutation directions
	 */
	private static short[] acquire_minutia(File file) {
		ScoreTest cWatch = new ScoreTest();
		return cWatch.getMinutiaDirFromFile(file);
	}


	/**
	 * This method acquire the template from a FVC file on the disk.
	 * This will be replaced by a connection to the card reader.
	 * @param file The template location on the disk
	 * @return a byte array containing the transformed template
	 */
	private static byte[] acquire_template(File file, boolean different) {
		ScoreTest cWatch = new ScoreTest();
		ArrayList<Integer> key = ScoreTest.randomKey(different);
		printkey(key);
		return cWatch.generateRawTemplate(key, file, 1).clone();
	}

	private static void printkey(ArrayList<Integer> key) {
		//DEBUG
//		System.out.println("KEY = ");
		boolean stop = false;
		for (int i = 0; i < key.size()  ; i++) {
			if (i > 10 && !stop) {
				i = key.size() -10;
//				System.out.print(", ..., ");
				stop = true;
			}
//			System.out.print(key.get(i) + " ,");
		}
//		System.out.println();
	}

	/**
	 * This method extract data from a long byte array
	 * @param offset the beggining offset to cut
	 * @param length the length of the packet
	 * @param payload the data from which we extract a packet
	 * @return an array containing "length" bytes of data
	 */
	private static byte[] getDataPacket(int offset, int length, byte[] payload) {

		if (offset + length >= payload.length) {
			length = payload.length - offset;
		}
		byte[] packet = new byte[length];
		System.arraycopy(payload, offset, packet, 0, length);
		return packet;
	}

}

