package de.luzifer.core.timer;

import de.luzifer.core.Core;
import de.luzifer.core.model.check.Check;
import de.luzifer.core.model.check.CheckManager;
import de.luzifer.core.model.exceptions.IllegalClickModificationException;
import de.luzifer.core.model.profile.storage.DataContainer;
import de.luzifer.core.model.repositories.UserRepository;
import de.luzifer.core.model.user.User;
import de.luzifer.core.utils.ActionBarUtil;
import de.luzifer.core.utils.Variables;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

@AllArgsConstructor
public class CheckTimer implements Runnable {

    private final HashMap<UUID, DataContainer> containerHashMap = new HashMap<>();

    private final CheckManager checkManager;
    private final UserRepository userRepository;

    public void run() {

        for(Player players : Bukkit.getOnlinePlayers()) {

            User user = userRepository.read(players.getUniqueId());

            if(user.isFrozen())
                Variables.PUNISHED.forEach(var -> players.sendMessage(Core.prefix + var.replace("&", "§")));

            user.getClicksAverageList().add(user.getClicks());
            user.getClicksAverageCheckList().add(user.getAverage());

            sendActionBar(user);

            dataContainer(user);

            int current_clicks = user.getClicks();
            for(Check check : checkManager.getChecks()) {
                
                if(check.isLoaded())
                    if(check.check(user))
                        check.onSuccess(user);
                    else
                        check.onFailure(user);
                
                if(current_clicks != user.getClicks())
                    throw new IllegalClickModificationException(check);
            }

            cleanUp(user);
        }
    }

    private void sendActionBar(User user) {
        
        if(user.getChecked() != null) {

            String message1 = "§4§l" + user.getChecked().getName();
            String message2;
    
            message2 = " §e§l-> §cClicks : §7§l" + user.getChecked().getClicks()+ " §6Average : §6§l" + user.getChecked().getAverage();
            message2 = message2 + " §6VL: §e" + user.getChecked().getViolations();

            if(Core.lowTPS)
                message2 = " §e§l-> §c§lCannot be checked -> §4§lLowTPS";

            if(Variables.pingChecker)
                if(user.getChecked().getPing() >= Variables.highestAllowedPing)
                    message2 = " §e§l-> §c§lCannot be checked -> §4§lPing §8(§4" + user.getChecked().getPing() + "§8)";

            if(Variables.bypass)
                if(user.getChecked().isBypassed())
                    message2 = " §e§l-> §c§lCannot be checked -> §4§lBypassed";

            ActionBarUtil.sendActionBar(user.getPlayer(), message1 + message2);
        }
    }

    private void cleanUp(User user) {

        if(Variables.sanctionateAtViolations > 0) {
            if(user.getViolations() >= Variables.sanctionateAtViolations) {
                user.sanction(null);
                user.clearViolations();
            }
        }

        if(user.clearViolations != 60*Variables.clearVLMinutes) {
            user.clearViolations++;
        } else {
            user.clearViolations();
            user.clearViolations = 0;
        }

        if(user.isRestricted()) {
            user.setRestricted(false);
        }

        user.setClicks(0);
    }

    private void dataContainer(User user) {
        if(!containerHashMap.containsKey(user.getPlayer().getUniqueId())) {
            containerHashMap.put(user.getPlayer().getUniqueId(), new DataContainer(user, Variables.storeAsManyData));
        }

        if(!containerHashMap.get(user.getPlayer().getUniqueId()).isFinish()) {
            DataContainer dataContainer = containerHashMap.get(user.getPlayer().getUniqueId());
            if(Variables.doNotStoreNothing) {
                if(user.getClicks() != 0) {
                    dataContainer.collectData();
                }
            } else {
                dataContainer.collectData();
            }
            containerHashMap.put(user.getPlayer().getUniqueId(), dataContainer);
        } else {
            containerHashMap.remove(user.getPlayer().getUniqueId());
        }

        if(!user.getProfile().getDataContainers().isEmpty()) {
            user.getProfile().checkForContainer();
        }
    }
}
