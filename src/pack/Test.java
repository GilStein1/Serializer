package pack;

public class Test {

	private int a, b;

	private Test test;

	public Test(int a, int b, Test test) {
		this.a = a;
		this.b = b;
		this.test = test;
	}

	public Test(int a, int b) {
		this.a = a;
		this.b = b;
	}

	public Test() {

	}

	public int getA() {
		return a;
	}

	public void setA(int a) {
		this.a = a;
	}

	public int getB() {
		return b;
	}

	public void setB(int b) {
		this.b = b;
	}

	public Test getTest() {
		return test;
	}

	public void setTest(Test test) {
		this.test = test;
	}
}
