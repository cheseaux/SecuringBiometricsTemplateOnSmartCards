package monpackage;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

/**
 * Base applet which will be executed on the card
 * @author Jonathan Cheseaux (jonathan.cheseaux@epfl.ch)
 *
 */
public class MonApplet extends Applet {

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

	/** The enrollment template **/
	private byte[] template;

	/** The challenger's template **/
	private byte[] external_template;

	/** The enrollment minutiae **/
	private short[] minutiae;

	/** The challenger's minutiae */
	private short[] external_minutiae;

	/** Challenger current template packet number received **/
	private short tempCapNumber = 0;

	/** User current template packet number received **/
	private short tempEnrollNumber = 0;

	/** The LSSMatcher instance responsible for computing the matching score **/
	private static LSSMatcher mccBase;

	/**
	 * Builder
	 */
	private MonApplet() {
		mccBase = new LSSMatcher();
	}

	/**
	 * Installs the applet on the Card
	 * @param bArray
	 * @param bOffset
	 * @param bLength
	 * @throws ISOException
	 */
	public static void install(byte bArray[], short bOffset, byte bLength) throws ISOException {
		new MonApplet().register();
	}

	/**
	 * Store a data packet on the card
	 * @param apdu the APDU request received
	 * @param dest the destination array
	 * @param index the index of the current packet treated
	 */
	public void save_received_buffer(APDU apdu, byte[] dest, short index) {
		apdu.setIncomingAndReceive();
		byte[] rcvd = apdu.getBuffer();
		short size = Util.makeShort(rcvd[ISO7816.OFFSET_P2], rcvd[ISO7816.OFFSET_P1]);
		if (dest == null) {
			dest = new byte[size];
		}
		short packetSize = PACKET_SIZE;

		if ((short) (index *  PACKET_SIZE) + PACKET_SIZE >= size) {
			packetSize = (short) (size - (short) (index *  PACKET_SIZE));
		}
		Util.arrayCopy(rcvd, apdu.getOffsetCdata(), dest, (short) (index *  PACKET_SIZE) , packetSize);
		rcvd[0] = (byte) 69;
		apdu.setOutgoingAndSend((short) 0, (short) 1);
	}

	/**
	 * Process received APDU's
	 */
	public void process(APDU apdu) throws ISOException {
		byte[] buffer = apdu.getBuffer();

		if (this.selectingApplet()) return;

		if (buffer[ISO7816.OFFSET_CLA] != CLA_MONAPPLET) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}

		switch (buffer[ISO7816.OFFSET_INS]) {

		case INS_SET_MINUTIA_CAP:
			apdu.setIncomingAndReceive();
			byte[] rcvd = apdu.getBuffer();
			short size = Util.makeShort(rcvd[ISO7816.OFFSET_P2], rcvd[ISO7816.OFFSET_P1]);
			if (external_minutiae == null) {
				external_minutiae = new short[size];
			}

			int k = 0;
			for (int i = apdu.getOffsetCdata(); i < apdu.getOffsetCdata() + size -1; i+=2) {
				external_minutiae[k] = 
						Util.makeShort(
								apdu.getBuffer()[i+1],apdu.getBuffer()[i]);
				k++;
			}

			rcvd[0] = (byte) 69;
			apdu.setOutgoingAndSend((short) 0, (short) 1);
			break;

		case INS_SET_TEMPLATE_CAP:
			apdu.setIncomingAndReceive();
			rcvd = apdu.getBuffer();
			size = Util.makeShort(rcvd[ISO7816.OFFSET_P2], rcvd[ISO7816.OFFSET_P1]);
			if (external_template == null) {
				external_template = new byte[size];
			}
			short packetSize = PACKET_SIZE;

			if ((short) (tempCapNumber *  PACKET_SIZE) + PACKET_SIZE >= size) {
				packetSize = (short) (size - (short) (tempCapNumber *  PACKET_SIZE));
			}

			Util.arrayCopy(rcvd, apdu.getOffsetCdata(), external_template, (short) (tempCapNumber *  PACKET_SIZE) , packetSize);


			rcvd[0] = (byte) 69;
			apdu.setOutgoingAndSend((short) 0, (short) 1);
			++tempCapNumber;
			break;

		case INS_ENROLL_TEMPLATE:
			apdu.setIncomingAndReceive();
			rcvd = apdu.getBuffer();
			size = Util.makeShort(rcvd[ISO7816.OFFSET_P2], rcvd[ISO7816.OFFSET_P1]);
			if (template == null) {
				template = new byte[size];
			}
			packetSize = PACKET_SIZE;

			if ((short) (tempEnrollNumber *  PACKET_SIZE) + PACKET_SIZE >= size) {
				packetSize = (short) (size - (short) (tempEnrollNumber *  PACKET_SIZE));
			}

			Util.arrayCopy(rcvd, apdu.getOffsetCdata(), template, (short) (tempEnrollNumber *  PACKET_SIZE) , packetSize);
			++tempEnrollNumber;
			break;
		case INS_ENROLL_MINUTIAE:
			apdu.setIncomingAndReceive();
			rcvd = apdu.getBuffer();
			size = Util.makeShort(rcvd[ISO7816.OFFSET_P2], rcvd[ISO7816.OFFSET_P1]);
			if (minutiae == null) {
				minutiae = new short[size];
			}

			k = 0;
			for (int i = apdu.getOffsetCdata(); i < apdu.getOffsetCdata() + size -1; i+=2) {
				minutiae[k] = 
						Util.makeShort(
								apdu.getBuffer()[i+1],apdu.getBuffer()[i]);
				k++;
			}

			rcvd[0] = (byte) 69;
			apdu.setOutgoingAndSend((short) 0, (short) 1);
			break;
		case INS_MATCH:

			short score = mccBase.match(template, external_template, minutiae, external_minutiae, apdu);
			apdu.getBuffer()[0] = (byte) (score & 0xFF);
			apdu.getBuffer()[1] = (byte) ((score>>8)&0xFF);
			apdu.setOutgoingAndSend((short) 0, (short) 2);
			break;
		case RESET:
			tempCapNumber = 0;
			external_minutiae = null;
			external_template = null;

			apdu.getBuffer()[0] = (byte) (69);
			apdu.setOutgoingAndSend((short) 0, (short) 1);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

}