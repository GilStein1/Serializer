package pack;

import serializer.Serializer;

public class Main {
	public static void main(String[] args) {

		Test t = new Test(13, 74.2, 'h', false, 123l, (short) 32, "isdjfgidfhgkej");

		System.out.println(Serializer.serialize(t, Test.class));

		Test t2 = Serializer.log().deserialize(Serializer.serialize(t, Test.class), Test.class);

		System.out.println(Serializer.serialize(t2, Test.class));

	}
}
