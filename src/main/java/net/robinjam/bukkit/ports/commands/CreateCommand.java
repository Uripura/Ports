package net.robinjam.bukkit.ports.commands;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.regions.CuboidRegion;
import java.util.List;
import net.robinjam.bukkit.ports.persistence.Port;
import net.robinjam.bukkit.util.Command;
import net.robinjam.bukkit.util.CommandExecutor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /port create command.
 * 
 * @author robinjam
 */
@Command(name = "create", usage = "[name]", permissions = "ports.create", playerOnly = true, min = 1, max = 1)
public class CreateCommand implements CommandExecutor {

	public void onCommand(CommandSender sender, List<String> args) {
		String name = args.get(0);

		if (Port.get(name) != null) {
			sender.sendMessage(ChatColor.RED
					+ "There is already a port with that name. Please pick a unique name.");
			return;
		}

		Player player = (Player) sender;
		WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		Selection selection = worldEdit.getSelection(player);
				
		if (selection == null) {
			sender.sendMessage(ChatColor.RED
					+ "Please select the activation area using WorldEdit first.");
			return;
		}
		
		if (!(selection instanceof CuboidRegion)) {
			sender.sendMessage(ChatColor.RED
					+ "Only cuboid regions are supported.");
			return;
		}

		Port port = new Port();
		port.setName(name);
		port.setActivationRegion(((CuboidRegion) selection).clone());
		port.setArrivalLocation(player.getLocation());
		Port.save();

		sender.sendMessage(ChatColor.AQUA
				+ "The port was created successfully.");
	}

}
