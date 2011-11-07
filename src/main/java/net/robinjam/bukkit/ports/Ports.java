package net.robinjam.bukkit.ports;

import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.Transaction;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.PersistenceException;
import net.robinjam.bukkit.ports.commands.*;
import net.robinjam.bukkit.ports.persistence.Port;
import net.robinjam.bukkit.util.CommandManager;
import net.robinjam.bukkit.util.Configuration;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author robinjam
 */
public class Ports extends JavaPlugin {
    
    private Configuration config;
    private PluginDescriptionFile pdf;
    private CommandManager commandManager;
    private WorldEditPlugin worldEditPlugin;
    private PlayerListener playerListener = new PlayerListener(this);
    private VehicleListener vehicleListener = new VehicleListener(this);
    private TicketManager ticketManager = new TicketManager(this);
    
    private static final Logger logger = Logger.getLogger("Minecraft");

    public void onEnable() {
        // Read plugin description file
        pdf = this.getDescription();
        
        // Load the configuration file
        this.reload();
        
        // Hook into WorldEdit
        this.hookWorldEdit();
        
        // Set up database
        this.setupDatabase();
        
        // Register events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_TELEPORT, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.VEHICLE_ENTER, vehicleListener, Priority.Normal, this);
        
        // Register commands
        commandManager = new CommandManager();
        this.getCommand("port").setExecutor(commandManager);
        commandManager.registerCommand("reload", new ReloadCommand(this));
        commandManager.registerCommand("list", new ListCommand(this));
        commandManager.registerCommand("create", new CreateCommand(this));
        commandManager.registerCommand("delete", new DeleteCommand(this));
        commandManager.registerCommand("select", new SelectCommand(this));
        commandManager.registerCommand("arrive", new ArriveCommand(this));
        commandManager.registerCommand("update", new UpdateCommand(this));
        commandManager.registerCommand("schedule", new ScheduleCommand(this));
        commandManager.registerCommand("describe", new DescribeCommand(this));
        commandManager.registerCommand("destination", new DestinationCommand(this));
        commandManager.registerCommand("link", new LinkCommand(this));
        commandManager.registerCommand("unlink", new UnlinkCommand(this));
        
        // Schedule ticket manager
        getServer().getScheduler().scheduleSyncRepeatingTask(this, ticketManager, 0L, 100L);
        
        logger.info(String.format("%s version %s is enabled!", pdf.getName(), pdf.getVersion()));
    }
    
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    public void reload() {
        File dataFolder = this.getDataFolder();
        
        if(!dataFolder.exists())
            dataFolder.mkdir();
        
        try {
            config = new Configuration(new File(dataFolder, "config.yml"));
        } catch (IOException ex) {
            logger.severe(String.format("[%s] Unable to create default configuration file!", pdf.getName()));
            logger.severe(String.format("[%s] %s", pdf.getName(), ex.getMessage()));
        }
    }
    
    private void hookWorldEdit() {
        Plugin plugin = this.getServer().getPluginManager().getPlugin("WorldEdit");
        this.worldEditPlugin = (WorldEditPlugin) plugin;
    }
    
    public WorldEditPlugin getWorldEditPlugin() {
        return this.worldEditPlugin;
    }
    
    public WorldEdit getWorldEdit() {
        return this.worldEditPlugin.getWorldEdit();
    }
    
    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> list = new ArrayList<Class<?>>();
        list.add(Port.class);
        return list;
    }

    private void setupDatabase() {
        if (!databaseExists()) {
            logger.info(String.format("[%s] Creating database", pdf.getName()));
            installDDL();
        } else if (databaseIsOutdated()) {
            logger.info(String.format("[%s] Upgrading database", pdf.getName()));
            try {
                upgradeDatabase();
            } catch (SQLException ex) {
                logger.severe(String.format("[%s] Unable to upgrade database!", pdf.getName()));
                logger.severe(ex.getMessage());
            }
        }
        
        Port.setPlugin(this);
    }
    
    private boolean databaseExists() {
        try {
            getDatabase().find(Port.class).findRowCount();
            return true;
        } catch (PersistenceException ex) {
            return false;
        }
    }
    
    private boolean databaseIsOutdated() {
        try {
            getDatabase().find(Port.class).findList();
            return false;
        } catch (PersistenceException ex) {
            return true;
        }
    }
    
    private void upgradeDatabase() throws SQLException {
        Transaction transaction = getDatabase().createTransaction();
        Connection connection = transaction.getConnection();
        
        try {
            Statement statement = connection.createStatement();
            statement.execute("ALTER TABLE port ADD version INT DEFAULT 0 NOT NULL");
            connection.commit();
        } finally {
            connection.close();
        }
    }
    
    public void teleportPlayer(Player player, Port port) {
        player.teleport(port.getDestination().getArrivalLocation());
        World world = player.getWorld();
        Chunk chunk = world.getChunkAt(player.getLocation());
        world.refreshChunk(chunk.getX(), chunk.getZ());
        player.sendMessage(ChatColor.AQUA + "Whoosh!");
    }
    
    public TicketManager getTicketManager() {
        return ticketManager;
    }
    
}
