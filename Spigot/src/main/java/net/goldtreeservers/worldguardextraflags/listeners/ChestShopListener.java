package net.goldtreeservers.worldguardextraflags.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.SessionManager;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;

import lombok.RequiredArgsConstructor;
import net.goldtreeservers.worldguardextraflags.flags.Flags;

@RequiredArgsConstructor
public class ChestShopListener implements Listener
{
	private final WorldGuardPlugin worldGuardPlugin;
	private final RegionContainer regionContainer;
	private final SessionManager sessionManager;

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onPreTransaction(PreTransactionEvent event)
	{
		Player player = event.getClient();
		Sign sign = event.getSign();

		if (player == null || sign == null)
		{
			return;
		}

		Location shopLocation = BukkitAdapter.adapt(sign.getLocation());
		LocalPlayer localPlayer = this.worldGuardPlugin.wrapPlayer(player);

		if (this.sessionManager.hasBypass(localPlayer, (World) shopLocation.getExtent()))
		{
			return;
		}

		if (this.regionContainer.createQuery().queryState(shopLocation, localPlayer, Flags.CHESTSHOP_TRANSACT) == State.DENY)
		{
			event.setCancelled(PreTransactionEvent.TransactionOutcome.CLIENT_DOES_NOT_HAVE_PERMISSION);
		}
	}
}
