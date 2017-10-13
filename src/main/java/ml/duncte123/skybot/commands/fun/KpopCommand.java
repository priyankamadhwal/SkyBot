package ml.duncte123.skybot.commands.fun;

import ml.duncte123.skybot.objects.command.Command;
import ml.duncte123.skybot.utils.AirUtils;
import ml.duncte123.skybot.utils.EmbedUtils;
import ml.duncte123.skybot.utils.Settings;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;

public class KpopCommand extends Command {

    /**
     * This is the executeCommand of the command, the thing you want the command to to needs to be in here
     * @param args The command agruments
     * @param event a instance of {@link net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent GuildMessageReceivedEvent}
     */
    @Override
    public void executeCommand(String[] args, GuildMessageReceivedEvent event) {
        String id = "";
        String name = "";
        String group = "";
        String imgUrl = "";

        String dbName = AirUtils.db.getName();
        Connection database = AirUtils.db.getConnManager().getConnection();
        try {

            if(args.length > 0) {

                PreparedStatement statement = database.prepareStatement("SELECT * FROM " + dbName + ".kpop WHERE name LIKE ? OR id= ? LIMIT 1");
                statement.setString(1, "%"+StringUtils.join(args, " ")+"%");
                statement.setString(2, StringUtils.join(args, " "));

                ResultSet res = statement.executeQuery();

                while (res.next()) {
                    id = res.getString("id");
                    name = res.getString("name");
                    group = res.getString("band");
                    imgUrl = res.getString("img");
                }

            } else {

                Statement statement = database.createStatement();

                ResultSet res = statement.executeQuery("SELECT * FROM " + dbName + ".kpop ORDER BY RAND() LIMIT 1");

                while (res.next()) {
                    id = res.getString("id");
                    name = res.getString("name");
                    group = res.getString("band");
                    imgUrl = res.getString("img");
                }
            }

            EmbedBuilder eb = EmbedUtils.defaultEmbed()
                    .setDescription("Here is a kpop member from the group " + group)
                    .addField("Name of the member", name, false)
                    .setImage(imgUrl)
                    .setFooter("Query id: " + id, Settings.defaultIcon);
            sendEmbed(event, eb.build());
        }
        catch (Exception e) {
           sendMsg(event, "SCREAM THIS TO _duncte123#1245_: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                database.close();
            }
            catch (SQLException e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * The usage instructions of the command
     * @return a String
     */
    @Override
    public String help() {
        return "Gives you a random kpop member, command idea by Exa\nUsage: " + Settings.prefix + getName() +" [search term]";
    }

    @Override
    public String getName() {
        return "kpop";
    }
}
