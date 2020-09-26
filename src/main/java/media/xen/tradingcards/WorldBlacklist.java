package media.xen.tradingcards;

import media.xen.tradingcards.models.WhitelistMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class WorldBlacklist {
    private SimpleConfig config;
    private String listedWorldsName = "Worlds";
    private String whitelistModeName = "Whitelist-Mode";
    private List<String> listedWorlds;
    private WhitelistMode whitelistMode;

    public WorldBlacklist(SimpleConfig config){
        this.config = config;
        loadWorlds();
    }

    private void loadWorlds(){
        listedWorlds = this.config.getConfig().getStringList(listedWorldsName);
    }

    private void setWhitelistMode()
    {
        boolean isWhitelist = this.config.getConfig().getBoolean(whitelistModeName);
        if(isWhitelist)
            this.whitelistMode = WhitelistMode.Whitelist;
        else
            this.whitelistMode = WhitelistMode.Blacklist;
    }

    public boolean isAllowed(World w) {
        boolean isOnList = listedWorlds.contains(w.getName());

        //If you're not on the blacklist, you're allowed
        if(this.whitelistMode == WhitelistMode.Blacklist)
        {
            return !isOnList;
        }

        //If you're on the whitelist, you're allowed
        if(this.whitelistMode == WhitelistMode.Whitelist)
        {
            return isOnList;
        }

        return false;
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

