package pack;

public class NoEmptyConstructor extends RuntimeException {
	public NoEmptyConstructor(Class<?> classWithNoEmptyConstructor) {
		super("class " + classWithNoEmptyConstructor.getName() + " has no empty constructor");
	}
}
