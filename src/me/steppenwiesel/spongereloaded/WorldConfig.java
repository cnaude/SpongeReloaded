package me.steppenwiesel.spongereloaded;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

public class WorldConfig {

	public static final int ID_WATER = Material.WATER.getId();
	public static final int ID_STATIONARY_WATER = Material.STATIONARY_WATER.getId();
	public static final int ID_LAVA = Material.LAVA.getId();
	public static final int ID_STATIONARY_LAVA = Material.STATIONARY_LAVA.getId();
	public static final int ID_FIRE = Material.FIRE.getId();
	public static final int ID_SPONGE = Material.SPONGE.getId();

	private final World world;
	private boolean enabled = true;
	private int range = 2;
	private boolean soakWater = true;
	private boolean soakLava = false;
	private boolean soakFire = false;

	private final SpongeReloadedPlugin plugin;

	public WorldConfig(final World world, final SpongeReloadedPlugin plugin) {
		this.plugin = plugin;
		this.world = world;
		final String wn = world.getName();
		final FileConfiguration conf = this.plugin.getConfig();
		// read settings
		enabled   = conf.getBoolean("worlds." + wn + ".enabled"   , conf.getBoolean("enabled"   ));
		soakWater = conf.getBoolean("worlds." + wn + ".soak_water", conf.getBoolean("soak_water"));
		soakLava  = conf.getBoolean("worlds." + wn + ".soak_lava" , conf.getBoolean("soak_lava" ));
		soakFire  = conf.getBoolean("worlds." + wn + ".soak_fire" , conf.getBoolean("soak_fire" ));

		range = conf.getInt("worlds." + wn + ".radius", 0);
		if (range <= 0) range = conf.getInt("radius");
		if (range <= 0) range = 2;

		if (!(soakWater || soakLava || soakFire)) enabled = false;
		soakWater = enabled && soakWater;
		soakLava = enabled && soakLava;
		soakFire = enabled && soakFire;
	}

	/**
	 * check whether the specified block is suckable in this world.
	 * @param block
	 * @return
	 */
	public boolean isSuckable(final Block block) {
		if (!enabled) return false;
		final int id = block.getTypeId();
		if (soakWater && (id == ID_WATER || id == ID_STATIONARY_WATER)) return true;
		if (soakLava && (id == ID_LAVA || id == ID_STATIONARY_LAVA)) return true;
		if (soakFire && id == ID_FIRE) return true;
		return false;
	}

	/**
	 * check whether sponges are enabled in this world.
	 * @return
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * check whether sponges can soak up water in this world.
	 * @return true if so, false otherwise.
	 */
	public boolean isWaterSuckable() {
		return enabled && soakWater;
	}

	/**
	 * check whether sponges can soak up lava in this world.
	 * @return true if so, false otherwise.
	 */
	public boolean isLavaSuckable() {
		return enabled && soakLava;
	}

	/**
	 * check whether sponges can soak up fire in this world.
	 * @return true if so, false otherwise.
	 */
	public boolean isFireSuckable() {
		return enabled && soakFire;
	}

	/**
	 * get the world this configuration object is for.
	 * @return the world.
	 */
	public World getWorld() {
		return world;
	}

	/**
	 * remove suckable blocks within the range of this block.
	 * @param block
	 */
	public void removeSuckables(final Block block) {
		for (int x = -range; x <= range; x++)
			for (int y = -range; y <= range; y++)
				for (int z = -range; z <= range; z++) {
					final Block checkBlock = block.getRelative(x,y,z);
					if (isSuckable(checkBlock)) {
						checkBlock.setTypeIdAndData(0, (byte) 0, false);
					}
				}
	}

	/**
	 * check whether a sponge is in the range configured for this world.
	 * @param block
	 * @return
	 */
	public boolean spongeInRange(final Block block) {
		if (isSuckable(block))
			for (int x = -range; x <= range; x++)
				for (int y = -range; y <= range; y++)
					for (int z = -range; z <= range; z++)
						if (block.getRelative(x,y,z).getTypeId() == ID_SPONGE)
							return true;
		return false;
	}

	/**
	 * call the physics around the center block.
	 * @param center
	 */
	public void callPhysics(final Block center) {
		// range+1 to trigger blocks around
		for (int x = -(range+1); x <= (range+1); x++)
			for (int y = -(range+1); y <= (range+1); y++)
				for (int z = -(range+1); z <= (range+1); z++) {
					final Block block = center.getRelative(x,y,z);
					if (isSuckable(block)) {
						final int id = block.getTypeId();
						final byte data = block.getData();
						block.setTypeIdAndData(0, (byte) 0, false);
						block.setTypeIdAndData(id, data, true);
					}
				}
	}

	public boolean bucketContainsSuckable(final Material bucket) {
		final String str = bucket.toString();
		return (str.contains("WATER") && isWaterSuckable()) || (str.contains("LAVA") && isLavaSuckable());
	}

}
