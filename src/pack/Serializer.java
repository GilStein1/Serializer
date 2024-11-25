package pack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Objects;

public class Serializer {

	private static final char FIELD_SPLIT_CHAR = ',';
	private static final char FIELD_NAME_SPLIT_CHAR = ':';
	private final ArrayList<Object> serializedObjects;

	public Serializer() {
		this.serializedObjects = new ArrayList<>();
	}

	public String serialize(Object obj, Class<?> classOfObject) {
		if (!obj.getClass().equals(classOfObject)) {
			throw new IllegalArgumentException("Object is not of type " + classOfObject.getName());
		}
		serializedObjects.clear();
		try {
			return insideSerializing(obj, classOfObject);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Could not serialize object of type " + obj.getClass().getName());
		} finally {
			serializedObjects.clear();
		}
	}

	public <T> T deserialize(String serializedValue, Class<T> classOfObject) {
		serializedObjects.clear();
		try {
			return insideDeserializing(serializedValue, classOfObject);
		} catch (NoSuchFieldException | InvocationTargetException | InstantiationException |
				 IllegalAccessException e) {
			throw new RuntimeException("Could not deserialize object of type " + classOfObject.getName());
		} finally {
			serializedObjects.clear();
		}
	}

	private String insideSerializing(Object obj, Class<?> classOfObject) throws IllegalAccessException {
		serializedObjects.add(obj);
		StringBuilder serializedValue = new StringBuilder();
		Field[] fields = classOfObject.getDeclaredFields();
		for (Field field : fields) {
			if (
				!Modifier.isTransient(field.getModifiers())
					&& !Modifier.isStatic(field.getModifiers())
					&& field.trySetAccessible()
			) {
				Object fieldFromObject = field.get(obj);
				if (fieldFromObject != null) {
					if (serializedObjects.contains(fieldFromObject)) {
						serializedValue
							.append(FIELD_SPLIT_CHAR)
							.append(field.getName())
							.append(FIELD_NAME_SPLIT_CHAR)
							.append("~").append(serializedObjects.indexOf(fieldFromObject)).append("~");
					} else if (field.getType().isPrimitive() || field.getType() == String.class) {
						serializedValue
							.append(FIELD_SPLIT_CHAR)
							.append(field.getName())
							.append(FIELD_NAME_SPLIT_CHAR)
							.append(fieldFromObject);
					} else {
						serializedValue
							.append(FIELD_SPLIT_CHAR)
							.append(field.getName())
							.append(FIELD_NAME_SPLIT_CHAR)
							.append(insideSerializing(fieldFromObject, field.getType()));
					}
					if (!serializedObjects.contains(fieldFromObject)) {
						serializedObjects.add(fieldFromObject);
					}
				}
			}
		}
		return serializedValue
			.append("}")
			.replace(0, 1, "{")
			.toString();
	}

	public <T> T insideDeserializing(String serializedValue, Class<T> classOfObject) throws NoSuchFieldException, InvocationTargetException, InstantiationException, IllegalAccessException {
		String valuesInside = serializedValue.substring(1, serializedValue.length() - 1);
		T object;
		try {
			object = classOfObject.getConstructor().newInstance();
		} catch (NoSuchMethodException e) {
			throw new NoEmptyConstructor(classOfObject);
		}
		serializedObjects.add(object);
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

	private Field getFieldFromName(Class<?> classOfObject, String fieldName) {
		Field currentField = null;
		for (Field f : classOfObject.getDeclaredFields()) {
			if (f.getName().equals(fieldName)) {
				currentField = f;
			}
		}
		return currentField;
	}

	private String[] splitStringByFields(String str) {
		boolean stop = false;
		ArrayList<String> fields = new ArrayList<>();
		while (!stop) {
			int index = str.indexOf(FIELD_SPLIT_CHAR);
			String field = index != -1 ? str.substring(0, index) : str;
			if (!field.contains("{")) {
				fields.add(field);
				str = index != -1 ? str.substring(index + 1) : "";
			} else {
				index = str.indexOf("}");
				field = str.substring(0, index + 1);
				fields.add(field);
				str = str.substring(index + 2);
			}
			stop = str.isEmpty();
		}
		return fields.toArray(new String[0]);
	}

	private Object transformStringValueToObject(String value, Class<?> classOfObject) throws NoSuchFieldException, InvocationTargetException, InstantiationException, IllegalAccessException {
		String className = classOfObject.getName();
		if (classOfObject != String.class && value.startsWith("~") && value.endsWith("~")) {
			String indexPart = value.substring(1, value.length() - 1);
			int index = Integer.parseInt(indexPart);
			return serializedObjects.get(index);
		}
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
