package biometrics;

/**
 * Container class used to represent minutiae characteristics
 * @author Leila Mirmohamadsadeghi
 *
 */
public class Minutia implements Comparable<Minutia>{
	private int x;
	private int y;
	private double dir; // in rad 
	private int Q; // quality
	
	/**
	 * Initialization constructor.
	 * @param x horizontal coordinate in pixels
	 * @param y vertical coordinate in pixels
	 * @param dir direction in rad
	 */
	public Minutia(int x, int y, double dir, int Q){
		this.x = x;
		this.y = y;
		this.dir = dir;
		this.Q = Q;

	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public double getDir() {
		return dir;
	}
	
	public short getDegreeDir() {
		return (short) (dir * 180 / Math.PI);
	}
	
	public int getQ() {
		return Q;
	}


	@Override
	public int compareTo(Minutia o) {
		return new Integer(Q).compareTo(o.getQ());
	}
}
