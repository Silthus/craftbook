package com.sk89q.craftbook.bukkit.commands;
import com.sk89q.craftbook.bukkit.CraftBookPlugin;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.NestedCommand;
import org.bukkit.command.CommandSender;

/**
 * Author: Turtle9598
 */
public class TopLevelCommands {

    public TopLevelCommands() {

    }

    @Command(aliases = {"craftbook", "cb"}, desc = "CraftBook Plugin commands")
    @NestedCommand(Commands.class)
    public void reload(CommandContext context, CommandSender sender) {

        sender.sendMessage("The CraftBook Common config has been reloaded.");
    }

    public class Commands {

        @Command(aliases = "reload", desc = "Reloads the CraftBook Common config")
        public void reload(CommandContext context, CommandSender sender) {

            CraftBookPlugin.getInstance().reloadAllConfiguration();
            sender.sendMessage("The CraftBook config has been reloaded.");
        }
    }
}