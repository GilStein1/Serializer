package pack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Objects;

public class Serializer {

	private static final char FIELD_SPLIT_CHAR = ',';
	private static final char FIELD_NAME_SPLIT_CHAR = ':';

	public static String serialize(Object obj, Class<?> classOfObject) {
		if(!obj.getClass().equals(classOfObject)) {
			throw new IllegalArgumentException("Object is not of type " + classOfObject.getName());
		}
		try {
			return insideSerializing(obj, classOfObject);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Could not serialize object of type " + obj.getClass().getName());
		}
	}

	public static <T> T deserialize(String serializedValue, Class<T> classOfObject) {
		try {
			return insideDeserializing(serializedValue, classOfObject);
		} catch (NoSuchFieldException | InvocationTargetException | InstantiationException |
				 IllegalAccessException e) {
			throw new RuntimeException("Could not deserialize object of type " + classOfObject.getName());
		}
	}

	private static String insideSerializing(Object obj, Class<?> classOfObject) throws IllegalAccessException {
		StringBuilder serializedValue = new StringBuilder();
		Field[] fields = classOfObject.getDeclaredFields();
		for (Field field : fields) {
			if (!Modifier.isTransient(field.getModifiers())) {
				field.setAccessible(true);
				if (field.getType().isPrimitive() || field.getType() == String.class) {
					serializedValue
						.append(FIELD_SPLIT_CHAR)
						.append(field.getName())
						.append(FIELD_NAME_SPLIT_CHAR)
						.append(field.get(obj));
				} else {
					serializedValue
						.append(FIELD_SPLIT_CHAR)
						.append(field.getName())
						.append(FIELD_NAME_SPLIT_CHAR)
						.append(insideSerializing(field.get(obj), field.getType()));
				}
			}
		}
		return serializedValue
			.append("}")
			.replace(0, 1, "{")
			.toString();
	}

	public static <T> T insideDeserializing(String serializedValue, Class<T> classOfObject) throws NoSuchFieldException, InvocationTargetException, InstantiationException, IllegalAccessException {
		String valuesInside = serializedValue.substring(1, serializedValue.length() - 1);
		T object;
		try {
			object = classOfObject.getConstructor().newInstance();
		} catch (NoSuchMethodException e) {
			throw new NoEmptyConstructor(classOfObject);
		}
		String[] fields = splitStringByFields(valuesInside);
		for (String field : fields) {
			int indexOfSplit = field.indexOf(FIELD_NAME_SPLIT_CHAR);
			String fieldName = field.substring(0, indexOfSplit);
			String fieldValue = field.substring(indexOfSplit + 1);
			Field fieldOfClass = getFieldFromName(classOfObject, fieldName);
			Objects.requireNonNull(fieldOfClass).setAccessible(true);
			try {
				fieldOfClass.set(object, transformStringValueToObject(fieldValue, fieldOfClass.getType()));
			} catch (IllegalAccessException ignored) {

			}
		}
		return object;
	}

	private static Field getFieldFromName(Class<?> classOfObject, String fieldName) {
		Field currentField = null;
		for (Field f : classOfObject.getDeclaredFields()) {
			if (f.getName().equals(fieldName)) {
				currentField = f;
			}
		}
		return currentField;
	}

	private static String[] splitStringByFields(String str) {
		boolean stop = false;
		ArrayList<String> fields = new ArrayList<>();
		while (!stop) {
			int index = str.indexOf(FIELD_SPLIT_CHAR);
			String field;
			if (index == -1) {
				field = str;
			} else {
				field = str.substring(0, index);
			}
			if (!field.contains("{")) {
				fields.add(field);
				if (index == -1) {
					stop = true;
				} else {
					str = str.substring(index + 1);
				}
			} else {
				index = str.indexOf("}");
				field = str.substring(0, index + 1);
				fields.add(field);
				str = str.substring(index + 2);
			}
			if (str.isEmpty()) {
				stop = true;
			}
		}
		return fields.toArray(new String[0]);
	}

	private static Object transformStringValueToObject(String value, Class<?> classOfObject) throws NoSuchFieldException, InvocationTargetException, InstantiationException, IllegalAccessException {
		String className = classOfObject.getName();
		return switch (className) {
			case "boolean" -> Boolean.parseBoolean(value);
			case "byte" -> Byte.parseByte(value);
			case "short" -> Short.parseShort(value);
			case "int" -> Integer.parseInt(value);
			case "long" -> Long.parseLong(value);
			case "float" -> Float.parseFloat(value);
			case "double" -> Double.parseDouble(value);
			case "java.lang.String" -> value;
			default -> insideDeserializing(value, classOfObject);
		};
	}

}
