/*
 * Skybot, a multipurpose discord bot
 *      Copyright (C) 2017  Duncan "duncte123" Sterken & Ramid "ramidzkh" Khan & Sanduhr32
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
 *
 */

package ml.duncte123.skybot;

import ml.duncte123.skybot.commands.essentials.eval.EvalCommand;
import ml.duncte123.skybot.objects.guild.GuildSettings;
import ml.duncte123.skybot.utils.*;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.event.Level;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class BotListener extends ListenerAdapter {

    /**
     * This filter helps us to fiter out swearing
     */
    private BadWordFilter filter = new BadWordFilter();
    /**
     * When a command gets ran, it'll be stored in here
     */
    private static Map<Guild, TextChannel> lastGuildChannel = new HashMap<>();
    /**
     * This timer is for checking unbans
     */
    private ScheduledExecutorService unbanService = Executors.newScheduledThreadPool(1, r -> new Thread(r, "Unban-Thread"));
    /**
     * This tells us if the {@link #unbanService} is running
     */
    public boolean unbanTimerRunning = false;
    
    /**
     * This timer is for checking new quotes
     */
    private ScheduledExecutorService settingsUpdateService = Executors.newScheduledThreadPool(1, r -> new Thread(r, "Settings-Thread"));
    
    /**
     * This tells us if the {@link #settingsUpdateService} is running
     */
    public boolean settingsUpdateTimerRunning = false;

    /**
     * Listen for messages send to the bot
     *
     * @param event The corresponding {@link GuildMessageReceivedEvent}
     */
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        //We only want to respond to members/users
        if (event.getAuthor().isFake() || event.getAuthor().isBot() || event.getMember() == null) {
            return;
        }
        
        GuildSettings settings = GuildSettingsUtils.getGuild(event.getGuild());
        
        if (event.getMessage().getContent().equals(Settings.prefix + "shutdown") && Arrays.asList(Settings.wbkxwkZPaG4ni5lm8laY).contains(event.getAuthor().getId())) {
            AirUtils.log(Level.INFO, "Initialising shutdown!!!");
            ShardManager manager = event.getJDA().asBot().getShardManager();
            for (JDA shard : manager.getShards()) {
                AirUtils.log(Level.INFO, String.format("Shard %s has been shut down", shard.getShardInfo().getShardId()));
                shard.shutdown();
            }
            //Kill other things
            ((EvalCommand) AirUtils.commandManager.getCommand("eval")).shutdown();
            if (unbanTimerRunning) {
                this.unbanService.shutdown();
            }
            if (settingsUpdateTimerRunning) {
                this.settingsUpdateService.shutdown();
            }
            
            try {
                AirUtils.db.getConnManager().getConnection().close();
            } catch (SQLException e) {
                /* ignored */
            }
            
            System.exit(0);
            return;
        }

        if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) && settings.isEnableSwearFilter()) {
            if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                Message messageToCheck = event.getMessage();
                if (filter.filterText(messageToCheck.getRawContent())) {
                    messageToCheck.delete().reason("Blocked for bad swearing: " + messageToCheck.getContent()).queue();
                    event.getChannel().sendMessage(
                            String.format("Hello there, %s please do not use cursive language within this Discord.", event.getAuthor().getAsMention())).queue(
                            m -> m.delete().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }
            }
        }
        String rw = event.getMessage().getRawContent();
        
        if (event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfUser()) && event.getChannel().canTalk()
                && rw.equals(event.getJDA().getSelfUser().getAsMention())) {

            event.getChannel().sendMessage(
                    String.format("Hey <@%s>, try `%shelp` for a list of commands. If it doesn't work scream at _duncte123#1245_",
                            event.getAuthor().getId(),
                            Settings.prefix)
            ).queue();
            return;

        } else if (!rw.startsWith(Settings.prefix) &&
                !rw.startsWith(settings.getCustomPrefix())
                && !rw.startsWith(event.getJDA().getSelfUser().getAsMention())) {
            return;
        }

            //If the topic contains -commands ignore it
        if (event.getChannel().getTopic() != null && event.getChannel().getTopic().contains("-commands")) {
            return;
        }
        
        // run the a command
        lastGuildChannel.put(event.getGuild(), event.getChannel());
        if (!Settings.prefix.equals(settings.getCustomPrefix())) {
            rw = rw.replaceFirst(
                    Pattern.quote(settings.getCustomPrefix()),
                    Settings.prefix);
        }
        AirUtils.commandManager.runCommand(rw, event);
    }
    
    /**
     * When the bot is ready to go
     *
     * @param event The corresponding {@link ReadyEvent}
     */
    @Override
    public void onReady(ReadyEvent event){
        AirUtils.log(Level.INFO, "Logged in as " + String.format("%#s (Shard #%s)", event.getJDA().getSelfUser(), event.getJDA().getShardInfo().getShardId()));
        
        //Start the timers if they have not been started yet
        if (!unbanTimerRunning && AirUtils.nonsqlite) {
            AirUtils.log(Level.INFO, "Starting the unban timer.");
            //Register the timer for the auto unbans
            unbanService.scheduleAtFixedRate(() -> AirUtils.checkUnbans(event.getJDA().asBot().getShardManager()),10, 10, TimeUnit.MINUTES);
            unbanTimerRunning = true;
        }
        if (!settingsUpdateTimerRunning && AirUtils.nonsqlite) {
            AirUtils.log(Level.INFO, "Starting the settings timer.");
            //This handles the updating from the setting and quotes
            settingsUpdateService.scheduleWithFixedDelay(GuildSettingsUtils::loadAllSettings, 1, 1, TimeUnit.HOURS);
            settingsUpdateTimerRunning = true;
        }
        //Update guild count from then the bot was offline (should never die tho)
        AirUtils.updateGuildCount(event.getJDA(), event.getJDA().asBot().getShardManager().getGuildCache().size());
    }
    
    /**
     * This will fire when a new member joins
     *
     * @param event The corresponding {@link GuildMemberJoinEvent}
     */
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        /*
        {{USER_MENTION}} = mention user
        {{USER_NAME}} = return username
        {{GUILD_NAME}} = the name of the guild
        {{GUILD_USER_COUNT}} = member count
        {{GUILD_OWNER_MENTION}} = mention the guild owner
        {{GUILD_OWNER_NAME}} = return the name form the owner
         */
        
        GuildSettings settings = GuildSettingsUtils.getGuild(event.getGuild());
        
        if (settings.isEnableJoinMessage()) {
            TextChannel publicChannel = AirUtils.getPublicChannel(event.getGuild());
            String msg = settings.getCustomJoinMessage()
                                 .replaceAll("\\{\\{USER_MENTION}}", event.getUser().getAsMention())
                                 .replaceAll("\\{\\{USER_NAME}}", event.getUser().getName())
                                 .replaceAll("\\{\\{GUILD_NAME}}", event.getGuild().getName())
                                 .replaceAll("\\{\\{GUILD_USER_COUNT}}", event.getGuild().getMemberCache().size() + "");
            publicChannel.sendMessage(msg).queue();
        }
    }
    
    /**
     * This will fire when the bot joins a guild and we check if we are allowed to join this guild
     *
     * @param event The corresponding {@link GuildJoinEvent}
     */
    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        //Temp disable that
        //if 60 of a guild is bots, we'll leave it
//        double[] botToUserRatio = AirUtils.getBotRatio(event.getGuild());
//        if (botToUserRatio[1] > 60) {
//            AirUtils.getPublicChannel(event.getGuild()).sendMessage(String.format("Hey %s, %s%s of this guild are bots (%s is the total btw). Iḿ outta here.",
//                    event.getGuild().getOwner().getAsMention(),
//                    botToUserRatio[1],
//                    "%",
//                    event.getGuild().getMemberCache().size())).queue(
//                    message -> message.getGuild().leave().queue()
//            );
//            AirUtils.log(Settings.defaultName + "GuildJoin", Level.INFO, "Joining guild: " + event.getGuild().getName() + ", and leaving it after. BOT ALERT");
//            return;
//        }
        AirUtils.log(Settings.defaultName + "GuildJoin", Level.INFO, "Joining guild: " + event.getGuild().getName() + ".");
        GuildSettingsUtils.registerNewGuild(event.getGuild());
        AirUtils.updateGuildCount(event.getJDA(), event.getJDA().asBot().getShardManager().getGuildCache().size());
    }
    
    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        GuildSettingsUtils.deleteGuild(event.getGuild());
        AirUtils.updateGuildCount(event.getJDA(), event.getJDA().asBot().getShardManager().getGuildCache().size());
    }
    
    /**
     * This will fire when a member leaves a channel in a guild, we check if the channel is empty and if it is we leave it
     *
     * @param event {@link GuildVoiceLeaveEvent}
     */
    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (!event.getVoiceState().getMember().getUser().getId().equals(event.getJDA().getSelfUser().getId()) && event.getGuild().getAudioManager().isConnected()) {
            if (!event.getChannelLeft().getId().equals(event.getGuild().getAudioManager().getConnectedChannel().getId())) {
                return;
            }
            if (event.getChannelLeft().getMembers().size() <= 1) {
                AirUtils.audioUtils.getMusicManager(event.getGuild()).player.stopTrack();
                AirUtils.audioUtils.getMusicManager(event.getGuild()).player.setPaused(false);
                AirUtils.audioUtils.getMusicManager(event.getGuild()).scheduler.queue.clear();
                lastGuildChannel.get(event.getGuild()).sendMessage(EmbedUtils.embedMessage("Leaving voice channel because all the members have left it.")).queue();
                if (event.getGuild().getAudioManager().isConnected()) {
                    event.getGuild().getAudioManager().closeAudioConnection();
                    event.getGuild().getAudioManager().setSendingHandler(null);
                }
            }
        }
    }
    
    /**
     * This will fire when a member moves from channel, if a member moves we will check if our channel is empty
     *
     * @param event {@link GuildVoiceMoveEvent}
     */
    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (!event.getVoiceState().getMember().getUser().getId().equals(event.getJDA().getSelfUser().getId()) && event.getGuild().getAudioManager().isConnected()) {
            if (event.getChannelLeft() != null) {
                if (!event.getChannelLeft().getId().equals(event.getGuild().getAudioManager().getConnectedChannel().getId())) {
                    return;
                }
                if (event.getChannelLeft().getMembers().size() <= 1) {
                    AirUtils.audioUtils.getMusicManager(event.getGuild()).player.stopTrack();
                    AirUtils.audioUtils.getMusicManager(event.getGuild()).player.setPaused(false);
                    AirUtils.audioUtils.getMusicManager(event.getGuild()).scheduler.queue.clear();
                    lastGuildChannel.get(event.getGuild()).sendMessage(EmbedUtils.embedMessage("Leaving voice channel because all the members have left it.")).queue();
                    if (event.getGuild().getAudioManager().isConnected()) {
                        event.getGuild().getAudioManager().closeAudioConnection();
                        event.getGuild().getAudioManager().setSendingHandler(null);
                    }
                }
            }
            
            if (event.getChannelJoined() != null) {
                if (!event.getChannelJoined().getId().equals(event.getGuild().getAudioManager().getConnectedChannel().getId())) {
                    return;
                }
                if (event.getChannelJoined().getMembers().size() <= 1) {
                    AirUtils.audioUtils.getMusicManager(event.getGuild()).player.stopTrack();
                    AirUtils.audioUtils.getMusicManager(event.getGuild()).player.setPaused(false);
                    AirUtils.audioUtils.getMusicManager(event.getGuild()).scheduler.queue.clear();
                    lastGuildChannel.get(event.getGuild()).sendMessage(EmbedUtils.embedMessage("Leaving voice channel because all the members have left it.")).queue();
                    if (event.getGuild().getAudioManager().isConnected()) {
                        event.getGuild().getAudioManager().setSendingHandler(null);
                        event.getGuild().getAudioManager().closeAudioConnection();
                    }
                }
            }
            
        }
    }
}
