package me.steppenwiesel.spongereloaded;

/**
 * Used by the {@link RecipeCreator} class to indicate that an error occured while creating the recipe.
 * @author Wüstengecko
 *
 */
public class RecipeException extends Exception {

	/**
	 * 
	 * @param cause A previously occured Exception that caused this to happen.
	 */
	public RecipeException(Throwable cause) {
		super(cause);
	}

	/**
	 * 
	 * @param reason The reason why this happens.
	 */
	public RecipeException(String reason) {
		super(reason);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -4821715776386096086L;

}
