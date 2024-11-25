package pack;

import javax.swing.*;

public class Main {
	public static void main(String[] args) {

		Test t = new Test(25, 61, true, "Hello", new Test2(90, 81));

		String serializedValue = Serializer.serialize(t, Test.class);

		System.out.println(serializedValue);

		Test test = Serializer.deserialize(serializedValue, Test.class);

		System.out.println(Serializer.serialize(test, Test.class));

	}
}