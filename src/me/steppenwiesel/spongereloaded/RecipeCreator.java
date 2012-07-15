package me.steppenwiesel.spongereloaded;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.inventory.ShapedRecipe;

/**
 * Builds a {@link ShapedRecipe}. This class reads the configuration from the given configuration file.<br>
 * Set up the file as follows:
 * 
 * <pre>
 * row 1 of recipe
 * row 2 of recipe (optional)
 * row 3 of recipe (optional)
 * empty row
 * description of contents
 * </pre>
 * 
 * The different rows of the recipe are up to 3 characters long.<br>
 * You can use almost any character, but recommended are alphanumeric chars.<br>
 * <br>
 * You MUST leave exactly one row empty for the parser to know when the recipe ends.<br>
 * <br>
 * In the content description, follow this line formatting:
 * <pre>
 * c|MATERIAL
 * </pre>
 * ... where {@code c} stands for a character you used in the recipe above and {@code MATERIAL} must match a material that Bukkit knows at this time.<br>
 * If the lines don't match this format, you will most likely get a {@link RecipeException}.
 * @author Wüstengecko
 *
 */
public final class RecipeCreator {

	private File cfg;
	private Logger logger;
	private ShapedRecipe recipe;

	/**
	 * Create a new RecipeCreator and build up the recipe.
	 * @param config The config file to use. Will automatically create it and fill with the content of the defaultConfig variable if it does not exist yet.
	 * @param logger The Logger object where to print all output data. Retrieves the Logger named "Minecraft" if this is null.
	 * @param defaultConfig The default configuration to fall back if no other configuration is given.
	 * @throws RecipeException If any errors occur during building up the recipe.
	 */
	public RecipeCreator(File config, Logger logger, String[] defaultConfig) throws RecipeException {
		this.cfg = config;
		if (logger == null) this.logger = Logger.getLogger("Minecraft");
		else this.logger = logger;
		if (!cfg.exists()) {
			try {
				writeDefaultConfig();
			} catch (IOException e) {
				throw new RecipeException(e);
			}
		}

		this.recipe = createRecipe();
		if (this.recipe == null) throw new RecipeException("Could not create recipe - unknown error");
	}

	private ShapedRecipe createRecipe() {
		try {
			String[] config = readConfig();
			// TODO generate a ShapedRecipe from the configuration file
		} catch (IOException e) {
			logger.severe("Could not read recipe configuration");
			e.printStackTrace();
		}
		return null;
	}

	private String[] readConfig() throws IOException {
		// TODO read line into an array of strings
		return null;
	}

	private void writeDefaultConfig() throws IOException {
		if (cfg.exists()) cfg.delete();
		if (!cfg.createNewFile()) throw new IOException("Could not create configuration file: " + cfg.getPath());
		FileOutputStream fos = new FileOutputStream(cfg);
		// TODO write.
	}

	/**
	 * Used to retrieve the created recipe.<br>
	 * At this time, the recipe is completely built up and will no longer throw any errors.
	 * @return The {@link ShapedRecipe}.
	 */
	public ShapedRecipe getRecipe() {
		return recipe;
	}

}
