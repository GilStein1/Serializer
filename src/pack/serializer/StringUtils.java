package pack.serializer;

public class StringUtils {

	private static final int STRING_FRAME_LENGTH = 1;
	public static final int NULL_INDEX = -1;
	public static final String EMPTY_STRING = "";

	public static String cutStringFrame(String str) {
		return str.substring(STRING_FRAME_LENGTH, str.length() - STRING_FRAME_LENGTH);
	}

}
