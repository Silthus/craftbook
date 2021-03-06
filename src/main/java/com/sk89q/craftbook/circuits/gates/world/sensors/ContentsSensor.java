package com.sk89q.craftbook.circuits.gates.world.sensors;

import org.bukkit.Server;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.circuits.ic.AbstractIC;
import com.sk89q.craftbook.circuits.ic.AbstractICFactory;
import com.sk89q.craftbook.circuits.ic.ChipState;
import com.sk89q.craftbook.circuits.ic.IC;
import com.sk89q.craftbook.circuits.ic.ICFactory;
import com.sk89q.craftbook.circuits.ic.ICVerificationException;
import com.sk89q.craftbook.util.ICUtil;

public class ContentsSensor extends AbstractIC {

    public ContentsSensor (Server server, ChangedSign sign, ICFactory factory) {
        super(server, sign, factory);
    }

    @Override
    public void load() {

        item = ICUtil.getItem(getLine(2));
    }

    ItemStack item;

    @Override
    public String getTitle () {
        return "Container Content Sensor";
    }

    @Override
    public String getSignTitle () {
        return "CONTENT SENSOR";
    }

    @Override
    public void trigger (ChipState chip) {

        if(chip.getInput(0))
            chip.setOutput(0, sense());
    }

    public boolean sense() {

        if (getBackBlock().getRelative(0, 1, 0).getState() instanceof InventoryHolder) {

            InventoryHolder inv = (InventoryHolder) getBackBlock().getRelative(0, 1, 0).getState();
            return inv.getInventory().containsAtLeast(item, 1);
        }

        return false;
    }

    public static class Factory extends AbstractICFactory {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new ContentsSensor(getServer(), sign, this);
        }

        @Override
        public void verify(ChangedSign sign) throws ICVerificationException {

            ItemStack item = ICUtil.getItem(sign.getLine(2));
            if(item == null)
                throw new ICVerificationException("Invalid item to detect!");
        }

        @Override
        public String getShortDescription() {

            return "Detects if the above container has a specific item inside it.";
        }

        @Override
        public String[] getLineHelp() {

            String[] lines = new String[] {"item id:data", null};
            return lines;
        }
    }
}
