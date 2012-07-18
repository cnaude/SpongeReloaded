package me.steppenwiesel.spongereloaded;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

/**
 * <p>
 * Builds a {@link ShapedRecipe}. This class reads the configuration from the given configuration file.<br>
 * Set up the file as follows:
 * </p>
 * 
 * <pre>
 * row 1 of recipe
 * row 2 of recipe (optional)
 * row 3 of recipe (optional)
 * empty row
 * description of contents
 * </pre>
 * 
 * <p>
 * The different rows of the recipe are up to 3 characters long.<br>
 * You can use almost any character, but recommended are alphanumeric chars.<br>
 * <br>
 * You MUST leave exactly one row empty for the parser to know when the recipe ends.<br>
 * <br>
 * In the content description, follow this line formatting:
 * </p>
 * 
 * <pre>
 * c-MATERIAL
 * </pre>
 * <p>
 * ... where {@code c} stands for a character you used in the recipe above and {@code MATERIAL} must match a material that Bukkit knows at this time.<br>
 * You can also use block / item IDs instead of the names. Consult the internet to find the IDs out!<br>
 * If the lines don't match this format, you will most likely get a {@link RecipeException}.
 * </p>
 * 
 * <p>
 * An example configuration file could look this:
 * </p>
 * 
 * <pre>
 * aba
 * bab
 * aba
 * 	<= this row stays empty
 * a-STRING
 * b-SAND
 * </pre>
 * <p>
 * This is the most commonly used recipe for sponges.<br>
 * Human-understandable, it is simply string (a) and sand (b) in a checkerboard.
 * </p>
 * @author Steppenwiesel
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
		this.defaultConfig = defaultConfig;

		try {
			this.recipe = createRecipe();
		} catch (RecipeException e) {
			Logger log = Logger.getLogger("Minecraft");
			log.info("Failed to read the crafting configuration from " + cfg.getName());
			log.info("Falling back to default. Your configuration will be overwritten.");
			try {
				writeDefaultConfig();
			} catch (IOException e1) {
				throw new RecipeException(e1);
			}
			this.recipe = createRecipe();
		}
		if (this.recipe == null) throw new RecipeException("Could not create recipe - unknown error (recipe is null)");
	}

	private ShapedRecipe createRecipe() throws RecipeException {
		if (!cfg.exists()) throw new RecipeException("No configuration file found");
		try {
			ShapedRecipe recipe = new ShapedRecipe(new ItemStack(Material.SPONGE, 1));
			String[] config = readConfig();
			int i;

			try {
				if (config[0].equals(""))
					throw new RecipeException("Invalid configuration data - first line is empty");
				else if (config[1].equals("")) {
					i = 2;
					recipe.shape(config[0]);
				} else if (config[2].equals("")) {
					i = 3;
					recipe.shape(config[0], config[1]);
				} else if (config[3].equals("")) {
					i = 4;
					recipe.shape(config[0], config[1], config[2]);
				} else
					throw new RecipeException("Invalid configuration data - too many rows");
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new RecipeException("Invalid configuration data - could not read recipe's rows");
			}

			try {
				// smiley!!
				//   |
				//   v
				for (; i < config.length; i++) {
					String line = config[i];

					String[] parts = line.split("-");
					char ch = parts[0].charAt(0);

					Material material = null;
					try {
						int id = new Integer(parts[1]);
						material = Material.getMaterial(id);
					} catch (NumberFormatException e) {
						material = Material.matchMaterial(parts[1]);
					}

					if (material == null) continue;
					recipe.setIngredient(ch, material);
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new RecipeException("Bad line formatting in recipe content description -- " + cfg.getName());
			}

			return recipe;
		} catch (IOException e) {
			throw new RecipeException(e);
		}
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

		// close streams
		br.close();
		isr.close();
		dis.close();
		fis.close();

		// convert Object[] to String[] (casting doesn't work)
		Object[] arrObject = lines.toArray();
		String[] arrString = new String[arrObject.length];
		for (int i = 0; i < arrObject.length; i++)
			arrString[i] = arrObject[i].toString();
		return arrString;
	}

	private void writeDefaultConfig() throws IOException {
		if (cfg.exists()) cfg.delete();
		if (!cfg.createNewFile()) throw new IOException("Could not create configuration file: " + cfg.getPath());
		FileOutputStream fos = new FileOutputStream(cfg);
		OutputStreamWriter osw = new OutputStreamWriter(fos, "utf-8");
		BufferedWriter bw = new BufferedWriter(osw);
		for (String line : defaultConfig)
			bw.write(line + "\n");
		bw.close();
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
