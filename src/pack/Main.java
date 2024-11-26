package pack;

public class Main {
	public static void main(String[] args) {

		Test t = new Test(1, 2);

		t.setTest(t);

		Test2 test2 = new Test2(t, 12);

		String value = Serializer.serialize(test2, Test2.class);

		System.out.println(value);

		Test2 t2 = Serializer.deserialize(value, Test2.class);

		System.out.println(t2.getTest().getTest() == t2.getTest());

		System.out.println(Serializer.serialize(t2, Test2.class));

	}
}