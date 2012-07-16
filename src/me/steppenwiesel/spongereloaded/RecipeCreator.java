package me.steppenwiesel.spongereloaded;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
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
	private String[] defaultConfig;
	private ShapedRecipe recipe;

	/**
	 * Create a new RecipeCreator and build up the recipe.
	 * @param config The config file to use. Will automatically create it and fill with the content of the defaultConfig variable if it does not exist yet.
	 * @param logger The Logger object where to print all output data. Retrieves the Logger named "Minecraft" if this is null.
	 * @param defaultConfig The default configuration to fall back if no other configuration is given.
	 * @throws RecipeException If any errors occur during building up the recipe.
	 */
	public RecipeCreator(File config, String[] defaultConfig) throws RecipeException {
		this.cfg = config;

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

	private ShapedRecipe createRecipe() throws RecipeException {
		try {
			String[] config = readConfig();
			// TODO generate a ShapedRecipe from the configuration file
		} catch (IOException e) {
			throw new RecipeException(e);
		}
		return null;
	}

	private String[] readConfig() throws IOException {
		ArrayList<String> lines = new ArrayList<>();

		FileInputStream fis = new FileInputStream(cfg);
		DataInputStream dis = new DataInputStream(fis);
		InputStreamReader isr = new InputStreamReader(dis);
		BufferedReader br = new BufferedReader(isr);

		String line;
		while ((line = br.readLine()) != null) {
			lines.add(line);
		}

		return (String[]) lines.toArray();
	}

	private void writeDefaultConfig() throws IOException {
		if (cfg.exists()) cfg.delete();
		if (!cfg.createNewFile()) throw new IOException("Could not create configuration file: " + cfg.getPath());
		FileOutputStream fos = new FileOutputStream(cfg);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		for (String line : defaultConfig)
			oos.writeChars(line);
		oos.close();
		fos.close();
	}

	/**
	 * Used to retrieve the created recipe.<br>
	 * This method is safe to use and will not generate any Exception.<br>
	 * The returned recipe will never be null.
	 * @return The {@link ShapedRecipe}.
	 */
	public ShapedRecipe getRecipe() {
		return recipe;
	}

}
