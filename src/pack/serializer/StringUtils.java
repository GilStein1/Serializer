package pack.serializer;

public class StringUtils {

	private static final int STRING_FRAME_LENGTH = 1;
	public static final int NULL_INDEX = -1;
	public static final String EMPTY_STRING = "";

	public static String cutStringFrame(String str) {
		return str.substring(STRING_FRAME_LENGTH, str.length() - STRING_FRAME_LENGTH);
	}

	public static int indexOfTheN(String str, char character, int n) {
		for (int i = 0; i < str.length(); i++) {
			if(str.charAt(i) == character) {
				n--;
			}
			if(n == 0) {
				return i;
			}
		}
		return NULL_INDEX;
	}

	public static String makeStringFrame(String str, char frameChar) {
		StringBuilder frame = new StringBuilder(EMPTY_STRING);
		frame.append(String.valueOf(frameChar).repeat(STRING_FRAME_LENGTH));
		return frame + str + frame;
	}

}
