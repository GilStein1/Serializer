package pack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Objects;

public class Serializer {

	private static final char FIELD_SPLIT_CHAR = ',';
	private static final char FIELD_NAME_SPLIT_CHAR = ':';

	public static String serialize(Object obj, Class<?> classOfObject) {
		try {
			return insideSerializing(obj, classOfObject);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Could not serialize object of type " + obj.getClass().getName());
		}
	}

	public static <T> T deserialize(String serializedValue, Class<T> classOfObject) {
		try {
			return insideDeserializing(serializedValue, classOfObject);
		} catch (NoSuchFieldException | NoSuchMethodException | InvocationTargetException | InstantiationException |
				 IllegalAccessException e) {
			throw new RuntimeException("Could not deserialize object of type " + classOfObject.getName());
		}
	}

	private static String insideSerializing(Object obj, Class<?> classOfObject) throws IllegalAccessException {
		StringBuilder serializedValue = new StringBuilder();
		Field[] fields = classOfObject.getDeclaredFields();
		for (Field field : fields) {
			field.setAccessible(true);
			if (field.getType().isPrimitive()) {
				serializedValue
					.append(FIELD_SPLIT_CHAR)
					.append(field.getName())
					.append(FIELD_NAME_SPLIT_CHAR)
					.append(field.get(obj));
			} else if (field.getType() == String.class) {
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
		return serializedValue
			.append("}")
			.replace(0, 1, "{")
			.toString();
	}

	public static <T> T insideDeserializing(String serializedValue, Class<T> classOfObject) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		String valuesInside = serializedValue.substring(1, serializedValue.length() - 1);
		T object = classOfObject.getConstructor().newInstance();
		String[] fields = splitStringByFields(valuesInside);
		for (String field : fields) {
			int indexOfSplit = field.indexOf(FIELD_NAME_SPLIT_CHAR);
			String fieldName = field.substring(0, indexOfSplit);
			String fieldValue = field.substring(indexOfSplit + 1);
			Field currentField = null;
			for (Field f : classOfObject.getDeclaredFields()) {
				if (f.getName().equals(fieldName)) {
					currentField = f;
				}
			}
			Field fieldOfClass = currentField;
			Objects.requireNonNull(fieldOfClass).setAccessible(true);
			if (fieldOfClass.getType().isPrimitive()) {
				fieldOfClass.set(object, transformStringValueToPrimitiveValue(fieldValue, fieldOfClass.getType()));
			} else if (fieldOfClass.getType() == String.class) {
				fieldOfClass.set(object, fieldValue);
			} else {
				fieldOfClass.set(object, insideDeserializing(fieldValue, fieldOfClass.getType()));
			}
		}
		return object;
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
				str = str.substring(index + 1);
			}
			if (str.isEmpty()) {
				stop = true;
			}
		}
		return fields.toArray(new String[0]);
	}

	private static Object transformStringValueToPrimitiveValue(String value, Class<?> classOfObject) {
		String className = classOfObject.getName();
		return switch (className) {
			case "boolean" -> Boolean.parseBoolean(value);
			case "byte" -> Byte.parseByte(value);
			case "short" -> Short.parseShort(value);
			case "int" -> Integer.parseInt(value);
			case "long" -> Long.parseLong(value);
			case "float" -> Float.parseFloat(value);
			case "double" -> Double.parseDouble(value);
			default -> value;
		};
	}

}
