package commoble.workshopsofdoom.util;

/**
 * Utility interface for lambdas that can accept a single argument + a vararg
 */
@FunctionalInterface
public interface MultiConsumer<FIRST, SECONDS>
{
	@SuppressWarnings("unchecked")
	void accept(FIRST first, SECONDS...seconds);
}
