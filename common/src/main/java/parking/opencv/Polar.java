package parking.opencv;

class Polar {
	public double r;
	public double theta;

	public Polar(double r, double theta) {
		this.r = r;
		this.theta = theta;
	}

	public Polar(Vector v) {
		this.r = v.length;
		this.theta = v.angle;
	}

	public String toString(){
		return r+" "+theta;
	}
}