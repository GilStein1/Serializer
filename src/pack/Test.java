package pack;

public class Test {

	int a, b;

	private Test2 test2;

	transient boolean is;

	String s;

	private static final int x = 5;

	public Test(int a, int b, boolean is, String s, Test2 test2) {
		this.a = a;
		this.b = b;
		this.is = is;
		this.s = s;
		this.test2 = test2;
	}

	public Test() {

	}

}
