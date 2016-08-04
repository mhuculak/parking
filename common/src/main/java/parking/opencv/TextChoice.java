package parking.opencv;

public class TextChoice {
	
	private char value;
	private double score;

	public TextChoice(char value) {
		this.value = value;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public char getText() {
		return value;
	} 

	public double getScore() {
		return score;
	}

	public String toString() {
		return value+" "+score;
	}
}