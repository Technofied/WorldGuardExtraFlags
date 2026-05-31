package net.goldtreeservers.worldguardextraflags;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import net.goldtreeservers.worldguardextraflags.listeners.*;
import net.goldtreeservers.worldguardextraflags.wg.handlers.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;

import lombok.Getter;
import net.goldtreeservers.worldguardextraflags.flags.Flags;

public class WorldGuardExtraFlagsPlugin extends JavaPlugin
{
	private static final Set<Flag<?>> FLAGS = WorldGuardExtraFlagsPlugin.getPluginFlags();
	@Getter private static WorldGuardExtraFlagsPlugin plugin;

	@Getter private WorldEditPlugin worldEditPlugin;

	@Getter private WorldGuardPlugin worldGuardPlugin;
	@Getter private WorldGuard worldGuard;

	@Getter private RegionContainer regionContainer;
	@Getter private SessionManager sessionManager;
	
	public WorldGuardExtraFlagsPlugin()
	{
		WorldGuardExtraFlagsPlugin.plugin = this;
	}
	
	@Override
	public void onLoad()
	{
		this.worldEditPlugin = (WorldEditPlugin) this.getServer().getPluginManager().getPlugin("WorldEdit");
		this.worldGuardPlugin = (WorldGuardPlugin) this.getServer().getPluginManager().getPlugin("WorldGuard");

		this.worldGuard = WorldGuard.getInstance();

		try
		{
			FlagRegistry flagRegistry = this.worldGuard.getFlagRegistry();
			flagRegistry.register(Flags.TELEPORT_ON_ENTRY);
			flagRegistry.register(Flags.TELEPORT_ON_EXIT);
			flagRegistry.register(Flags.COMMAND_ON_ENTRY);
			flagRegistry.register(Flags.COMMAND_ON_EXIT);
			flagRegistry.register(Flags.CONSOLE_COMMAND_ON_ENTRY);
			flagRegistry.register(Flags.CONSOLE_COMMAND_ON_EXIT);
			flagRegistry.register(Flags.WALK_SPEED);
			flagRegistry.register(Flags.KEEP_INVENTORY);
			flagRegistry.register(Flags.KEEP_EXP);
			flagRegistry.register(Flags.CHAT_PREFIX);
			flagRegistry.register(Flags.CHAT_SUFFIX);
			flagRegistry.register(Flags.BLOCKED_EFFECTS);
			flagRegistry.register(Flags.GODMODE);
			flagRegistry.register(Flags.RESPAWN_LOCATION);
			flagRegistry.register(Flags.WORLDEDIT);
			flagRegistry.register(Flags.FLY);
			flagRegistry.register(Flags.FLY_SPEED);
			flagRegistry.register(Flags.PLAY_SOUNDS);
			flagRegistry.register(Flags.FROSTWALKER);
			flagRegistry.register(Flags.NETHER_PORTALS);
			flagRegistry.register(Flags.GLIDE);
			flagRegistry.register(Flags.ITEM_DURABILITY);
			flagRegistry.register(Flags.CHESTSHOP_TRANSACT);
			flagRegistry.register(Flags.JOIN_LOCATION);
		}
		catch (Exception e)
		{
			this.getServer().getPluginManager().disablePlugin(this);

			throw new RuntimeException(e instanceof IllegalStateException ?
					"WorldGuard prevented flag registration. Did you reload the plugin? This is not supported!" :
					"Flag registration failed!", e);
		}
	}
	
	@Override
	public void onEnable()
	{
		this.regionContainer = this.worldGuard.getPlatform().getRegionContainer();
		this.sessionManager = this.worldGuard.getPlatform().getSessionManager();

		this.sessionManager.registerHandler(TeleportOnEntryFlagHandler.FACTORY(plugin), null);
		this.sessionManager.registerHandler(TeleportOnExitFlagHandler.FACTORY(plugin), null);

		this.sessionManager.registerHandler(WalkSpeedFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(FlySpeedFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(FlyFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(GlideFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(GodmodeFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(PlaySoundsFlagHandler.FACTORY(plugin), null);
		this.sessionManager.registerHandler(BlockedEffectsFlagHandler.FACTORY(), null);

		this.sessionManager.registerHandler(CommandOnEntryFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(CommandOnExitFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(ConsoleCommandOnEntryFlagHandler.FACTORY(), null);
		this.sessionManager.registerHandler(ConsoleCommandOnExitFlagHandler.FACTORY(), null);

		this.getServer().getPluginManager().registerEvents(new PlayerListener(this, this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);
		this.getServer().getPluginManager().registerEvents(new BlockListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);
		this.getServer().getPluginManager().registerEvents(new EntityListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);

		this.worldEditPlugin.getWorldEdit().getEventBus().register(new WorldEditListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager));

		if (this.getServer().getPluginManager().getPlugin("ChestShop") != null)
		{
			this.getServer().getPluginManager().registerEvents(new ChestShopListener(this.worldGuardPlugin, this.regionContainer, this.sessionManager), this);
		}

		this.setupMetrics();
	}

	
	private void setupMetrics()
	{
		final int bStatsPluginId = 7301;
		
        Metrics metrics = new Metrics(this, bStatsPluginId);
        metrics.addCustomChart(new Metrics.AdvancedPie("flags_used", () ->
		{
			Map<Flag<?>, Boolean> valueMap = WorldGuardExtraFlagsPlugin.FLAGS.stream().collect(Collectors.toMap(v -> v, v -> false));

			WorldGuard.getInstance().getPlatform().getRegionContainer().getLoaded().forEach(m ->
			{
				m.getRegions().values().forEach(r -> r.getFlags().keySet().forEach(f -> valueMap.computeIfPresent(f, (k, v) -> true)));
			});

			return valueMap.entrySet().stream().collect(Collectors.toMap(v -> v.getKey().getName(), v -> v.getValue() ? 1 : 0));
		}));
	}
	
	private static Set<Flag<?>> getPluginFlags()
	{
		Set<Flag<?>> flags = new HashSet<>();
		
		for (Field field : Flags.class.getFields())
		{
			try
			{
				flags.add((Flag<?>)field.get(null));
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
			}
		}
		
		return flags;
	}
}
