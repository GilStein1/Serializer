package pack;

public class Test2 {
	public Test2() {

	}
	private Test test;
	private int aa;
	private String text;
	public Test2(Test test, int aa, String text) {
		this.test = test;
		this.aa = aa;
		this.text = text;
	}
	public String getText() {
		return text;
	}
	public Test getTest() {
		return test;
	}
	public void setTest(Test test) {
		this.test = test;
	}
	public int getAa() {
		return aa;
	}
	public void setAa(int aa) {
		this.aa = aa;
	}
}
