package serializer;

public class LoggedSerializer {

	private static LoggedSerializer instance;
	private final Serializer serializer;

	static LoggedSerializer getInstance() {
		if (instance == null) {
			instance = new LoggedSerializer();
		}
		return instance;
	}

	private LoggedSerializer() {
		serializer = new Serializer();
		serializer.setLogged();
	}

	public String serialize(Object obj, Class<?> classOfObject) {
		return serializer.serializeWithWrappedExceptions(obj, classOfObject);
	}

	public <T> T deserialize(String serializedValue, Class<T> classOfObject) {
		return serializer.deserializeWithWrappedExceptions(serializedValue, classOfObject);
	}

}
