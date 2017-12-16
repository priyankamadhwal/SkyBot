/*
 * Skybot, a multipurpose discord bot
 *      Copyright (C) 2017  Duncan "duncte123" Sterken & Ramid "ramidzkh" Khan & Maurice R S "Sanduhr32"
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ml.duncte123.skybot.commands.guild.mod;

import ml.duncte123.skybot.objects.command.Command;
import ml.duncte123.skybot.objects.command.CommandCategory;
import ml.duncte123.skybot.utils.AirUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.event.Level;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CleanupCommand extends Command {

    public final static String help = "performs a cleanup in the channel where the command is run.";

    public CleanupCommand() {
        this.category = CommandCategory.MOD_ADMIN;
    }

    @Override
    public void executeCommand(String invoke, String[] args, GuildMessageReceivedEvent event) {

        if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY)) {
            sendMsg(event, "You don't have permission to run this command!");
            return;
        }

        int total = 5;
        boolean keepPinned = false;

        if (args.length > 0) {

            if (args.length == 1 && args[0].equalsIgnoreCase("keep-pinned"))
                keepPinned = true;
            else {
                if(args.length == 2 && args[1].equalsIgnoreCase("keep-pinned"))
                     keepPinned = true;
                try {
                    total = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sendError(event.getMessage());
                    sendMsg(event, "Error: Amount to clear is not a valid number");
                    return;
                }
                if (total < 2 || total > 100) {
                    event.getChannel().sendMessage("Error: count must be minimal 2 and maximal 100").queue(
                            message -> message.delete().queueAfter(5, TimeUnit.SECONDS)
                    );
                    return;
                }
            }
        }

        try {
            MessageHistory mh = event.getChannel().getHistory();
            List<Message> msgLst = mh.retrievePast(total).complete();

            if(keepPinned)
                msgLst = msgLst.stream().filter(message -> !message.isPinned()).collect(Collectors.toList());

            event.getChannel().deleteMessages(msgLst).queue();
            event.getChannel().sendMessage("Removed " + msgLst.size() + " messages!").queue(
                    message -> message.delete().queueAfter(5, TimeUnit.SECONDS)
            );
            AirUtils.log(Level.DEBUG, msgLst.size() + " messages removed in channel " + event.getChannel().getName() + " on guild " + event.getGuild().getName());
        } catch (Exception e) {
            event.getChannel().sendMessage("ERROR: " + e.getMessage()).queue();
        }
    }

    @Override
    public String help() {
        return "Performs a cleanup in the channel where the command is run.\n" +
                "Usage: `"+PREFIX+getName()+ "[ammount/keep-pinned] [keep-pinned]`";
    }

    @Override
    public String getName() {
        return "cleanup";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"clear", "purge"};
    }
}