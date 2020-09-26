package media.xen.tradingcards;

import media.xen.tradingcards.models.WhitelistMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class WorldBlacklist {
    private SimpleConfig config;
    private String listedWorldsName = "World-Blacklist";
    private List<String> listedWorlds;

    public WorldBlacklist(SimpleConfig config){
        this.config = config;
        loadWorlds();
    }

    private void loadWorlds(){
        listedWorlds = this.config.getConfig().getStringList(listedWorldsName);
    }

    public boolean isAllowed(World w) {
        boolean isOnList = listedWorlds.contains(w.getName());

        //If you're not on the blacklist, you're allowed
        return !isOnList;
    }

    public void add(World w) {
        listedWorlds.add(w.getName());
        this.config.getConfig().set(listedWorldsName, null);
        this.config.getConfig().set(listedWorldsName, listedWorlds);
        this.config.saveConfig();
    }

    public void remove(World w) {
        listedWorlds.remove(w.getName());
        this.config.getConfig().set(listedWorldsName, null);
        this.config.getConfig().set(listedWorldsName, listedWorlds);
        this.config.saveConfig();
    }
}

