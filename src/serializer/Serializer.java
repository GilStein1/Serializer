package serializer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static serializer.StringUtils.*;

public class Serializer {

	private static final char FIELD_SPLIT_CHAR = ',';
	private static final char FIELD_NAME_SPLIT_CHAR = ':';
	private static final String SERIALIZATION_INSTANCE_CHARACTER = "~";
	private static final char CHAR_REPLACING_STRING_FRAME = (char) 67284345;
	private static Serializer instance;
	protected boolean isLogged;

	Serializer() {
		isLogged = false;
	}

	static Serializer getInstance() {
		if (instance == null) {
			instance = new Serializer();
		}
		return instance;
	}

	/**
	 * The method to convert objects to Strings
	 *
	 * @param obj           the object to be serialized
	 * @param classOfObject the class of the object
	 * @return a String that describes the object
	 * @throws RuntimeException         when the serialization fails
	 * @throws IllegalArgumentException when the classOfObject does not
	 *                                  match the class of the given object
	 */
	public static String serialize(Object obj, Class<?> classOfObject) {
		return getInstance().serializeWithWrappedExceptions(obj, classOfObject);
	}

	/**
	 * The method to convert Strings back to objects
	 *
	 * @param serializedValue the serialized object
	 * @param classOfObject   the class of the object
	 * @return the deserialized object
	 * @throws RuntimeException   when the deserialization fails
	 * @throws NoEmptyConstructor when the class of the serialized object
	 *                            does not have an empty constructor
	 * @implNote the class of the serialized object must have a public empty constructor
	 * with no arguments for the deserializer to be able to deserialize it
	 */
	public static <T> T deserialize(String serializedValue, Class<T> classOfObject) {
		return getInstance().deserializeWithWrappedExceptions(serializedValue, classOfObject);
	}

	public static LoggedSerializer log() {
		return LoggedSerializer.getInstance();
	}

	String serializeWithWrappedExceptions(Object obj, Class<?> classOfObject) {
		if (!obj.getClass().equals(classOfObject)) {
			throw new IllegalArgumentException("Object is not of type " + classOfObject.getName());
		}
		try {
			return insideSerializing(obj, classOfObject, new ArrayList<>());
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Could not serialize object of type " + obj.getClass().getName());
		}
	}

	private String insideSerializing(Object obj, Class<?> classOfObject, ArrayList<Object> serializedObjects) throws IllegalAccessException {
		serializedObjects.add(obj);
		StringBuilder serializedValue = new StringBuilder();
		Field[] fields = classOfObject.getDeclaredFields();
		for (Field field : fields) {
			if (
				!Modifier.isTransient(field.getModifiers())
					&& !Modifier.isStatic(field.getModifiers())
					&& field.trySetAccessible()
			) {
				logValue(
					"serializing object of type "
						+ field.getType().getName()
						+ " called "
						+ field.getName()
				);
				Object fieldFromObject = field.get(obj);
				if (fieldFromObject != null) {
					if (serializedObjects.contains(fieldFromObject)) {
						serializedValue
							.append(FIELD_SPLIT_CHAR)
							.append(field.getName())
							.append(FIELD_NAME_SPLIT_CHAR)
							.append(SERIALIZATION_INSTANCE_CHARACTER)
							.append(serializedObjects.indexOf(fieldFromObject))
							.append(SERIALIZATION_INSTANCE_CHARACTER);
					} else if (field.getType().isPrimitive() || field.getType() == String.class) {
						serializedValue
							.append(FIELD_SPLIT_CHAR)
							.append(field.getName())
							.append(FIELD_NAME_SPLIT_CHAR)
							.append(
								field.getType() != String.class ?
									fieldFromObject :
									makeStringFrame(
										((String) fieldFromObject)
											.replaceAll("\"", String.valueOf(CHAR_REPLACING_STRING_FRAME)),
										'\"'
									)
								);
					} else {
						serializedValue
							.append(FIELD_SPLIT_CHAR)
							.append(field.getName())
							.append(FIELD_NAME_SPLIT_CHAR)
							.append(insideSerializing(fieldFromObject, field.getType(), serializedObjects));
					}
					if (!serializedObjects.contains(fieldFromObject)) {
						serializedObjects.add(fieldFromObject);
					}
				} else {
					logValue("field " + field.getName() + " is null");
				}
			} else {
				if (Modifier.isTransient(field.getModifiers())) {
					logValue("skipped field " + field.getName() + " because it is transient");
				}
				if(Modifier.isStatic(field.getModifiers())) {
					logValue("skipped field " + field.getName() + " because it is static");
				}
				if(!field.trySetAccessible()) {
					logValue("couldn't access field " + field.getName());
				}
			}
		}
		return serializedValue
			.append("}")
			.replace(0, 1, "{")
			.toString();
	}

	<T> T deserializeWithWrappedExceptions(String serializedValue, Class<T> classOfObject) {
		try {
			return insideDeserializing(serializedValue, classOfObject, new ArrayList<>());
		} catch (NoSuchFieldException | InvocationTargetException | InstantiationException |
				 IllegalAccessException e) {
			throw new RuntimeException("Could not deserialize object of type " + classOfObject.getName());
		}
	}

	public <T> T insideDeserializing(String serializedValue, Class<T> classOfObject, List<Object> serializedObjects) throws NoSuchFieldException, InvocationTargetException, InstantiationException, IllegalAccessException {
		String valuesInside = cutStringFrame(serializedValue);
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
			logValue("deserializing object of type " + fieldOfClass.getType().getName() + " called " + fieldName);
			Objects.requireNonNull(fieldOfClass).setAccessible(true);
			try {
				fieldOfClass.set(object, transformStringValueToObject(fieldValue, fieldOfClass.getType(), serializedObjects));
			} catch (IllegalAccessException ignored) {
				logValue("could not deserialize field called " + fieldName);
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
			String field = index != NULL_INDEX ? str.substring(0, index) : str;
			if(field.contains("{") || field.contains("\"")) {
				index = field.contains("{") ? str.indexOf("}") : indexOfTheN(str, '\"', 2);
				field = str.substring(0, index + 1);
				fields.add(field);
				logValue("read field " + field);
				str = str.substring((str.length() > index + 2) ? (index + 2) : (index + 1));
			}
			else {
				fields.add(field);
				logValue("read field " + field);
				str = index != NULL_INDEX ? str.substring(index + 1) : EMPTY_STRING;
			}
			stop = str.isEmpty();
		}
		return fields.toArray(new String[0]);
	}

	private Object transformStringValueToObject(String value, Class<?> classOfObject, List<Object> serializedObjects) throws NoSuchFieldException, InvocationTargetException, InstantiationException, IllegalAccessException {
		logValue(
			"Value to turn to object -> " + value
				+ ", Class of that object -> " + classOfObject.getName()
		);
		String className = classOfObject.getName();
		if (classOfObject != String.class && value.startsWith(SERIALIZATION_INSTANCE_CHARACTER) && value.endsWith(SERIALIZATION_INSTANCE_CHARACTER)) {
			logValue("looping instance of class " + className + " detected");
			String indexPart = cutStringFrame(value);
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
			case "java.lang.String" -> cutStringFrame(value).replaceAll(String.valueOf(CHAR_REPLACING_STRING_FRAME), "\"");
			default -> insideDeserializing(value, classOfObject, serializedObjects);
		};
	}

	private void logValue(CharSequence value) {
		if (isLogged) {
			System.out.println(value + "\n");
		}
	}

	void setLogged() {
		this.isLogged = true;
	}

}
