package biometrics;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class provide methods for loading and transforming a biometric template
 * @author Jonathan Cheseaux (jonathan.cheseaux@epfl.ch)
 * @author (some parts) Leila Mirmohamadsadeghi
 *
 */
public class MCCBase {

	/** cylinder discretization in spatial dimension (base) **/
	private int ns;
	/** cylinder discretization in directional dimension (height) **/
	private int nd; 
	/** radius of cylinder **/
	private int r;
	/** collection of minutiae of the fingerprint **/
	private ArrayList<Minutia> minutiae;
	/** spatial smoothing **/
	private double sigmaS;
	/** directional smoothing **/
	private double sigmaD;
	
	/**
	 * By default constructor, initializes all parameters to 0. except log file.
	 */
	public MCCBase(){
		this.ns = 0;
		this.nd = 0;
		this.r = 0;
		this.minutiae = null;
		this.sigmaS = 0;
		this.sigmaD = 0;
	}
	/**
	 * Initialization constructor. To be called when computing a cylinder set from a minutiae set
	 * @param ns cylinder base discretization step
	 * @param nd cylinder height discretization step
	 * @param r cylinder radius
	 * @param minutiae list of minutiae extracted from fingerprint
	 * @param sigmaS spatial standard deviation
	 * @param muPsi binarization threshold
	 * @param sigmaD directional standard deviation
	 * @param minVC minimum valid cells necessary for cylinder validity
	 * @param minM minimum contributing minutiae necessary for cylinder validity
	 * @param outLOG LOG file
	 */
	public MCCBase(int ns, int nd, int r, ArrayList<Minutia> minutiae, double sigmaS, double sigmaD){

		this.ns = ns;
		this.nd = nd;
		this.r = r;
		this.minutiae = minutiae;
		this.sigmaS = sigmaS;
		this.sigmaD = sigmaD;

	}
	
	/**
	 * Computes the spatial contribution of a minutia mt to a cylinder centered at pX, pY
	 * @param mt neighboring minutia
	 * @param pX horizontal coordinate of cell center
	 * @param pY vertical coordinate of cell center
	 * @return spatial contribution of minutia mt on current cell
	 */
	public double cmS(Minutia mt, double pX, double pY){
		double t = ds(mt, pX, pY);
		double val = (1/(this.sigmaS*Math.sqrt(2*Math.PI)))*gaus(t, this.sigmaS);
		return val;

	}
	
	/**
	 * This function applies a transformation on the template which diversifies the latter and attempts to provide irreversibility properties
	 * @param linTemplate the original template
	 * @param H the transformation key
	 * @param flag 0 for the double sum-square-modulo transformation, 1 for the binarized version
	 * @return transformed diversified template
	 */
	public byte[] transform(ArrayList<double[]> linTemplate, ArrayList<Integer> H, int flag, double A, double n){
		byte[] linTemplateQuant = new byte[linTemplate.size() * 1024 / 8]; 
		int k = 0;
		int index = 0;
		for(int i = 0; i < linTemplate.size(); i++){
			double[] currentCyl = Arrays.copyOf(linTemplate.get(i), linTemplate.get(i).length);

			byte[] temp = new byte[8];
			for(int j = 0; j < currentCyl.length; j++){
				if((double)j%2.0 == 0){
					// double sum-square-modulo transformation
					double d1 = (A*(currentCyl[H.get(j)]+currentCyl[H.get(j+1)]));
					double c1 = (d1*d1)%n; // ciphertext

					if(flag == 1){
						if(c1 > 100000){
							temp[index++] = 1;
						}else{
							temp[index++] = 0;
						}
					}
					if (index == 8) {
						linTemplateQuant[k] = utils.Utils.bitsArrayToByte(temp);
						temp = new byte[8];
						k++;
						index = 0;
					}
					
				}
			}
		}
		return linTemplateQuant;
	}
	
	/**
	 * Euclidean distance between minutia m and point (pX, pY)
	 * @param m central minutia
	 * @param pX horizontal coordinate of cell center
	 * @param pY vertical coordinate of cell center
	 * @return Euclidean distance between minutia m and point (pX, pY)
	 */
	public double ds(Minutia m, double pX, double pY){
		double val = Math.sqrt(Math.pow((double)m.getX()-pX, 2)+Math.pow((double)m.getY()-pY, 2));
		return val;

	}
	/**
	 * Computes the directional contribution of a minutia mt to a cylinder centered at pX, pY
	 * @param m central minutia
	 * @param mt neighboring minutia
	 * @param dphiK direction of current cell
	 * @return directional contribution of neighbor on current cell
	 */
	public double cmD(Minutia m, Minutia mt, double dphiK){
		double diff1 = angularDiff(m.getDir(), mt.getDir());
		double diff = angularDiff(dphiK, diff1);
		double deltaD = 2*Math.PI/(double)this.nd;
		double val = 0;
		diff = Math.abs(diff);
		if(diff == 0){
			val = 0.5*deltaD*(gaus(0, this.sigmaD)-gaus(0.5*deltaD, this.sigmaD));
		}
		else{
			val = 0.75*deltaD*gaus(diff, this.sigmaD)+0.25*deltaD*gaus(diff-0.5*deltaD, this.sigmaD);
		}
		val = (1/(this.sigmaD*Math.sqrt(2*Math.PI)))*val;
		return val;
	}
	
	/**
	 * Computes value on a Gaussian with zero mean and standard deviation sigma
	 * @param x value to be scaled on the Gaussian
	 * @param sigma standard deviation of Gaussian
	 * @return Value on zero-mean Gaussian of specified width.
	 */
	public double gaus(double x, double sigma){
		return Math.exp(-1*Math.pow(x, 2)/(2*Math.pow(sigma, 2)));
	}
	
	/**
	 * Computes the angular difference between two given angles t1 and t2
	 * @param t1
	 * @param t2 angles whose difference is required.
	 * @return diff angular difference of t1 and t2 according to (9) in reference paper.
	 */
	public static double angularDiff(double t1, double t2){
		double diff = 0;
		if((t1-t2) < Math.PI && (t1-t2) >= -1*Math.PI)
			diff = t1-t2;
		else if((t1-t2) < -1*Math.PI)
			diff = 2*Math.PI+t1-t2;
		else if((t1-t2) >= Math.PI)
			diff = -2*Math.PI+t1-t2;
		return diff;
	}

	/**
	 * Compute the untransformed template from the fingerprint impression
	 * @return A list of double array
	 */
	public ArrayList<double[]> computeTemplate(){
		// cylFile is the file in which to write the computed cylinders
		ArrayList<double[]> result = new ArrayList<double[]>();

		double deltaD = 2*Math.PI/((double)(this.nd));
		double deltaS = 2*(double)this.r/(double)this.ns;
		double mu = 0.01;
		double tau = 400;
		// loop over minutiae
		for(int f = 0; f < this.minutiae.size(); f++){
			Minutia m = this.minutiae.get(f);
			double x = (double) m.getX();
			double y = (double) m.getY();
			double t = (double) m.getDir();

			double[] linCylD = new double[this.ns*this.ns*this.nd]; // current cylinder in its linearized form

			double[] dphi = new double[this.nd];

			// loop over i and j to compute p_ij
			for(int i = 0; i < this.ns; i++){
				for(int j = 0; j < this.ns; j++){
					double s_t = Math.sin(t);
					double c_t = Math.cos(t);
					double ind = ((double)this.ns+1)/2;
					double di = (double)i+1;
					double dj = (double)j+1;
					double pX_ij = x + deltaS*(c_t*(di-ind)+s_t*(dj-ind));
					double pY_ij = y + deltaS*(-1*s_t*(di-ind)+c_t*(dj-ind));

					ArrayList<Minutia> neighbors = new ArrayList<Minutia>();
					for(int nt = 0; nt < this.minutiae.size(); nt++){
						Minutia mt = this.minutiae.get(nt);	
						if((f != nt)&&(ds(mt, pX_ij, pY_ij) <= 3*sigmaS)){
							neighbors.add(mt);
						}
					}
					for(int k = 0; k < this.nd; k++){
						dphi[k]= -1*Math.PI+ ((double)k+0.5)*deltaD;
						int indice = (int)(k*ns*ns+j*ns+i+1)-1;	
						linCylD[indice] = 0;
						for(int u = 0; u< neighbors.size(); u++){
							double cmsVal = cmS(neighbors.get(u), pX_ij, pY_ij);
							double cmdVal = cmD(m, neighbors.get(u), dphi[k]);

							linCylD[indice] += cmdVal*cmsVal;	


						} // end loop over neighbors
						linCylD[indice] = sigmoid(linCylD[indice], mu, tau);
//						System.out.println(linCylD[indice]);
					} // end loop over k
				} // end loop over j
			} // end loop over i
			result.add(linCylD);
		}
		return result;
	}
	
	/**
	 * Computes the sigmoid function according to (5) in the main paper.
	 * @param nu
	 * @param mu
	 * @param tau
	 * @return double Z(nu, mu, tau)
	 */
	public double sigmoid(double nu, double mu, double tau){
		return 1/(1+Math.exp(-tau*(nu-mu)));
	}


	
}