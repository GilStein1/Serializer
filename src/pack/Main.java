package pack;

import pack.serializer.LoggedSerializer;
import pack.serializer.Serializer;

import java.util.ArrayList;

public class Main {
	public static void main(String[] args) {

//		ArrayList<Integer> list = new ArrayList<>();
//		list.add(1);
//		list.add(2);
//		list.add(3);
//		list.add(4);
//
//		System.out.println(Serializer.log().serialize(list, ArrayList.class));

		Test t = new Test(1, 2);

		t.setTest(t);

		Test2 test2 = new Test2(t, 12, "Yotam said \"Hello\"");

		String value = Serializer.serialize(test2, Test2.class);

		System.out.println(value);

		Test2 t2 = Serializer.deserialize(value, Test2.class);

		System.out.println(Serializer.serialize(t2, Test2.class));

		System.out.println(t2.getText());

	}
}