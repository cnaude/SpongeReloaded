package me.steppenwiesel.spongereloaded;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class SpongeReloadedPlugin extends JavaPlugin implements Listener {

	/**
	 * the WorldConfig objects, mapped by the world's names
	 * @see {@link World}, {@link World#getName()}
	 */
	private Map<String, WorldConfig> wconf;

	@Override
	public void onEnable() {
		super.onEnable();
		Bukkit.getPluginManager().registerEvents(this, this);

		// create / check config file
		if (!(new File(this.getDataFolder(), "config.yml").exists())) {
			saveDefaultConfig();
			log("Generated default configuration file.");
		} else
			updateConfig();

		// crafting
		if (getConfig().getBoolean("craftable")) {
			try {
				final String[] defaultRecipe = new String[] { "aba", "bab", "aba", "", "a-SAND", "b-STRING" };
				final File config = new File(this.getDataFolder(), "recipe.cfg");
				final ShapedRecipe recipe = new RecipeCreator(config, defaultRecipe).getRecipe();
				Bukkit.addRecipe(recipe);
			} catch (final RecipeException e) {
				log("Failed to read crafting configuration.");
				e.printStackTrace();
			}
		}

		// world configurations
		wconf = new HashMap<>();
		// now loading on-the-fly!

		log("Enabled " + this.getDescription().getName() + " v" + this.getDescription().getVersion());
	}

	@Override
	public void onDisable() {
		super.onDisable();
		// remove world configuration objects from RAM
		wconf = null;
		log("Disabled " + this.getDescription().getName() + " v" + this.getDescription().getVersion());
	}

	private void updateConfig() {
		FileConfiguration config = getConfig();
		final String configVersion = config.getString("config_version", "null");
		if (configVersion.equals("1.3") || configVersion.equals("1.4"));
		else {
			// update everything but range
			final int radius = config.getInt("range", config.getInt("radius", 2));
			new File(this.getDataFolder(), "config.yml").delete();
			saveDefaultConfig();
			reloadConfig();
			// 'config' contains an invalid pointer due to the reload
			config = getConfig();
			config.set("radius", radius);
			saveConfig();
			log("Updated config.yml to version " + config.getString("config_version"));
		}
	}

	private WorldConfig getWorldConfig(final World world) {
		final String worldName = world.getName();
		WorldConfig wconf = this.wconf.get(worldName);
		if (wconf == null) {
			// need a new one
			wconf = new WorldConfig(world, this);
			this.wconf.put(worldName, wconf);
			log("Loaded configuration for world '" + worldName + "'");
		}
		return wconf;
	}

	/**
	 * generate an 'info' message in the console.
	 * @param text to show
	 */
	private void log(final String text) {
		this.getLogger().info(text);
	}

	/*
	 * BEGIN EVENTHANDLER
	 */

	/**
	 * remove the world configuration if a world is getting unloaded.
	 * @param event
	 */
	public void onWorldUnload(WorldUnloadEvent event) {
		String worldName = event.getWorld().getName();
		if (this.wconf.remove(worldName) != null)
			log("Removed configuration for world " + worldName + " from memory");
	}

	/**
	 * react to the placement of sponges and water/lava blocks.
	 * @param event
	 */
	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent event) {
		final Block block = event.getBlock();
		final WorldConfig wconf = getWorldConfig(block.getWorld());
		if (block.getTypeId() == WorldConfig.ID_SPONGE) {
			wconf.removeSuckables(block);
			wconf.callPhysics(block);
		} else if (wconf.isSuckable(block))
			event.setCancelled(wconf.spongeInRange(block));
	}

	/**
	 * react to players emptying a bucket.
	 * @param event
	 */
	@EventHandler
	public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event) {
		final Material bucket = event.getBucket();
		final WorldConfig wconf = getWorldConfig(event.getBlockClicked().getWorld());
		if (wconf.bucketContainsSuckable(bucket)) {
			final Block block = event.getBlockClicked().getRelative(event.getBlockFace());
			event.setCancelled(wconf.spongeInRange(block));
		}
	}

	/**
	 * react to a player using a lighter
	 * @param event
	 */
	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent event) {
		final ItemStack item = event.getItem();
		if (item == null || item.getTypeId() != Material.FLINT_AND_STEEL.getId()) return;
		final Block newBlock = event.getClickedBlock().getRelative(event.getBlockFace());
		final WorldConfig wconf = getWorldConfig(newBlock.getWorld());
		if (wconf.isFireSuckable())
			event.setCancelled(wconf.spongeInRange(newBlock));
	}

	/**
	 * react to spreading fire
	 * @param event
	 */
	@EventHandler
	public void onBlockSpread(final BlockSpreadEvent event) {
		final Block source = event.getSource();
		final WorldConfig wconf = getWorldConfig(source.getWorld());
		if (wconf.isSuckable(source))
			event.setCancelled(wconf.spongeInRange(event.getBlock()));
	}

	/**
	 * react to flowing water/lava.
	 * @param event
	 */
	@EventHandler
	public void onBlockFromTo(final BlockFromToEvent event) {
		final Block from = event.getBlock();
		final Block to = event.getToBlock();
		final WorldConfig wconf = getWorldConfig(to.getWorld());
		if (wconf.isSuckable(from)) {
			event.setCancelled(wconf.spongeInRange(to));
		}
	}

	/**
	 * react to a sponge that was broken by a player.
	 * @param event
	 */
	@EventHandler
	public void onBlockBreak(final BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (block.getTypeId() != WorldConfig.ID_SPONGE) return;
		final WorldConfig wconf = getWorldConfig(block.getWorld());
		wconf.callPhysics(block);
	}

	/**
	 * react to a sponge that burnt down.
	 * @param event
	 */
	@EventHandler
	public void onBlockBurn(final BlockBurnEvent event) {
		final Block block = event.getBlock();
		if (block.getTypeId() != WorldConfig.ID_SPONGE) return;
		final WorldConfig wconf = getWorldConfig(block.getWorld());
		wconf.callPhysics(block);
	}

	/**
	 * routine block physics check that was initiated by Bukkit.
	 * @param event
	 */
	@EventHandler
	public void onBlockPhysics(final BlockPhysicsEvent event) {
		final Block block = event.getBlock();
		final WorldConfig wconf = getWorldConfig(block.getWorld());
		if (block.getTypeId() == WorldConfig.ID_SPONGE)
			wconf.removeSuckables(block);
		else if (wconf.isSuckable(block) && wconf.spongeInRange(block))
			block.setTypeIdAndData(0, (byte) 0, false);
	}

	/**
	 * react to a sponge that was moved by an extending piston.
	 * @param event
	 */
	@EventHandler
	public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
		// check if sponge(s) is/are affected, call physics if so
		final WorldConfig wconf = getWorldConfig(event.getBlock().getWorld());
		for (final Block i : event.getBlocks()) {
			if (i.getTypeId() == WorldConfig.ID_SPONGE) {
				wconf.callPhysics(i);
				// note: Bukkit automatically calls the physics for each affected block.
				// No need to manually remove all suckable blocks in this method.
			}
		}
	}

	/**
	 * react to a sponge that was moved by a retracting piston.
	 * @param event
	 */
	@EventHandler
	public void onBlockPistonRetract(final BlockPistonRetractEvent event) {
		final Block oldBlock = event.getBlock().getRelative(event.getDirection(), 2);
		final Block newBlock = event.getBlock().getRelative(event.getDirection(), 1);
		if (!event.isSticky() || oldBlock.getTypeId() != WorldConfig.ID_SPONGE) return;

		// update sponge
		final WorldConfig wconf = getWorldConfig(event.getBlock().getWorld());
		wconf.removeSuckables(newBlock);
		wconf.callPhysics(newBlock);
		wconf.callPhysics(oldBlock);
	}

	/**
	 * react to endermen stealing a sponge.
	 * @param event
	 */
	@EventHandler
	public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
		final Block block = event.getBlock();
		if (block.getTypeId() == WorldConfig.ID_SPONGE)
			getWorldConfig(block.getWorld()).callPhysics(block);
	}

	/*
	 * END EVENTHANDLER
	 */

}
