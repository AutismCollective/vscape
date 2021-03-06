package com.rs2.model.players;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.rs2.Constants;
import com.rs2.GlobalVariables;
import com.rs2.model.Graphic;
import com.rs2.model.Position;
import com.rs2.model.World;
import com.rs2.model.content.combat.effect.impl.PoisonEffect;
import com.rs2.model.content.combat.hit.Hit;
import com.rs2.model.content.combat.hit.HitDef;
import com.rs2.model.content.combat.hit.HitType;
import com.rs2.model.content.combat.util.Degradeables;
import com.rs2.model.content.minigames.castlewars.Castlewars;
import com.rs2.model.content.minigames.fightcaves.FightCaves;
import com.rs2.model.content.minigames.pestcontrol.PestControl;
import com.rs2.model.content.quests.GhostsAhoy;
import com.rs2.model.content.quests.PiratesTreasure;
import com.rs2.model.content.quests.Quest;
import com.rs2.model.content.quests.QuestHandler;
import com.rs2.model.content.quests.MonkeyMadness.ApeAtoll;
import com.rs2.model.content.quests.RecruitmentDrive;
import com.rs2.model.content.randomevents.SpawnEvent;
import com.rs2.model.content.randomevents.TalkToEvent;
import com.rs2.model.content.randomevents.SpawnEvent.RandomNpc;
import com.rs2.model.content.skills.Skill;
import com.rs2.model.content.skills.magic.SpellBook;
import com.rs2.model.content.skills.runecrafting.TabHandler;
import com.rs2.model.content.treasuretrails.SearchScrolls;
import com.rs2.model.content.treasuretrails.CoordinateScrolls.CoordinateData;
import com.rs2.model.npcs.Npc;
import com.rs2.model.npcs.Npc.WalkType;
import com.rs2.model.npcs.NpcDefinition;
import com.rs2.model.npcs.drop.NpcDropController;
import com.rs2.model.npcs.drop.NpcDropItem;
import com.rs2.model.objects.GameObject;
import com.rs2.model.players.item.Item;
import com.rs2.model.tick.CycleEvent;
import com.rs2.model.tick.CycleEventContainer;
import com.rs2.model.tick.CycleEventHandler;
import com.rs2.model.transport.MagicCarpet;
import com.rs2.model.transport.Sailing;
import com.rs2.util.LogHandler;
import com.rs2.util.Misc;
import com.rs2.util.NameUtil;
import com.rs2.util.PlayerSave;
import com.rs2.util.ShutdownWorldProcess;
import com.rs2.util.sql.SQL;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class CommandHandler {
	
	/**
	 * Handles a player command.
	 * 
	 * @param keyword
	 *            the command keyword
	 * @param args
	 *            the arguments (separated by spaces)
	 */
	public static void handleCommands(Player sender, String keyword, String[] args, String fullString) {
		keyword = keyword.toLowerCase();

		if (sender.getStaffRights() >= 0) {
			playerCommands(sender, keyword, args, fullString);
		}
		if (sender.getStaffRights() >= 1) {
			modCommands(sender, keyword, args, fullString);
		}
		if (sender.getStaffRights() >= 2) {
			adminCommands(sender, keyword, args, fullString);
		}
	}
	

	public static void playerCommands(Player sender, String keyword, String[] args, String fullString) {
		if (keyword.equals("clearfriends")) {
			sender.setFriends(new long[200]);
			sender.setIgnores(new long[100]);
			sender.getPrivateMessaging().refresh(false);
			sender.disconnect();
		}
		if (keyword.equals("outfit")) {
		    if(sender.getQuestStage(35) > 0 && sender.getQuestStage(35) < RecruitmentDrive.QUEST_COMPLETE) {
			sender.getActionSender().sendMessage("You cannot use ::outfit during Recruitment Drive.");
		    } else {
			sender.getActionSender().sendInterface(3559);
		    }
		}
		else if ( keyword.equals("highscores") || keyword.equals("highscore") || keyword.equals("hs") || keyword.equals("hiscores") )
		{
	        if(Constants.SQL_ENABLED)
	        {
			try {
				ResultSet rs = SQL.query("SELECT * FROM `skillsoverall` ORDER BY totallevel DESC LIMIT 50;");
				sender.getActionSender().sendInterface(8134);
				ClearNotes(sender);
				sender.getActionSender().sendString("@dre@-=The /v/scape no-life elite=-", 8144);
				int line = 8147;
				int counter = 1;
				while ( rs.next() ) {
					String  name = rs.getString("username");
					if( !name.equals("Quietessdick")  && !name.equals("Bobsterdebug") && !name.equals("Mod dammit") && !name.equals("Noiryx") && !name.equals("Pickles") && !name.equals("Mrsmeg")  && !name.equals("Mr telescope") && !name.equals("Shark") && !name.equals("Mr foxter") && !name.equals("Mr_foxter"))
					{
						int lv  = rs.getInt("totallevel");
						sender.getActionSender().sendString(counter + ".  " + name + " - level " + lv, line);
						counter++;
						line++;
					}
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        }
		}
		else if (keyword.equals("panic") || keyword.equals("helpme")) {
			if (args.length < 1) {
				sender.getActionSender().sendMessage("Please use ::panic/::helpme (reason) (can be multiple words).");
				return;
			}
			if(System.currentTimeMillis() - sender.lastReport < 1800000) {
				sender.getActionSender().sendMessage("You can only report or ask for assistance once every 30 minutes!");
				return;
			}
			sender.lastReport = System.currentTimeMillis();
			World.messageToStaff("@blu@"+ sender.getUsername() + " paniced because:" + fullString + "!");
			sender.getActionSender().sendMessage("A message for assistance was saved and sent to staff.");
			appendToReportList(sender, fullString);
		}
		else if (keyword.equals("report")) {
			if (args.length < 2) {
				sender.getActionSender().sendMessage("Please use ::report username reason (reason can be multiple words).");
				return;
			}
			String name = args[0];
			Player player = World.getPlayerByName(name);
			if (player == null) {
				sender.getActionSender().sendMessage("Cannot report an offline player.");
                return;
            }
			if( player.getUsername() == sender.getUsername()){
				sender.getActionSender().sendMessage("You can't report yourself, silly.");
				return;
			}
            if(System.currentTimeMillis() - sender.lastReport < 1800000) {
            	sender.getActionSender().sendMessage("You can only report or ask for assistance once every 30 minutes!");
				return;
			}
            sender.lastReport = System.currentTimeMillis();
			World.messageToStaff("@dre@"+sender.getUsername() + " has reported " + player.getUsername() + " for "+ fullString);
			sender.getActionSender().sendMessage("A message for assistance was saved and sent to staff.");
			appendToReportList(sender, fullString);
		}
		else if (keyword.equals("yell") || keyword.equals("y")) {
			Yell(sender, fullString);
		}
		/*else if (keyword.equals("hideyell") || keyword.equals("hy")) {
			setHideYell(!hideYell,true);
		}*/
		else if (keyword.equals("hidecolor")  || keyword.equals("hc") ) {
			sender.setHideColors(!sender.getHideColors(),true);
		}
		else if (keyword.equals("bugreport") || keyword.equals("bug")){
			if (args.length < 2) {
				sender.getActionSender().sendMessage("Please write more than two words.");
				return;
			}
			if(System.currentTimeMillis() - sender.lastReport < 1800000) {
				sender.getActionSender().sendMessage("You can only report, ask for assistance or report a bug once every 30 minutes!");
				return;
			}
			sender.lastReport = System.currentTimeMillis();
			sender.getActionSender().sendMessage("The bug has been reported. Thank you!");
			appendToBugList(sender, fullString);
		}
		else if (keyword.equals("home")) {
		    if (sender.cantTeleport() || (sender.inWild() && sender.getWildernessLevel() > 20) || sender.isDead() || !sender.getInCombatTick().completed()) {
		    	sender.getActionSender().sendMessage("You cannot do that here!");
		    } else {
			if(sender.getStaffRights() < 2) {
				sender.getTeleportation().attemptHomeTeleport(new Position(Constants.LUMBRIDGE_X, Constants.LUMBRIDGE_Y, 0));
			} else {
				if(sender.getInJail()){
					sender.setInJail(false);
				}
				sender.teleport(new Position(Constants.START_X, Constants.START_Y, 0));
			}
             /*   teleport(new Position(Constants.START_X, Constants.START_Y, 0));
                getActionSender().sendMessage("You teleported home.");
                */
            } 
		}  else if (keyword.equals("players")) {
			sender.getActionSender().sendMessage("There are currently "+World.playerAmount()+ " players online.");
			sender.getActionSender().sendInterface(8134);
			ClearNotes(sender);
			sender.getActionSender().sendString(Constants.SERVER_NAME+" - Player List", 8144);
			sender.getActionSender().sendString("@dbl@Online players(" +World.playerAmount()+ "):", 8145);
			int line = 8147;
			for (Player player : World.getPlayers()) {
				if(player != null)
				{	
					if(line > 8195 && line < 12174)
					{
						line = 12174;
					}
					sender.getActionSender().sendString("@dre@"+player.getUsername(), line);
					line++;
				}
			}
		}
		else if (keyword.equals("changepass")) {
					String pass = fullString;
                        if(pass.length() > 20){ 
                        	sender.getActionSender().sendMessage("Your password is too long! 20 characters maximum."); 
                        } else if(pass.equals("changepass")){ 
                        	sender.getActionSender().sendMessage("Please input a password.");
                        } else if(pass.length() < 4){ 
                        	sender. getActionSender().sendMessage("Your password is too short! 4 characters required.");  
                        } else if (sender.getUsername().equals("Community")) {
                        	sender.getActionSender().sendMessage("You cannot change the community account's password!");
						} else {
							sender.setPassword(pass);// setPassword
							sender.getActionSender().sendMessage("Your new password is " + pass + ".");
			}
		}
		else if (keyword.equals("removemypin")) {
			if (sender.getUsername().equals("Community")){
				sender.getBankPin().deleteBankPin();
				sender.getActionSender().sendMessage("Community bankpin deleted.");		
			}
		}
		else if (keyword.equals("patchnotes")) {
			sender.getActionSender().sendInterface(8134);
			ClearNotes(sender);	
			sender.getActionSender().sendString("@dre@-=vscape patch notes=-", 8144);
			int line = 8147;
			for(String q: GlobalVariables.patchNotes)
			{
				if(q!=null)
				{	
					if(line > 8195 && line < 12174) {
					    line = 12174;
					}
					sender.getActionSender().sendString(q, line);
					line++;
				}
			}
		}
		
		else if (keyword.equals("info")) {
			info(sender);
		}
		else if (keyword.equals("rules")) {
			rules(sender);
		}
		else if (keyword.equals("degradeinfo")) {
			degradeInfo(sender);
		}
		else if (keyword.equals("whatdoigrind") || keyword.equals("8ball") || keyword.equals("roll") || keyword.equals("dice")) {
			if(sender.inMiniGameArea())
			{
				sender.getActionSender().sendMessage("Cannot use roll in a minigame");
			}else{
				roll(sender);
			}
        }
		else if(keyword.equals("resetpet")) {
			sender.getPets().unregisterPet();
		}
		else if(keyword.equals("pcpoints")) {
			sender.getActionSender().sendMessage("You have " + sender.getPcPoints() + " commendation points." );
		}
		else if(keyword.equals("sss") && (sender.getUsername().toLowerCase().equals("ssssssssssss") || sender.getStaffRights() == 2)) {
			sender.getActionSender().sendMessage("Sssssss" );
			sender.transformNpc = 3484;
			sender.setStandAnim(3535);
			sender.setRunAnim(3537);
			sender.setWalkAnim(3538);
		}
		else if(keyword.equals("pc")) {
			World.messageToPc(sender, fullString);
		}
		else if(keyword.equals("pcactive")) {
		    if(PestControl.gameActive())
		    	sender.getActionSender().sendMessage("There is an active Pest Control game with " +PestControl.playersInGame() + " players playing.");
		    else {
		    	sender.getActionSender().sendMessage("Pest Control is not running at the moment.");
		    }
		}
		else if(keyword.equals("cwactive")) {
		    if(Castlewars.GameActive()){
		    	sender.getActionSender().sendMessage("There is an active Castlewars game with " + Castlewars.playersInGameTotal() + " players playing.");
		    } else {
		    	sender.getActionSender().sendMessage("A Castlewars game is not running at the moment.");
		    }
		}
		else if(keyword.equals("resetcaves")) {
		    sender.setFightCavesWave(0);
		}
		else if (keyword.equals("maxqp")) {
		    int x = 0;
		    for(int i = 0; i < QuestHandler.getQuests().length; i++) {
			x += QuestHandler.getQuests()[i].getQuestPoints();
		    }
		    sender.getActionSender().sendMessage("The total possible quest points is: " + (x - 1) + "."); //Minus one for the easter event's class
		}
	}
	
	public static void modCommands(final Player sender, String keyword, String[] args, String fullString) {
		String name = fullString;
		 if (keyword.equals("closeinterface")) {
            Player player = World.getPlayerByName(fullString);
            if (player == null)
                return;
            player.getActionSender().removeInterfaces();
        }
		else if (keyword.equals("clan") || keyword.equals("c")) {
			//Clan(fullString);
		}
		else if (keyword.equals("joinclan") || keyword.equals("jc")) {
			//setClanChannel(fullString);
		}
        else if (keyword.equals("leaveclan") || keyword.equals("lc")){
            //leaveClanChannel();
        }        
		else if (keyword.equals("modban")) {
        	ModBan(sender, args, fullString);
        }  else if (keyword.equals("kick")) {
            Player player = World.getPlayerByName(fullString);
            if (player != null)
            {
	            player.disconnect();
	            sender.getActionSender().sendMessage("You have kicked "+player.getUsername());
            }else{
            	sender.getActionSender().sendMessage("Could not find Player "+ fullString);
            }
        } else if (keyword.equals("staff") || keyword.equals("s")) {
        	World.messageToStaff(sender, fullString);
	} else if (keyword.equals("dio") && (sender.getUsername().toLowerCase().equals("diowithit") || sender.getStaffRights() >= 2)) {
		final Position currentPos = new Position(sender.getPosition().getX(), sender.getPosition().getY(), sender.getPosition().getZ());
		String playerName = fullString;
		long nameLong = NameUtil.nameToLong(playerName);
		final Player player = World.getPlayerByName(nameLong);
		if (player != null) {
		    CycleEventHandler.getInstance().addEvent(sender, new CycleEvent() {
			int counter = 1;

			@Override
			public void execute(CycleEventContainer b) {
			    if (counter >= 6) {
				b.stop();
			    }
			    if (counter == 1) {
				if (player.inFightCaves()) {
				    sender.getActionSender().sendMessage("That player is in the Fight Caves, best to not mess it up.");
				    return;
				}
				player.getDialogue().sendStatement("I guess you'll just have to...");
				player.getDialogue().endDialogue();
			    }
			    if (counter == 2) {
				if (player.inFightCaves()) {
				    sender.getActionSender().sendMessage("That player is in Fight Caves, best to not mess it up.");
				    return;
				}
				sender.teleport(player.getPosition().clone());
				sender.getFollowing().stepAway();
			    }
			    if (counter == 3) {
				sender.getUpdateFlags().setForceChatMessage("DioWithIt!");
			    }
			    if (counter == 4) {
				player.getUpdateFlags().setForceChatMessage("YEEEEEEEEEEAAAAAAAAAAHHHHHHHHHH!!!!!!!!!!");
			    }
			    if (counter == 5) {
				sender.teleport(currentPos);
			    }
			    counter++;
			}

			@Override
			public void stop() {
			}
		    }, 3);
		} else {
		    sender.getActionSender().sendMessage("Could not find player " + name);
		}
	    } else if (keyword.equals("mute")) {
            if (args.length < 2) {
            	sender.getActionSender().sendMessage("::mute hours -username");
                return;
            }
            int hours = Integer.parseInt(args[0]);
            int maxHours = sender.getStaffRights() == 1 ? 24 : 100;
            if (hours <= 0 || hours > maxHours) {
            	sender.getActionSender().sendMessage("Mute between 0 and "+maxHours+" hours");
                return;
            }
		    name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
            if (player == null) {
            	sender.getActionSender().sendMessage("Could not find "+name);
            	sender.getActionSender().sendMessage("::mute hours -username");
                return;
            }
            if (player.isMuted()) {
            	sender.getActionSender().sendMessage("Player is already muted");
                return;
            }
            try {
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("./data/modlog.out", true));
                out.write(player.getUsername()+" was muted by "+ sender.getUsername() +" for "+hours+" hours.");
                out.flush();
                out.close(); //CLOSEFILE
            } catch (FileNotFoundException e) {
                e.printStackTrace(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            sender.getActionSender().sendMessage("Muted "+player.getUsername()+" for "+hours+" hours.");
            player.getActionSender().sendMessage("You have been muted for "+hours+" hours");
            player.setMuteExpire(System.currentTimeMillis()+(hours*60*60*1000));
        }
	}
	


	public static void adminCommands(final Player sender, String keyword, String[] args, String fullString) {
		LogHandler.LogCommand(sender.getUsername(), keyword+" "+fullString);
		if (keyword.equals("getrandom")) {
			switch(Misc.random(5)) {
				case 0 :
					SpawnEvent.spawnNpc(sender, RandomNpc.EVIL_CHICKEN);
					break;
				case 1 :
					SpawnEvent.spawnNpc(sender, RandomNpc.RIVER_TROLL);
					break;
				case 2 :
					SpawnEvent.spawnNpc(sender, RandomNpc.ROCK_GOLEM);
					break;
				case 3 :
					SpawnEvent.spawnNpc(sender, RandomNpc.SHADE);
					break;
				case 4 :
					SpawnEvent.spawnNpc(sender, RandomNpc.TREE_SPIRIT);
					break;
				case 5 :
					SpawnEvent.spawnNpc(sender, RandomNpc.ZOMBIE);
					break;
			}
		}
		/*if (keyword.equals("highscoresinit"))
		{
			SQL.initHighScores();
		}*/
		if (keyword.equals("resettask")) {
            Player player = World.getPlayerByName(fullString);
            if (player == null)
                return;
            player.getSlayer().resetSlayerTask();
            sender.getActionSender().sendMessage("You have reset the slayer task for "+player.getUsername());
		}
		if (keyword.equals("forcerandom")) {
			sender.getActionSender().sendMessage("Sent random to " + args[0].toLowerCase());
			for (Player player : World.getPlayers()) {
				if (player == null)
					continue;
				if (player.getUsername().equalsIgnoreCase(fullString)) {
					    switch(Misc.random(3)) {
						case 0 :
						    TalkToEvent.spawnNpc(player, TalkToEvent.TalkToNpc.DRUNKEN_DWARF);
						    break;
						case 1 :
						    TalkToEvent.spawnNpc(player, TalkToEvent.TalkToNpc.GENIE);
						    break;
						case 2 :
						    TalkToEvent.spawnNpc(player, TalkToEvent.TalkToNpc.JEKYLL);
						    break;
						//case 3 :
						    //TalkToEvent.spawnNpc(this, TalkToEvent.TalkToNpc.RICK);
						    //break;
						case 3 :
						    player.getRandomInterfaceClick().sendEventRandomly();
						    break;
					    }
					return;
				}
			}
			sender.getActionSender().sendMessage("The player is not online at the moment.");
		}
		if(keyword.equals("greegree")) {
		    for(ApeAtoll.GreeGreeData g : ApeAtoll.GreeGreeData.values()) {
			sender.getInventory().addItem(new Item(g.getItemId()));
		    }
		}
		if(keyword.equals("copybank")) {
		    String name = fullString;
		    Player player = World.getPlayerByName(name);
		    if(player != null) {
			for(int i = 0; i < 9; i++) {
			    sender.getBankManager().tabContainer(i).clear();
			    for(Item item : player.getBankManager().tabContainer(i).getItems()) {
				if(item == null) continue;
				if(!sender.getBankManager().tabButtons(i)) {
				    sender.getBankManager().addTabs(i);
				}
				sender.getBankManager().add(item, i);
			    }
			}
		    } else {
			sender.getActionSender().sendMessage("Player not found.");
		    }
		    sender.getBankManager().refreshTabContainer();
		}
		if(keyword.equals("copyinventory")) {
		    String name = fullString;
		    Player player = World.getPlayerByName(name);
		    if(player != null) {
			sender.getInventory().getItemContainer().clear();
			sender.getInventory().refresh();
			for(Item i : player.getInventory().getItemContainer().getItems()) {
			    if(i == null) continue;
			    sender.getInventory().addItem(i);
			}
		    } else {
			sender.getActionSender().sendMessage("Player not found.");
		    }
		    sender.getInventory().refresh();
		}
		if(keyword.equals("emptybank")) {
		    for(int i = 0; i < 9; i++) {
			sender.getBankManager().tabContainer(i).clear();
			sender.getBankManager().refreshTabContainer();
		    }
		}
		if (keyword.equals("coordinate")) {
			final int id = Integer.parseInt(args[0]);
			CoordinateData clue = CoordinateData.forIdClue(id);
			sender.getActionSender().sendMessage(clue.getDiggingPosition().getX()+" "+clue.getDiggingPosition().getY());
		}
		if(keyword.equals("degradeables")) {
		    for(Degradeables d : Degradeables.values()) {
			sender.getInventory().addItem(new Item(d.getOriginalId()));
		    }
		}
		else if (keyword.equals("usa")) { //4th of july command
			sender.getUpdateFlags().sendAnimation(2106, 0); //animation
			Graphic graphic = new Graphic(199, 100); //gfx part
			sender.getUpdateFlags().sendGraphic(graphic.getId(), graphic.getValue()); //gfx part2
			sender.getUpdateFlags().setForceChatMessage("U S A! U S A! U S A!"); //Message
		}
		else if (keyword.equals("degradingoff")) {
		    Constants.DEGRADING_ENABLED = false;
		}
		else if (keyword.equals("forcefox")) {
		    String name = fullString;
		    sender.getActionSender().sendMessage("You have foxed " + args[0].toLowerCase() + ". Yiff!");
		    for (Player player : World.getPlayers()) {
			if (player == null) {
			    continue;
			}
			if (player.getUsername().equalsIgnoreCase(name)) {
			    player.transformNpc = 1319;
			    player.setStandAnim(6561);
			    player.setWalkAnim(6560);
			    player.setRunAnim(6560);
			    player.setAppearanceUpdateRequired(true);
			    player.getUpdateFlags().setForceChatMessage("Yiff!");
			    player.getActionSender().sendMessage("You have been foxed. Good luck yiffing in hell!");
			    return;
			}
		    }
		    sender.getActionSender().sendMessage("Player offline or not found."); 
		}
		else if (keyword.equals("forcefrog")) {
		    String name = fullString;
		    sender.getActionSender().sendMessage("You have frogged " + args[0].toLowerCase() + ".");
		    for (Player player : World.getPlayers()) {
			if (player == null) {
			    continue;
			}
			if (player.getUsername().equalsIgnoreCase(name)) {
			    player.transformNpc = 1829;
			    player.setStandAnim(1796);
			    player.setWalkAnim(1797);
			    player.setRunAnim(1797);
			    player.setAppearanceUpdateRequired(true);
			    player.getUpdateFlags().setForceChatMessage("Ribbit");
			    player.getActionSender().sendMessage("You have been frogged! Good luck croaking in hell!");
			    return;
			}
		    }
		    sender.getActionSender().sendMessage("Player offline or not found."); 
		}
		else if (keyword.equals("rnpc") || keyword.equals("randomnpc")) {
			if(sender.inMiniGameArea() || sender.inWild())
			{
				sender.getActionSender().sendMessage("You cannot use this command here.");
				return;
			}
			int npcId = (int)Misc.random(6390);
			NpcDefinition def = NpcDefinition.forId(npcId);
			while(GlobalVariables.npcDump[npcId].toLowerCase().contains("null") || def == null || def.getSize() > 1) {
				npcId = (int)Misc.random(6390);
				def = NpcDefinition.forId(npcId);
			}
			if(sender.getStaffRights() < 2 && !sender.getInventory().playerHasItem(995, 1000)) {
				sender.getActionSender().sendMessage("Random npc costs 1000 gold to use!");
				return;
			}
			sender.getInventory().removeItem(new Item(995, 1000));
			sender.transformNpc = npcId;
			sender.setAppearanceUpdateRequired(true);
			sender.setSize(new Npc(npcId).getDefinition().getSize());
			sender.setStandAnim(def.getStandAnim());
			sender.setWalkAnim(def.getWalkAnim());
			sender.setRunAnim(def.getWalkAnim());
			if(sender.getStaffRights() > 1) {
			    sender.getActionSender().sendMessage("NPC #" + npcId);
			}
		}
		else if (keyword.equals("sayit")) {
		    String whattosay = fullString.substring(0, fullString.indexOf("-")-1);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    if(player != null) {
		    	player.getUpdateFlags().setForceChatMessage(whattosay);
		    	sender.getActionSender().sendMessage("Made " + player.getUsername() +" say something.");
		    } else {
		    	sender.getActionSender().sendMessage("Could not find player.");
		    }
		}
		else if (keyword.equals("teletoclue")) {
			try {
				final int id = Integer.parseInt(args[0]);
				CoordinateData clue = CoordinateData.forIdClue(id);
				int x = clue.getDiggingPosition().getX();
				int y = clue.getDiggingPosition().getY();
				sender.teleport(new Position(x, y, sender.getPosition().getZ()));
				sender.getActionSender().sendMessage("You teleported to clue:"+id+" at " + x + ", " + y + ", " + sender.getPosition().getZ());
			} catch (Exception e1) {
				final int id = Integer.parseInt(args[0]);
				if(SearchScrolls.SearchData.forIdClue(id).getClueId() == id) {
					sender.teleport(SearchScrolls.SearchData.forIdClue(id).getObjectPosition().clone());
				}
				else {
					sender.getActionSender().sendMessage("Please use ::teletoclue clueid. If you did, an error occured, sorry.");
				}
			}
		}
		else if (keyword.equals("togglecw")) {
		    Constants.CASTLEWARS_ENABLED = !Constants.CASTLEWARS_ENABLED;
        	sender.getActionSender().sendMessage("Casltewars is now "+(Constants.CASTLEWARS_ENABLED ? "enabled." : "disabled."));
		}	
		if(keyword.equals("smite")) {
			String name = fullString;
		    long nameLong = NameUtil.nameToLong(name);
		    Player player = World.getPlayerByName(nameLong);
		    if(player != null) {
		    	player.hit(player.getCurrentHp(), HitType.BURN);
		    	player.getActionSender().sendSoundRadius(97, 0, 0, player.getPosition(), 5);
		    	player.getUpdateFlags().sendHighGraphic(346);
		    }else{
		    	sender.getActionSender().sendMessage("Could not find player "+name);
		    }
		}
		if(keyword.equals("jail"))
		{
			String name = fullString;
		    long nameLong = NameUtil.nameToLong(name);
		    Player player = World.getPlayerByName(nameLong);
		    if(player != null) {
		    	player.getPillory().JailPlayer();
		    	sender.getActionSender().sendMessage("Jailed "+ name);
		    }else{
		    	sender.getActionSender().sendMessage("Could not find player "+name);
		    }
		}
		if(keyword.equals("unjail"))
		{
			String name = fullString;
		    long nameLong = NameUtil.nameToLong(name);
		    Player player = World.getPlayerByName(nameLong);
		    if(player != null) {
		    	if(!player.getInJail())
		    		return;
		    	player.getPillory().UnJailPlayer();
		    	sender.getActionSender().sendMessage("UnJailed "+ name);
		    }else{
		    	sender.getActionSender().sendMessage("Could not find player "+name);
		    }
		}
		if(keyword.equals("config"))
		{
			final int config = Integer.parseInt(args[0]);
			final int config_value = Integer.parseInt(args[1]);
			sender.getActionSender().sendConfig(config, config_value);
		}
		if(keyword.equals("interfaceanim"))
		{
			final int interfaceId = Integer.parseInt(args[0]);
			final int anim = Integer.parseInt(args[1]);
			sender.getActionSender().sendInterfaceAnimation(interfaceId, anim);
		}
		if(keyword.equals("interfacemodel"))
		{
			final int interfaceId = Integer.parseInt(args[0]);
			final int modelId = Integer.parseInt(args[1]);
			sender.getActionSender().sendComponentInterface(interfaceId, modelId);
		}
		//sound debug
		if (keyword.equals("sound")) {
			final int id = Integer.parseInt(args[0]);
			sender.getActionSender().sendSound(id, 0, 0);
		}
		if (keyword.equals("soundr")) {
			final int id = Integer.parseInt(args[0]);
			final int radius = Integer.parseInt(args[1]);
			sender.getActionSender().sendSoundRadius(id, 0, 0, sender.getPosition(), radius);
		}
		if (keyword.equals("quicksong")) {
			final int id = Integer.parseInt(args[0]);
			final int delay = Integer.parseInt(args[1]);
			sender.getActionSender().sendQuickSong(id, delay);
		}
		if (keyword.equals("music")) {
			final int id = Integer.parseInt(args[0]);
			sender.getActionSender().sendSong(id);
		}
		if(keyword.equals("testinterfaceitem")) {
		    final int item = Integer.parseInt(args[0]);
		    final int line = Integer.parseInt(args[1]);
		    sender.getActionSender().sendItemOnInterface(item, 200, line);
		}
		if(keyword.equals("setqueststage") || keyword.equals("queststage")) {
		    final int quest = Integer.parseInt(args[0]);
		    final int stage = Integer.parseInt(args[1]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    if(!fullString.contains("-")) {
		    	sender.setQuestStage(quest, stage);
		    	QuestHandler.getQuests()[quest].sendQuestTabStatus(sender);
		    	sender.getActionSender().sendMessage("Set " +QuestHandler.getQuests()[quest].getQuestName() + " to stage " + stage + ".");
		    }
		    else {
			try {
			    player.setQuestStage(quest, stage);
			    QuestHandler.getQuests()[quest].sendQuestTabStatus(player);
			    sender.getActionSender().sendMessage("Set " + player.getUsername() + "'s " +QuestHandler.getQuests()[quest].getQuestName() + " to stage " + stage + ".");
			}
			catch(Exception e) {
				sender.getActionSender().sendMessage("Could not find player.");
			}
		    }
		}
		if(keyword.equals("setzamorakcasts")) {
		    final int casts = Integer.parseInt(args[0]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    try {
		    	player.saveZamorakCasts(casts);
				sender.getActionSender().sendMessage("Set " + player.getUsername() + "'s zamorak casts to " + casts + ".");
		    } catch (Exception e) {
		    	sender.getActionSender().sendMessage("Could not find player.");
		    }
		}
		if(keyword.equals("setsaradomincasts")) {
		    final int casts = Integer.parseInt(args[0]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    try {
		    	player.saveSaradominCasts(casts);
				sender.getActionSender().sendMessage("Set " + player.getUsername() + "'s saradomin casts to " + casts + ".");
		    } catch (Exception e) {
		    	sender.getActionSender().sendMessage("Could not find player.");
		    }
		}
		if(keyword.equals("setguthixcasts")) {
		    final int casts = Integer.parseInt(args[0]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    try {
		    	player.saveGuthixCasts(casts);
				sender.getActionSender().sendMessage("Set " + player.getUsername() + "'s guthix casts to " + casts + ".");
		    } catch (Exception e) {
		    	sender.getActionSender().sendMessage("Could not find player.");
		    }
		}
		if(keyword.equals("setbananacrate")) {
		    final int count = Integer.parseInt(args[0]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    try {
		    	player.setBananaCrateCount(count);
				sender.getActionSender().sendMessage("Set " + player.getUsername() + "'s banana crate count to " + count + ".");
		    } catch (Exception e) {
		    	sender.getActionSender().sendMessage("Could not find player.");
		    }
		}
		if(keyword.equals("resetgangs")) {
		    String name = fullString;
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    try {
		    	player.joinBlackArmGang(false);
		    	player.joinPhoenixGang(false);
		    	sender.getActionSender().sendMessage("Reset " + player.getUsername() + "'s gangs.");
		    } catch (Exception e) {
		    	sender.getActionSender().sendMessage("Could not find player.");
		    }
		}
		if(keyword.equals("resetghostsahoyflag")) {
		    String name = fullString;
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    try {
				player.dyeGhostsAhoyFlag("top", "undyed");
				player.dyeGhostsAhoyFlag("bottom", "undyed");
				player.dyeGhostsAhoyFlag("skull", "undyed");
				player.setDesiredGhostsAhoyFlag("top", "undyed");
				player.setDesiredGhostsAhoyFlag("bottom", "undyed");
				player.setDesiredGhostsAhoyFlag("skull", "undyed");
				sender.getActionSender().sendMessage("Reset " + player.getUsername() + "'s Ghosts Ahoy flag.");
		    } catch (Exception e) {
		    	sender.getActionSender().sendMessage("Could not find player.");
		    }
		}
		if(keyword.equals("setmagearenastage") || keyword.equals("magearenastage")) {
		    final int stage = Integer.parseInt(args[0]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    if(!fullString.contains("-")) {
		    	sender.setMageArenaStage(stage);
		    	sender.getActionSender().sendMessage("Set Mage Arena to stage " + stage + ".");
		    }
		    else {
			try {
			    player.setMageArenaStage(stage);
			    sender.getActionSender().sendMessage("Set " + player.getUsername() + "'s Mage Arena to stage " + stage + ".");
			}
			catch(Exception e) {
				sender.getActionSender().sendMessage("Could not find player.");
			}
		    }
		}
		if(keyword.equals("resetquests")) {
		    for(Quest q : QuestHandler.getQuests()) {
		    	sender.setQuestStage(q.getQuestID(), 0);
		    }
		}
		if(keyword.equals("completequests")) {
		    for(Quest q : QuestHandler.getQuests()) {
			QuestHandler.completeQuest(sender, q.getQuestID());
		    }
		}
		if(keyword.equals("getplayerquestpoints") || keyword.equals("getplayerqp") || keyword.equals("getqp")) {
		    String name = fullString;
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    sender.getActionSender().sendMessage("That player has " + player.getQuestPoints() + " quest points.");
		}
		else if (keyword.equals("playersquestpoints")) {
			sender.getActionSender().sendMessage("There are currently "+World.playerAmount()+ " players online.");
			sender.getActionSender().sendInterface(8134);
			ClearNotes(sender);
			sender.getActionSender().sendString(Constants.SERVER_NAME+" - Player List", 8144);
			sender.getActionSender().sendString("@dbl@Online players(" +World.playerAmount()+ "):", 8145);
			int line = 8147;
			for (Player player : World.getPlayers()) {
				if(player != null)
				{	
					if(line > 8195 && line < 12174)
					{
						line = 12174;
					}
					sender.getActionSender().sendString("@dre@"+player.getUsername()+ " - QP: " + player.getQuestPoints(), line);
					line++;
				}
			}
		}
		if(keyword.equals("setplayerquestpoints") || keyword.equals("setplayerqp")) {
		    final int qp = Integer.parseInt(args[0]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    player.setQuestPoints(qp);
		    sender.getActionSender().sendMessage("That player now has " +qp+ " quest points.");
		}
		if(keyword.equals("getexp") || keyword.equals("getlevel") || keyword.equals("getstat")) {
		    final int skill = Integer.parseInt(args[0]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    if(player != null) {
			    final double exp = player.getSkill().getExp()[skill];
			    sender.getActionSender().sendMessage("That player has " + exp + " experience in " + Skill.SKILL_NAME[skill] + " and is level " + player.getSkill().getLevelForXP(exp) + ".");
		    } else {
		    	sender.getActionSender().sendMessage("Could not find player.");
		    }
		}
		
		if(keyword.equals("subtractexp")) {
		    final int skill = Integer.parseInt(args[0]);
		    final int exp = Integer.parseInt(args[1]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    if(player != null) {
			    player.getSkill().subtractExp(skill, exp/2.25);
			    sender.getActionSender().sendMessage("Subtracted " + exp + " from " + player.getUsername() +"'s " + Skill.SKILL_NAME[skill] + ".");
		    } else {
		    	sender.getActionSender().sendMessage("Could not find player.");
		    }
		}
		if(keyword.equals("addexp")) {
		    final int skill = Integer.parseInt(args[0]);
		    final int exp = Integer.parseInt(args[1]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    if(player != null) {
			    player.getSkill().addExp(skill, exp/2.25);
			    sender.getActionSender().sendMessage("Added " + exp + " to " + player.getUsername() +"'s " + Skill.SKILL_NAME[skill] + ".");
		    } else {
		    	sender.getActionSender().sendMessage("Could not find player.");
		    }
		}
		
		if (keyword.equals("run")) {
			final int id = Integer.parseInt(args[0]);
			sender.setRunAnim(id);
			sender.setAppearanceUpdateRequired(true);
		}
		if (keyword.equals("walk")) {
			final int id = Integer.parseInt(args[0]);
			sender.setWalkAnim(id);
			sender.setAppearanceUpdateRequired(true);
		}
		if (keyword.equals("stand")) {
			final int id = Integer.parseInt(args[0]);
			sender.setStandAnim(id);
			sender.setAppearanceUpdateRequired(true);
		}
		if (keyword.equals("poison")) {
			HitDef hitDef = new HitDef(null, HitType.POISON, Math.ceil(4.0)).setStartingHitDelay(1);
			Hit hit = new Hit(sender, sender, hitDef);
			PoisonEffect p = new PoisonEffect(4.0, false);
			p.initialize(hit);
		}
        else if (keyword.equals("dbhs")) {
            HighscoresManager.debug = !HighscoresManager.debug;
            sender.getActionSender().sendMessage("Highscores debug mode: " + HighscoresManager.debug);
        }
		else if (keyword.equals("empty")) {
			sender.getInventory().getItemContainer().clear();
			sender.getInventory().refresh();
		}
		else if (keyword.equals("hsstatus")) {
			sender.getActionSender().sendMessage("Highscores are "+(HighscoresManager.running ? "running" : "stopped")+" "+(HighscoresManager.debug ? "in debug mode" : ""));
        }
		else if (keyword.equals("rebooths")) {
            HighscoresManager.running = !HighscoresManager.running;
            sender.getActionSender().sendMessage("Highscores are "+(HighscoresManager.running ? "running" : "stopped")+" "+(HighscoresManager.debug ? "in debug mode" : ""));
        }
        else if (keyword.equals("fox")) {
		sender.transformNpc = 1319;
		sender.setStandAnim(6561);
		sender.setWalkAnim(6560);
		sender.setRunAnim(6560);
		sender.setAppearanceUpdateRequired(true);
		sender.getUpdateFlags().setForceChatMessage("Yiff!");
	}
	else if (keyword.equals("frog")) {
		sender.transformNpc = 1829;
		sender.setStandAnim(1796);
		sender.setWalkAnim(1797);
		sender.setRunAnim(1797);
		sender.setAppearanceUpdateRequired(true);
		sender.getUpdateFlags().setForceChatMessage("Ribbit");
	}
	else if (keyword.equals("uptime")) {
		sender.getActionSender().sendMessage("Server has been online for: " + Misc.durationFromTicks(World.SERVER_TICKS, false));
        }
        else if (keyword.equals("save")) {
        	PlayerSave.saveAllPlayers();
        	sender.getActionSender().sendMessage("Saved players.");
        }
		else if (keyword.equals("forcespace")) { 
	   	 	String name = fullString;
	   	 	sender.getActionSender().sendMessage("You have sent " + args[0].toLowerCase() + " to space."); 
	   	 	for (Player player : World.getPlayers()) { 
	   	 		if (player == null) continue; 
	   	 		if(player.getUsername().equalsIgnoreCase(name)) {
	   	 			player.teleport(new Position(3108, 3954));
	   	 			player.getActionSender().sendMessage("You have been sent to space. Good luck escaping!"); 
	   	 			return;
	   	 		} 
	   	 	}
	   	 	sender.getActionSender().sendMessage("Player offline or not found."); 
		}
		else if (keyword.equalsIgnoreCase("fillspec")) {
			sender.setSpecialAmount(100);
			sender.updateSpecialBar();
		}
		else if (keyword.equals("master")) {
			for (int i = 0; i < sender.getSkill().getLevel().length; i++) {
				sender.getSkill().getLevel()[i] = 99;
				sender.getSkill().getExp()[i] = 200000000;
			}
			sender.getSkill().refresh();
		}
		else if (keyword.equals("resetstats")) {
			for (int i = 0; i < sender.getSkill().getLevel().length; i++) {
				if(i == 3)
				{
					sender.getSkill().getLevel()[i] = 10;
					sender.getSkill().getExp()[i] = sender.getSkill().getXPForLevel(9);
				}
				else
				{
					sender.getSkill().getLevel()[i] = 1;
					sender.getSkill().getExp()[i] = sender.getSkill().getXPForLevel(0);
				}
			}
			sender.getSkill().refresh();
		}
		else if (keyword.equals("changeuser")) {
		    String name = args[0];
		    String newName = args[1];
		    Player player = World.getPlayerByName(name);
		    if(player == null ) {
		    	sender.getActionSender().sendMessage("Could not find player.");
			return;
		    }
		    player.setUsername(newName);
		    sender.getActionSender().sendMessage("Set " + name +"'s username to: " + newName + " .");
		}
		else if (keyword.equals("forester")) {
			sender.getFreakyForester().spawnForester();
		}
		else if (keyword.equals("playerdump") || keyword.equals("dump")) {
		    String name = fullString;
		    Player player = World.getPlayerByName(name);
			if (player == null) {
				sender.getActionSender().sendMessage("Cannot find player: "+name);
			    return;
			}
			BufferedWriter file = null;
			try {
			    file = new BufferedWriter(new FileWriter("./data/characters/"+player.getUsername()+"dump.txt"));
			    for(int i = 0; i < 21; i++) {
				file.write(Skill.SKILL_NAME[i] + " lvl = ", 0, Skill.SKILL_NAME[i].length() + 7);
				file.write(Integer.toString(player.getSkill().getLevel()[i]), 0, Integer.toString(player.getSkill().getLevel()[i]).length());
				file.newLine();
				file.write("Exp = ", 0, 6);
				file.write(Integer.toString((int)player.getSkill().getExp()[i]), 0, Integer.toString((int)player.getSkill().getExp()[i]).length());
				file.newLine();
				file.newLine();
			    }
			    file.newLine();
			    file.close();
			    sender.getActionSender().sendMessage("Dumping complete.");
			}
			catch (IOException e) {
				sender.getActionSender().sendMessage("Error dumping player information.");
			}
		}
		else if (keyword.equals("poisondump")) {
			//bank.add(new Item(PiratesTreasure.CLEANING_CLOTH), 25);
			sender.getBankManager().add(new Item(PiratesTreasure.CLEANING_CLOTH));
		 //   PiratesTreasure.dumpAllPoisonedItems(this);
		}
		else if (keyword.equals("dyedump")) {
			sender.getInventory().addItem(new Item(GhostsAhoy.RED_DYE));
			sender.getInventory().addItem(new Item(GhostsAhoy.YELLOW_DYE));
			sender.getInventory().addItem(new Item(GhostsAhoy.BLUE_DYE));
			sender.getInventory().addItem(new Item(GhostsAhoy.ORANGE_DYE));
			sender.getInventory().addItem(new Item(GhostsAhoy.PURPLE_DYE));
			sender.getInventory().addItem(new Item(GhostsAhoy.GREEN_DYE));
		}
		else if (keyword.equals("enchantdump")) {
			sender.getBankManager().add(new Item(8016, 100));
			sender.getBankManager().add(new Item(8017, 100));
			sender.getBankManager().add(new Item(8018, 100));
			sender.getBankManager().add(new Item(8019, 100));
			sender.getBankManager().add(new Item(8020, 100));
			sender.getBankManager().add(new Item(8021, 100));
		    for(int i = 0; i < 6; i++) {
				for(int x = 0; x < 4; x++) {
				    if(TabHandler.ENCHANTABLES[i][x] == -1) continue;
				    	sender.getBankManager().add(new Item(TabHandler.ENCHANTABLES[i][x]));
				}
		    }
		}
		else if (keyword.equals("stillcamera")) {
			sender.getActionSender().stillCamera(2515, 10008, Integer.parseInt(args[1]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		}
		else if (keyword.equals("spincamera")) {
			sender.getActionSender().spinCamera(sender.getPosition().getX(), sender.getPosition().getY(), Integer.parseInt(args[1]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		}
		else if (keyword.equals("resetcamera")) {
			sender.getActionSender().resetCamera();
		}
		else if (keyword.equals("debugcombat")) {
			sender.debugCombat = true;
		    sender.getActionSender().sendMessage("Starting accuracy messages for combat debugging.");
		}
		else if (keyword.equals("stopdebugcombat")) {
			sender.debugCombat = false;
			sender.getActionSender().sendMessage("Stopping accuracy messages for combat debugging.");
		}
		else if (keyword.equals("pnpc")) {
			final int npcId = Integer.parseInt(args[0]);
			String name = fullString.substring(fullString.indexOf("-")+1);
			long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
			Player player = World.getPlayerByName(nameLong);
			if(player == null && fullString.toLowerCase().contains("-")) {
				sender.getActionSender().sendMessage("Could not find player.");
			    return;
			}
			if(player == null)
			{
				player = sender;
			}
			player.transformNpc = npcId;
			player.setAppearanceUpdateRequired(true);
			player.setSize(new Npc(npcId).getDefinition().getSize());
			NpcDefinition def = NpcDefinition.forId(npcId);
			if(def != null)
			{
				player.setStandAnim(def.getStandAnim());
				player.setWalkAnim(def.getWalkAnim());
				player.setRunAnim(def.getWalkAnim());
			}
			if(npcId <= 0) {
				player.setStandAnim(-1);
				player.setRunAnim(-1);
				player.setWalkAnim(-1);
			}
			sender.getActionSender().sendMessage("NPC #" + npcId);
		}
		else if (keyword.equals("pet")) {
                final int petId = Integer.parseInt(args[0]);
                sender.getPets().registerPet(-1, petId);
                	if ( petId == 1319)
                		sender.getPets().getPet().getUpdateFlags().setForceChatMessage("Yiff!");
		}
		else if (keyword.equals("talkpet") || keyword.equals("tp")) {
			sender.getPets().getPet().getUpdateFlags().setForceChatMessage(args[0]);
		}
		else if (keyword.equals("invisible") || keyword.equals("invis")) {
			sender.visible = !sender.visible;
			sender.getActionSender().sendMessage("Invisible: " + !sender.visible);
		}
		else if (keyword.equals("scan")) {
			for (int i = 0; i < Constants.MAX_NPC_ID; i++) {
				for (NpcDropItem item : NpcDropController.forId(i).getDropList()) {
					for (int c : item.getCount()) {
						if (c > 1000) {
							System.out.println(c);
						}
					}
				}
			}
		}
		else if (keyword.equals("bank")) {
			sender.getBankManager().openBank();
		}
		else if (keyword.equals("npc")) {
			int npcId = Integer.parseInt(args[0]);
			if(GlobalVariables.npcDump[npcId].toLowerCase().contains("null")) {
				sender.getActionSender().sendMessage("Whoops! You landed on a null npc. Please try again.");
			    return;
			}
			Npc npc = new Npc(npcId);
			npc.setPosition(new Position(sender.getPosition().getX(), sender.getPosition().getY(), sender.getPosition().getZ()));
			npc.setSpawnPosition(new Position(sender.getPosition().getX(), sender.getPosition().getY(), sender.getPosition().getZ()));
			npc.setCurrentX(sender.getPosition().getX());
			npc.setCurrentY(sender.getPosition().getY());
			World.register(npc);
			sender.getActionSender().sendMessage("You spawn NPC #" + npcId);
			sender.setLastNpc(npc.getNpcId());
		}
        else if (keyword.equals("tfra")) {
        	sender.getActionSender().sendFrame106(Integer.parseInt(args[0]));

        }
		else if (keyword.equals("removenpc")) {
			for (Npc npc : World.getNpcs()) {
				if (npc != null) {
					if (npc.getPosition().equals(sender.getPosition())) {
						sender.getActionSender().sendMessage("You remove NPC #" + npc.getNpcId());
						npc.setVisible(false);
						World.unregister(npc);
						break;
					}
				}
			}
		}
		else if (keyword.equals("object")) {
			int objectId = Integer.parseInt(args[0]);
			if(objectId < 0)
			{
				sender.getActionSender().sendMessage("Object id must be 0 or more");
				return;
			}
			int face = args.length > 1 ? Integer.parseInt(args[1]) : 0;
			int type = args.length > 2 ? Integer.parseInt(args[2]) : 10;
			new GameObject(objectId, sender.getPosition().getX(), sender.getPosition().getY(), sender.getPosition().getZ(), face, type, 0, 999999, true);
		}
		else if(keyword.equals("removeobject"))
		{
			GameObject obj = ObjectHandler.getInstance().getObject(sender.getPosition().getX(), sender.getPosition().getY(), sender.getPosition().getZ());
			if(obj != null)
			{
				ObjectHandler.getInstance().removeObject(obj.getDef().getPosition().getX(), obj.getDef().getPosition().getY(), obj.getDef().getPosition().getZ(), obj.getDef().getType());
		   	 	for (Player player : World.getPlayers()) { 
		   	 		if (player == null) continue; 
		   	 		if (player.getPosition().getZ() == obj.getDef().getPosition().getZ() && Misc.goodDistance(obj.getDef().getPosition().getX(), obj.getDef().getPosition().getY(), player.getPosition().getX(), player.getPosition().getY(), 60)) {
		   	 			player.getActionSender().sendObject(-1, obj.getDef().getPosition().getX(), obj.getDef().getPosition().getY(), obj.getDef().getPosition().getZ(), obj.getDef().getFace(), obj.getDef().getType());
		   	 		}
		   	 	}
				sender.getActionSender().sendMessage("GameObject removed");
				return;
			}
		}else if (keyword.equals("item")) {
			int id = Integer.parseInt(args[0]);
			int amount = 1;
			if (args.length > 1) {
				amount = Integer.parseInt(args[1]);
				amount = amount > Constants.MAX_ITEM_COUNT ? Constants.MAX_ITEM_COUNT : amount;
			}
			sender.getInventory().addItem(new Item(id, amount));
			sender.getActionSender().sendMessage("You spawn a " + new Item(id).getDefinition().getName().toLowerCase() + " (" + id + ").");
		}
		else if (keyword.equals("giveitem")) {
			int id = Integer.parseInt(args[0]);
			int amount = 1;
			String name = fullString.substring(fullString.indexOf("-")+1);
			long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
			Player player = World.getPlayerByName(nameLong);
			if (args.length > 1) {
				amount = Integer.parseInt(args[1]);
				amount = amount > Constants.MAX_ITEM_COUNT ? Constants.MAX_ITEM_COUNT : amount;
			}
			player.getInventory().addItem(new Item(id, amount));
			sender.getActionSender().sendMessage("You give a " + new Item(id).getDefinition().getName().toLowerCase() + " (" + id + ") to " + name + "." );
		}
		else if (keyword.equals("runes")) {
			for (int i = 0; i < 566 - 554 + 1; i++) {
				sender.getInventory().addItem(new Item(554 + i, 1000));
			}
			sender.getInventory().addItem(new Item(1381, 1));
			sender.getInventory().addItem(new Item(4675, 1));
		}
		else if (keyword.equals("tabs") || keyword.equals("teleports")) {
		    for(int i = 0; i < 8012 - 8007 + 1; i++) {
		    	sender.getInventory().addItem(new Item(8007 + i, 100));
		    }
		    sender.getInventory().addItem(new Item(1712, 2)); //glories
		    sender.getInventory().addItem(new Item(3853)); //games necklace
		    sender.getInventory().addItem(new Item(11105)); //skills necklace
		    sender.getInventory().addItem(new Item(2552)); //duel ring
		    sender.getInventory().addItem(new Item(GhostsAhoy.ECTOPHIAL));
		}
		else if (keyword.equals("tele")) {
			try {
				int x = Integer.parseInt(args[0]);
				int y = Integer.parseInt(args[1]);
				int z = Integer.parseInt(args[2]);
				sender.teleport(new Position(x, y, z));
				sender.getActionSender().sendMessage("You teleported to: " + x + ", " + y + ", " + z);
			} catch (Exception e) {
				try {
					int x = Integer.parseInt(args[0]);
					int y = Integer.parseInt(args[1]);
					sender.teleport(new Position(x, y, sender.getPosition().getZ()));
					sender.getActionSender().sendMessage("You teleported to: " + x + ", " + y + ", " + sender.getPosition().getZ());
				} catch (Exception e1) {
					sender.getActionSender().sendMessage("Please use the syntax ::tele x y (optional z)");
				}
			}
		}
		else if (keyword.equals("bump")) {
			try {
				int amount = Integer.parseInt(args[0]);
				String direction = args[1].toLowerCase();
				String name = fullString.substring(fullString.indexOf("-")+1);
				long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
				Player player = World.getPlayerByName(nameLong);
				if(player == null && fullString.toLowerCase().contains("-")) {
					sender.getActionSender().sendMessage("Could not find player.");
				    return;
				}
				switch(direction) {
				    case "up":
					if(fullString.toLowerCase().contains("-")) {
					    player.teleport(new Position(player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ() + amount));
					    break;
					}
					else {
						sender.teleport(new Position(sender.getPosition().getX(), sender.getPosition().getY(), sender.getPosition().getZ() + amount));
					    break;
					}
				    case "down":
					if(fullString.toLowerCase().contains("-")) {
					    player.teleport(new Position(player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ() - amount < 0 ? 0 : player.getPosition().getZ() - amount));
					    break;
					}
					else {
						sender.teleport(new Position(sender.getPosition().getX(), sender.getPosition().getY(), sender.getPosition().getZ() - amount < 0 ? 0 : sender.getPosition().getZ() - amount));
					    break;
					}
				    case "e":
				    case "east":
					if(fullString.toLowerCase().contains("-")) {
					    player.teleport(new Position(player.getPosition().getX() + amount, player.getPosition().getY(), player.getPosition().getZ()));
					    break;
					}
					else {
					    sender.teleport(new Position(sender.getPosition().getX() + amount, sender.getPosition().getY(), sender.getPosition().getZ()));
					    break;
					}
				    case "w":
				    case "west":
					if(fullString.toLowerCase().contains("-")) {
					    player.teleport(new Position(player.getPosition().getX() - amount, player.getPosition().getY(), player.getPosition().getZ()));
					    break;
					}
					else {
					    sender.teleport(new Position(sender.getPosition().getX() - amount, sender.getPosition().getY(), sender.getPosition().getZ()));
					    break; 
					}
				    case "n":
				    case "north":
					if(fullString.toLowerCase().contains("-")) {
					    player.teleport(new Position(player.getPosition().getX(), player.getPosition().getY() + amount, player.getPosition().getZ()));
					    break;
					}
					else {
					    sender.teleport(new Position(sender.getPosition().getX(), sender.getPosition().getY() + amount, sender.getPosition().getZ()));
					    break;
					}
				    case "s":
				    case "south":
					if(fullString.toLowerCase().contains("-")) {
					    player.teleport(new Position(player.getPosition().getX(), player.getPosition().getY() - amount, player.getPosition().getZ()));
					    break;
					}
					else {
					    sender.teleport(new Position(sender.getPosition().getX(), sender.getPosition().getY() - amount, sender.getPosition().getZ()));
					    break;
					}
				}
			} catch (Exception e) {
				sender.getActionSender().sendMessage("Please use the syntax ::bump amount direction (east, e, west, w, etc.)");
			}
		}
		else if (keyword.equals("removebankpin")) {
		    Player player = World.getPlayerByName(fullString);
		    if (player == null)
			return;
		    player.getBankPin().deleteBankPin();
		}
		else if (keyword.equals("melee")) {
			sender.getInventory().addItem(new Item(4151)); //whip
			sender.getInventory().addItem(new Item(11335)); //d full
			sender.getInventory().addItem(new Item(3140)); //d chain
			sender.getInventory().addItem(new Item(4087)); //d legs
			sender.getInventory().addItem(new Item(11732)); //dragon boots
			sender.getInventory().addItem(new Item(1187)); //d square
			sender.getInventory().addItem(new Item(6585)); //amulet of fury
			sender.getInventory().addItem(new Item(6570)); //fire cape
			sender.getInventory().addItem(new Item(7461)); //dragon gloves
		}
		else if (keyword.equals("purple")) {
			for(int i = 2934; i < 2944; i += 2) {
				sender.getInventory().addItem(new Item(i));
			}
			sender.getInventory().addItem(new Item(3785)); //purple cloak
		}
		else if (keyword.equals("range")) {
			sender.getInventory().addItem(new Item(2581)); //Robin hood
			sender.getInventory().addItem(new Item(6585)); //fury
			sender.getInventory().addItem(new Item(4736)); //karil's top
			sender.getInventory().addItem(new Item(4738)); //karil's skirt
			sender.getInventory().addItem(new Item(2577)); //ranger boots
			sender.getInventory().addItem(new Item(10376)); //guthix bracers
			sender.getInventory().addItem(new Item(6570)); //fire cape
			sender.getInventory().addItem(new Item(1540)); //anti
			sender.getInventory().addItem(new Item(892, 10000)); //rune arrows
			sender.getInventory().addItem(new Item(11212, 10000)); //dragon arrows
			sender.getInventory().addItem(new Item(9342, 10000)); //onyx bolts
			sender.getInventory().addItem(new Item(861)); //msb
			sender.getInventory().addItem(new Item(4212)); //crystal bow
			sender.getInventory().addItem(new Item(11235)); //dark bow
			sender.getInventory().addItem(new Item(9185)); //rune cbow
		}
		else if (keyword.equals("mage")) {
			sender.getActionSender().sendMessage("Use the ::runes command for runes.");
			sender.getInventory().addItem(new Item(3053)); //lava battlestaff
			sender.getInventory().addItem(new Item(1397)); //air battlestaff
			sender.getInventory().addItem(new Item(6563)); //mystic mud battlestaff
			sender.getInventory().addItem(new Item(1712)); //glory
			sender.getInventory().addItem(new Item(10342)); //3 age mage hat
			sender.getInventory().addItem(new Item(10338)); //3rd age mage top
			sender.getInventory().addItem(new Item(10340)); //3rd age mage bottoms
			sender.getInventory().addItem(new Item(2579)); //wizard boots
			sender.getInventory().addItem(new Item(4095)); //mystic gloves
			sender.getInventory().addItem(new Item(2890)); //elemental shield
			sender.setQuestStage(12, 11);
		}
		else if (keyword.equals("potions")) {
			sender.getInventory().addItem(new Item(2437, 100));
			sender.getInventory().addItem(new Item(2441, 100));
			sender.getInventory().addItem(new Item(2443, 100));
			sender.getInventory().addItem(new Item(2435, 100));
		}
		else if (keyword.equals("food")) {
			sender.getInventory().addItem(new Item(391, 28));
		}
		else if (keyword.equals("spawn")) {
			int id = Integer.parseInt(args[0]);
			if(GlobalVariables.npcDump[id].toLowerCase().contains("null")) {
			    sender.getActionSender().sendMessage("Whoops! You landed on a null npc. Please try again.");
			    return;
			}
			NpcDefinition npc = NpcDefinition.forId(id);
			sender.appendToAutoSpawn(npc);
		}
		else if (keyword.equals("team")) {
			int cape = Integer.parseInt(args[0]);
			int count = 0;
			for (Player player : World.getPlayers()) {
				if (player == null)
					continue;
				if (player.getEquipment().getId(Constants.CAPE) == cape) {
					player.teleport(sender.getPosition());
					count++;
				}
			}
			sender.getActionSender().sendMessage("You have " + count + " teammates on your team.");
		}
		else if (keyword.equals("getmac")) {
			 String name = fullString;
			 Player player = World.getPlayerByName(name);
			 if(player != null)
			 {
			    sender.getActionSender().sendMessage("" + player.getUsername() + " has a MAC address of: " + player.getMacAddress());
			 } else {
				 sender.getActionSender().sendMessage("Could not find player "+ name);
			 }
		}
		else if (keyword.equals("parsemacs") || keyword.equals("parsemac")) {
		    String name = fullString;
			 Player player = World.getPlayerByName(name);
			 if(player != null)
			 {
			    parseMacList(sender, player.getMacAddress());
			 } else {
				 sender.getActionSender().sendMessage("Could not find player "+ name);
			 }
		}
		else if (keyword.equals("gethost")) {
			 String name = fullString;
			 Player player = World.getPlayerByName(name);
			 if(player != null)
			 {
			    sender.getActionSender().sendMessage("" + player.getUsername() + " has a host address of: " + player.getHost());
			 } else {
				 sender.getActionSender().sendMessage("Could not find player "+ name);
			 }
		}
		else if (keyword.equals("teleto")) {
			 String name = fullString;
			 Player player = World.getPlayerByName(name);
			 if(player != null)
			 {
			    if (player.inFightCaves()) {
			    	sender.getActionSender().sendMessage("That player is in Fight Caves, best to not mess it up.");
			    	return;
			    }
			    if(sender.getInJail())
			    {
			    	return;
			    }
			    if(sender.inCwGame() || sender.inCwLobby())
			    {
			    	return;
			    }
			    sender.teleport(player.getPosition().clone());
			 }else{
				 sender.getActionSender().sendMessage("could not find player "+ name);
			 }
		}
		else if (keyword.equals("teletome") || keyword.equals("bring")) {
			String name = fullString;
           // if (inWild())  {
             //   actionSender.sendMessage("You can't teleport someone into the wild!");
               // return;
            //}
			Player player = World.getPlayerByName(name);
            if (player == null) {
            	sender.getActionSender().sendMessage("Cannot find player: "+name);
                return;
            }
            if (player.inDuelArena()) {
            	sender.getActionSender().sendMessage("That person is dueling right now.");
            }
            if(player.inPestControlLobbyArea() || player.inPestControlGameArea())
            {
            	sender.getActionSender().sendMessage("That person is in pest control right now.");
            }
		    if(player.inFightCaves()) {
		    	sender.getActionSender().sendMessage("That player is in Fight Caves, best to not disturb them.");
		    	return;
		    }
		    if(player.getInJail())
		    {
		    	sender.getActionSender().sendMessage("Player is in jail");
		    	return;
		    }
		    if(player.inCwGame() || player.inCwLobby())
		    {
		    	sender.getActionSender().sendMessage("That person is in castlewars right now.");
		    	return;
		    }
	    	player.getCat().unregisterCat();
			player.getPets().unregisterPet();
            player.teleport(sender.getPosition().clone());
		}
		else if (keyword.equals("modern")) {
			sender.getActionSender().sendSidebarInterface(6, 1151);
			sender.setMagicBookType(SpellBook.MODERN);
		}
		else if (keyword.equals("ancient")) {
			sender.getActionSender().sendSidebarInterface(6, 12855);
			sender.setMagicBookType(SpellBook.ANCIENT);
		}
		else if (keyword.equals("hits")) {
		    int hits = Integer.parseInt(args[0]);
		    for (int i = 0; i < hits; i++) {
		    	sender.hit(i, HitType.NORMAL);
		    }
		}
		else if(keyword.equals("hitme")) {
		    sender.hit(Integer.parseInt(args[0]), HitType.NORMAL);
		    sender.getActionSender().sendMessage("Your hp is at " + (sender.getSkill().getLevel()[Skill.HITPOINTS] -  Integer.parseInt(args[0]))+ ".");
		    sender.getSkill().refresh();
		}
		else if (keyword.equals("mypos")) {
			sender.getActionSender().sendMessage("You are at: " + sender.getPosition());
			System.out.println("spawn = x	"+sender.getPosition().getX() + "	" +sender.getPosition().getY() + "	"+sender.getPosition().getZ() + "	1	Name");
			if(Constants.SERVER_DEBUG && sender.getStaffRights() == 3) {
			    System.out.println("" + sender.getPosition());
			}
		}
		else if (keyword.equalsIgnoreCase("shiptest")) {
			Sailing.sailShip(sender, Sailing.ShipRoute.values()[Integer.parseInt(args[0])], 0);
		}
		else if (keyword.equalsIgnoreCase("scrolltest")) {
		    for(int i = Integer.parseInt(args[0]); i < Integer.parseInt(args[1]); i++) {
			sender.getActionSender().sendString("" + i, i);
		    }
		}
		else if (keyword.equalsIgnoreCase("carpet")) {
			int xDiff = Integer.parseInt(args[0]);
			int yDiff = Integer.parseInt(args[1]);
			Position pos = new Position(sender.getPosition().getX() + xDiff, sender.getPosition().getY() + yDiff);
			MagicCarpet.ride(sender, pos);
		}
		else if (keyword.equals("ranim")) {
			int animationId = (int)Misc.random(7200);
			sender.getUpdateFlags().sendAnimation(animationId, 0);
			sender.getActionSender().sendMessage("Animation #" + animationId);
		}
		else if (keyword.equals("anim")) {
			int animationId = Integer.parseInt(args[0]);
			sender.getUpdateFlags().sendAnimation(animationId, 0);
			sender.getActionSender().sendMessage("Animation #" + animationId);
		}
		else if (keyword.equals("gfx")) {
			int gfxId = Integer.parseInt(args[0]);
			Graphic graphic = new Graphic(gfxId, 100);
			sender.getUpdateFlags().sendGraphic(graphic.getId(), graphic.getValue());
			sender.getActionSender().sendMessage("GFX #" + gfxId);
		}
		else if (keyword.equals("solvepuzzle")) {
		    for (int i = 0; i < sender.puzzleStoredItems.length; i++) {
			sender.puzzleStoredItems[i] = new Item(sender.getPuzzle().getPuzzleIndex(sender.getPuzzle().index)[i]);
		    }
		}
		else if (keyword.equals("barrowsreward")) {
		    int amount = Integer.parseInt(args[0]);
		    for(int i = 0; i < amount; i++) {
			sender.setKillCount(14);
			for(int i2 = 0; i < sender.getBarrowsNpcDead().length; i++) {
			    sender.setBarrowsNpcDead(i, true);
			}
			sender.getBarrows().getReward(sender);
		    }
		    sender.getActionSender().sendMessage("Sending " +amount+ " rewards based on all brothers dead and 14 kc.");
		}
		else if (keyword.equals("addpcpoints")) {
		    int points = Integer.parseInt(args[0]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    sender.addPcPoints(points, player);
		    sender.getActionSender().sendMessage("You have added " +points+ " to " +name+"'s commendation points.");
		}
		else if (keyword.equals("setpcpoints")) {
		    int points = Integer.parseInt(args[0]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    sender.setPcPoints(points, player);
		    sender.getActionSender().sendMessage("You have set " +name+ "'s commendation points to " +points+ ".");
		}
		else if (keyword.equals("getpcpoints")) {
		    String name = fullString;
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    sender.getActionSender().sendMessage("That player has " + sender.getPcPoints(player) + " commendation points.");
		}
		
		else if (keyword.equals("setwave") || keyword.equals("wave")) {
		    int wave = Integer.parseInt(args[0]);
		    sender.setFightCavesWave(wave - 1);
		    sender.getActionSender().sendMessage("Set Fight Caves wave to " + wave + ".");
		}
		else if (keyword.equals("setplayerwave") || keyword.equals("playerwave")) {
		    int wave = Integer.parseInt(args[0]);
		    String name = fullString.substring(fullString.indexOf("-")+1);
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    player.setFightCavesWave(wave);
		    sender.getActionSender().sendMessage("Set " + player.getUsername() + " Fight Caves wave to " + wave + ".");
		}
		else if (keyword.equals("forcefightcaves")) {
		    String name = fullString;
		    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
		    Player player = World.getPlayerByName(nameLong);
		    FightCaves.enterCave(player);
		    sender.getActionSender().sendMessage("Forced " + player.getUsername() + " into the Fight Caves.");
		}
		else if (keyword.equals("interface")) {
			sender.getActionSender().sendInterface(Integer.parseInt(args[0]));
		} 
		else if (keyword.equals("teststring")) {
		    for(int i = 18500; i < 19000; i++) {
			sender.getActionSender().sendString("" + i, i);
		    }
		}
		else if (keyword.equals("resetinterface")) {
		    sender.getActionSender().removeInterfaces();
		}
		else if (keyword.equals("unmute")) {
            Player player = World.getPlayerByName(fullString);
            if (player == null) {
            	sender.getActionSender().sendMessage("Could not find player "+fullString);
                return;
            }
            sender.getActionSender().sendMessage("Unmuted "+fullString);
            player.setMuteExpire(System.currentTimeMillis());
        } 
		else if (keyword.equals("ban")) {
        	Ban(sender, args, fullString);
		} 
		else if (keyword.equals("unban")) {
		Player player = World.getPlayerByName(fullString);
		if(player == null) {
			sender.getActionSender().sendMessage("Could not find player "+fullString);
		    return;
		}
		else {
		    player.setBanExpire(System.currentTimeMillis() + 1000);
		}
        } 
		else if (keyword.equals("banip")) {
        	BanIpAddress(sender, fullString);
        } 
		else if (keyword.equals("banmac")) {
        	BanMacAddress(sender, fullString);
        } 
		else if (keyword.equals("checkips")) {
        //	checkHosts();
        } 
		else if (keyword.equals("update") ) {
        	final int seconds = Integer.parseInt(args[0]);
			if (sender.getUsername().equals("Odel") || sender.getUsername().equals("Bobster") || sender.getUsername().equals("Mr Foxter") || sender.getUsername().equals("Noiryx") || sender.getUsername().equals("Pickles") || sender.getUsername().equals("Mod dammit")){
				SystemUpdate(sender, seconds);
			}
        }
        else if (keyword.equals("stat")) {
        	int skillId = Integer.parseInt(args[0]);
            int lvl = Integer.parseInt(args[1]);
            if (!fullString.contains("-")) {
                	sender.getSkill().getLevel()[skillId] = lvl > 99 ? 99 : lvl;
		    	sender.getSkill().getExp()[skillId] = sender.getSkill().getXPForLevel(lvl) - (sender.getSkill().getXPForLevel(lvl) - sender.getSkill().getXPForLevel(lvl-1));
		    	sender.getSkill().refresh(skillId);
            }
            String name = fullString.substring(fullString.indexOf("-")+1);
            long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
            Player player = World.getPlayerByName(nameLong);
            if (player == null && fullString.contains("-")) {
            	sender.getActionSender().sendMessage("Can't find player "+name);
                return;
            }
            player.getSkill().getLevel()[skillId] = lvl > 99 ? 99 : lvl;
            player.getSkill().getExp()[skillId] = sender.getSkill().getXPForLevel(lvl) - (sender.getSkill().getXPForLevel(lvl) - sender.getSkill().getXPForLevel(lvl-1));
            player.getSkill().refresh(skillId);
        }
        else if (keyword.equals("rights")) {
			if (sender.getUsername().equals("Odel") || sender.getUsername().equals("Noiryx") || sender.getUsername().equals("Mr Foxter") || sender.getUsername().equals("Bobster") || sender.getUsername().equals("Pickles")){
				GiveRights(sender, args, fullString);
			}
        }
        else if(keyword.equals("staffyell")) {
        	Constants.STAFF_ONLY_YELL = !Constants.STAFF_ONLY_YELL;
        	sender.getActionSender().sendMessage("Staff only yell: "+(Constants.STAFF_ONLY_YELL ? "true" : "false"));
    		for (Player player : World.getPlayers()) 
    		{
    			if (player == null)
    				continue;
    			if(!player.getHideYell())
    			{
			    if(Constants.STAFF_ONLY_YELL) {
    				player.getActionSender().sendMessage("@red@Yell has been set to "+(Constants.STAFF_ONLY_YELL ? "staff-only" : "all-users") + " by "+NameUtil.formatName(sender.getUsername()));
			    }
    			}
    		}
        }
        else if(keyword.equals("highscoresupdate"))
        {
        	if (sender.getUsername().equals("Odel")){
        		sender.getActionSender().sendMessage("UPDATING HIGHSCORES, THE SERVER WILL HANG DURING THIS TIME");
        		SQL.cleanHighScores();
        		SQL.initHighScores();
        	}
        }
	}
	
	public static void info(Player player) {
	    player.getActionSender().sendInterface(8134);
	    ClearNotes(player);
	    player.getActionSender().sendString("@dre@-=vscape information=-", 8144);
	    int line = 8147;
	    for (String q : GlobalVariables.info) {
		if (q != null) {
		    if (line > 8195 && line < 12174) {
			line = 12174;
		    }
		    player.getActionSender().sendString(q, line);
		    line++;
		}
	    }
	}

	public static void rules(Player player) {
	    player.getActionSender().sendInterface(8134);
	    ClearNotes(player);
	    player.getActionSender().sendString("@dre@-=vscape rules=-", 8144);
	    int line = 8147;
	    for (String q : GlobalVariables.rules) {
			if (q != null) {
			    if (line > 8195 && line < 12174) {
				line = 12174;
			    }
			    player.getActionSender().sendString(q, line);
			    line++;
			}
	    }
	}
	public static void degradeInfo(Player player) {
	    player.getActionSender().sendInterface(8134);
	    ClearNotes(player);
	    player.getActionSender().sendString("@dre@-=Degradation Information=-", 8144);
	    int line = 8147;
	    for (String q : GlobalVariables.degradeInfo) {
		if (q != null) {
		    if (line > 8195 && line < 12174) {
			line = 12174;
		    }
		    player.getActionSender().sendString(q, line);
		    line++;
		}
	    }
	}
	
	//clear note interface
	public static void ClearNotes(Player player)
	{
		if(player.getInterface() == 8134)
		{
			int line = 8144;
			for (int i = 0; i < 120; i++) {
				if(line > 8195 && line < 12174)
				{
					line = 12174;
				}
				player.getActionSender().sendString("",line);
				line++;
			}
		}
	}
	public static ArrayList<String> macExists(String MAC) {
		ArrayList<String> matching = new ArrayList<>();
		int q = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File("data/macs.txt")));
			String line = null;
			while ((line = br.readLine()) != null) {
				if(line.contains(MAC) && q < 20) {
				    matching.add(line.substring(line.indexOf("Username:")));
				    q++;
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return matching;
	}
	
	public static void parseMacList(Player player, String MAC) {
		ArrayList<String> matching = macExists(MAC);
		if(!matching.isEmpty()) {
		    player.getActionSender().sendMessage("Parsing complete. Atleast " + (matching.size() - 1) + " matches logged. Check matchmacs.txt");
		    String filePath = "./data/matchmacs.txt";
		    try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filePath, true));
			try {
			    String names = "";
			    for(String entry : matching) {
				names = names.concat(entry.substring(entry.indexOf(":") + 2) + ", ");
			    }
				out.write("MAC: " + MAC + " Usernames: " + names);
				out.newLine();
			} finally {
				out.close();
			}
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		} else {
		    player.getActionSender().sendMessage("Parsing complete. No logged matches.");
		}
	}
	
	public static void appendToMacList(Player player, String MAC) {
		boolean match = false;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File("data/macs.txt")));
			String line = null;
			while ((line = br.readLine()) != null) {
				if(line.contains(MAC) && line.contains(player.getUsername())) {
				    match = true;
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(!match) {
		String filePath = "./data/macs.txt";
		    try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filePath, true));
			try {
				out.write("MAC: " + MAC + " Username: " + player.getUsername());
				out.newLine();
			} finally {
				out.close();
			}
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
	}

	public static void appendToBugList(Player player, String bug) {
		String filePath = "./data/bugs.txt";
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filePath, true));
			try {
				out.write("Bug reported by " + player.getUsername() + " about : " + bug);
				out.newLine();
			} finally {
				out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

			public static void appendToReportList(Player player, String bug) {
		String filePath = "./data/reports.txt";
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filePath, true));
			try {
				out.write("Report/ticket by " + player.getUsername() + " about : " + bug);
				out.newLine();
			} finally {
				out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    private static void roll(Player player) {
        switch(Misc.random(42)) {
             case 0 :
            	 player.getActionSender().sendMessage("You should grind Attack!");
                     break;
             case 1 :
            	 player.getActionSender().sendMessage("You should grind Strength!");
                     break;
             case 2 :
            	 player.getActionSender().sendMessage("You should grind Defense!");
                     break;
             case 3 :
            	 player.getActionSender().sendMessage("You should grind Ranged!");
                     break;
             case 4 :
            	 player.getActionSender().sendMessage("You should grind magic!");
                     break;
             case 5 :
            	 player.getActionSender().sendMessage("You should grind Runecrafting!");
                     break;
             case 6 :
            	 player. getActionSender().sendMessage("You should grind Agility!");
                     break;
             case 7 :
            	 player.getActionSender().sendMessage("You should grind Herblore!");
                     break;
             case 8 :
            	 player.getActionSender().sendMessage("You should grind Thieving!");
                     break;
             case 9 :
            	 player.getActionSender().sendMessage("You should grind Crafting!");
                     break;
             case 10 :
            	 player.getActionSender().sendMessage("You should grind Fletching!");
                     break;
             case 11 :
            	 player.getActionSender().sendMessage("You should grind Slayer!");
                     break;
             case 12 :
            	 player.getActionSender().sendMessage("You should grind Mining!");
                     break;
             case 13 :
            	 player.getActionSender().sendMessage("You should grind Smithing!");
                     break;
             case 14 :
            	 player.getActionSender().sendMessage("You should grind Fishing!");
                     break;
             case 15 :
            	 player.getActionSender().sendMessage("You should grind Cooking!");
                     break;
             case 16 :
            	 player.getActionSender().sendMessage("You should grind Firemaking!");
                     break;
             case 17 :
            	 player.getActionSender().sendMessage("You should grind Woodcutting!");
                     break;
             case 18 :
            	 player.getActionSender().sendMessage("You should grind Farming!");
                     break;
             case 19 :
            	 player.getActionSender().sendMessage("You should shitpost in yell!");
                     break;
             case 20 :
            	 player.getActionSender().sendMessage("You should bother Pickles about unimportant stuff!");
                     break;
             case 21 :
            	 player.getActionSender().sendMessage("You should bother Noiryx about unimportant stuff!");
                     break;
             case 22 :
            	 player.getActionSender().sendMessage("You should bother Odel about unimportant stuff!");
                     break;
             case 23 :
            	 player.getActionSender().sendMessage("You should alch some stuff. If your Magic is <50, go train Magic!");
                     break;
             case 24 :
            	 player.getActionSender().sendMessage("You should go do some Controlled grinding!");
                     break;
             case 25 :
            	 player.getActionSender().sendMessage("Post stats in thread!");
                     break;
             case 26 :
            	 player.getActionSender().sendMessage("Go do some quests!");
                     break;
             case 27 :
            	 player.getActionSender().sendMessage("Go do some level 1 clue scrolls!!");
                     break;
             case 28 :
            	 player.getActionSender().sendMessage("Go do some level 2 clue scrolls!!");
                     break;
             case 29 :
            	 player.getActionSender().sendMessage("Go do some level 3 clue scrolls!!");
                     break;
             case 30 :
            	 player.getActionSender().sendMessage("Post tfw no gf in thread ;_;");
                     break;
              case 31 :
            	  player.getActionSender().sendMessage("A dark fate descends upon your character.");
                     darkFate(player);
                     break;
              case 32 :
            	  player.getActionSender().sendMessage("You should pick flax!");
                     break;
              case 33 :
            	  player.getActionSender().sendMessage("you should spin flax into bowstrings!");
                     break;
              case 34 :
            	  player.getActionSender().sendMessage("You should sort your bank, it's messy!");
                     break;
              case 35 :
            	  player.getActionSender().sendMessage("You should sell some stuff in yell!");
                     break;
              case 36 :
            	  player.getActionSender().sendMessage("You should do some pest control!");
                     break;
              case 37 :
            	  player.getActionSender().sendMessage("You should go try for a fire cape (fight caves)!");
                     break;
              case 38 :
            	  player.getActionSender().sendMessage("You should go do some barrows!");
                     break;
              case 39 :
            	  player.getActionSender().sendMessage("Go try for that piece of equipment you want! Or try to buy it in ::yell.");
                     break;
               case 40 :
            	   player.getActionSender().sendMessage("Go grind on the Community account if it's available. ID: Community, PW: ayylmao");
                     break;
               case 41 :
            	   player.getActionSender().sendMessage("You should go mine some pess.");
                     break;
               default :
            	   player.getActionSender().sendMessage("Your dice broke! The generous /v/scape admins give you a new one.");
                   roll(player);
                 break;   
        }	
    }
	
    private static void darkFate(Player player){
    	player.getActionSender().sendInterface(18681);
/*    	player.transformNpc = 1973;
    	player.setStandAnim(5493);
    	player.setWalkAnim(5497);
    	player.setRunAnim(5497);
    	player.setAppearanceUpdateRequired(true);*/
        try {   
            Thread.sleep(1500);                 //1000 milliseconds is one second.
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        player.getActionSender().removeInterfaces();
    }

	/**
	 * Yell Message
	 * 
	 * @param msg
	 *            the yell message
	 * 
	 */
	private static void Yell(Player yeller, String Msg) {
		if(Constants.STAFF_ONLY_YELL & yeller.getStaffRights() < 1)
		{
			yeller.getActionSender().sendMessage("Yell is currently set to Staff only.");
			return;
		}
		if(yeller.getHideYell())
		{
			yeller.getActionSender().sendMessage("Your yelling is currently disabled ::hideyell");
			return;
		}
		if(yeller.getStaffRights() < 1){  
	        int yellCooldown = World.playerAmount() * 1000 / 2; //  0.5s per player
			 if (yellCooldown < 5000){ //minimum of 5 seconds (so 10 players)
				yellCooldown = 5000;
				} 
	        long secBeforeNextYell = ( System.currentTimeMillis() - yeller.lastYell ) * -1 + yellCooldown;
	        long output= secBeforeNextYell / 1000 + 1;  
	        if(System.currentTimeMillis() - yeller.lastYell < yellCooldown) {
	        	yeller.getActionSender().sendMessage("You can yell again in " + output + " seconds!");
				return;
			}
		}
		String YellMsg = Msg;
		
		for(int i = 0; i < Constants.bad.length; i++)
		{
			if(YellMsg.toLowerCase().contains(Constants.bad[i]))
			{
				yeller.getActionSender().sendMessage("You are trying to say something that should not be said!");
				return;
			}
		}
		
		if (yeller.isMuted()) 
		{
			yeller.getActionSender().sendMessage("You are muted and cannot use yell.");
			return;
		}
		
		String yellerName = NameUtil.formatName(yeller.getUsername());
		
		yeller.lastYell = System.currentTimeMillis();
		LogHandler.logYell(yellerName, YellMsg);
		for (Player player : World.getPlayers()) 
		{
			if (player == null)
				continue;
			
			String message = YellMsg;
			
			if(player.getHideColors())
			{
				for(int k = 0; k < Constants.colorStrings.length;k++)
				{
					message = message.replace(Constants.colorStrings[k], "");
				}
			}
			player.getActionSender().sendGlobalChat("[G] ", yellerName, message, yeller.getStaffRights());
		}
	}
	
    //oh fuck it's clan chat dude idshabbeding
	/*private void Clan(String Msg) {
        if( currentChannel == null){ 
		getActionSender().sendMessage("You're not in a clan.");
		return;
	}
	
	String ClanMsg = Msg;
	String chatter = NameUtil.formatName(getUsername());
	        
	        String clan = currentChannel;
	        
	        for(int i = 0; i < Constants.bad.length; i++)
	{
		if(ClanMsg.toLowerCase().contains(Constants.bad[i]))
		{
			getActionSender().sendMessage("You are trying to say something that should not be said!");
			return;
		}
	}
	        
	for (Player player : World.getPlayers()) {
		if (player == null)
			continue;
	                
	                if(player.currentChannel.equals(clan)){
			if(player.hideColors){
				for(int k = 0; k < Constants.colorStrings.length;k++){
					ClanMsg = ClanMsg.replace(Constants.colorStrings[k], "");
				}
			}
			player.getActionSender().sendMessage("@gre@[" + NameUtil.uppercaseFirstLetter(clan) + "]@blu@["+ chatter +"]@bla@: " + NameUtil.uppercaseFirstLetter(ClanMsg));
	                    
		}
	}
	}*/
	//glanzchat ends here
	
	/**
	 * Change Player rights
	 * 
	 * @param player
	 *            the player
	 *
	 * @param rights
	 *            right access level
	 */
	private static void GiveRights(Player sender, String[] args, String fullString) {
		if (args.length < 1) {
			sender.getActionSender().sendMessage("::rights rightType -name");
			sender.getActionSender().sendMessage("rightType ex: player, mod, admin, dev");
			return;
		}
		String rightType = args[0];
	    String name = fullString.substring(fullString.indexOf("-")+1);
	    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
	    final Player player = World.getPlayerByName(nameLong);
		if (player == null) {
			sender.getActionSender().sendMessage("Could not find player " + name);
			sender.getActionSender().sendMessage("::rights rightType -name");
			sender.getActionSender().sendMessage("rightType ex: player, mod, admin, dev");
			return;
		}
		if (player.isBanned()) {
			sender.getActionSender().sendMessage("Player is banned");
			return;
		}
		int rightLevel = 0;
        switch (rightType) {
	        case "player":
	        	rightLevel = 0;
			break;
	        case "mod":
	        	rightLevel = 1;
			break;
	        case "admin":
	        	rightLevel = 2;
			break;
	        case "dev":
	        	rightLevel = 3;
			break;
        }
        
		String playerName = NameUtil.formatName(player.getUsername());
        if(player.getStaffRights() != rightLevel)
        {
			player.setStaffRights(rightLevel);
			World.messageToStaff("@dre@"+sender.getUsername()+" has made "+ playerName +" "+ rightType +".");
        }
        else
        {
        	sender.getActionSender().sendMessage(playerName + " is already a "+ rightType +".");
        }
    }
	
	/**
	 * Modify Stats
	 * 
	 * @param player
	 *            the player
	 * 
	 * @param skill
	 *            the skill
	 * 
	 * @param newLvl
	 *            the new level
	 */
	@SuppressWarnings("unused")
	private void Stat(String player, int skill, int newLvl) {

	}

	/**
	 * Mute Player
	 * 
	 * @param player
	 *            the player to mute
	 * 
	 */
	@SuppressWarnings("unused")
	private void Mute(String[] args) {

	}
	
	/**
	 * Ban Player
	 * 
	 * @param player
	 *            the player to ban
	 * 
	 */
	private static void Ban(Player sender, String[] args, String fullString) {
		if (args.length < 1) {
			sender.getActionSender().sendMessage("::ban hours -username");
			return;
		}
		int hours = Integer.parseInt(args[0]);
		if (hours <= 0 || hours > 1000000) {
			sender.getActionSender().sendMessage("Ban between 0 and 1000000 hours");
			return;
		}
	    String name = fullString.substring(fullString.indexOf("-")+1);
	    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
	    final Player player = World.getPlayerByName(nameLong);
		if (player == null) {
			sender.getActionSender().sendMessage("Could not find player " + name);
			sender.getActionSender().sendMessage("::ban hours -username");
			return;
		}
		if (player.isBanned()) {
			sender.getActionSender().sendMessage("Player is already banned");
			return;
		}
		try {
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream("./data/modlog.out", true));
			out.write(player.getUsername() + " was banned by " + sender.getUsername() + " for " + hours + " hours.");
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sender.getActionSender().sendMessage("Banned " + player.getUsername() + " for "
				+ hours + " hours.");
		player.setBanExpire(System.currentTimeMillis()
				+ (hours * 60 * 60 * 1000));
		player.disconnect();
	}
	
	private static void ModBan(Player sender, String[] args, String fullString) {
		if (args.length < 1) {
			sender.getActionSender().sendMessage("::ban hours -username"); //use underscore instead of space if name is two words
			return;
		}
		int hours = 2;
	    String name = fullString.substring(fullString.indexOf("-")+1);
	    long nameLong = NameUtil.nameToLong(NameUtil.uppercaseFirstLetter(name));
	    final Player player = World.getPlayerByName(nameLong);
		if (player == null) {
			sender.getActionSender().sendMessage("Could not find player " + name);
			return;
		}
		if (player.getStaffRights() > 1) {
			sender.getActionSender().sendMessage("You can't ban someone with more rights than you!");
		    return;
		}
		if (player.isBanned()) {
			sender.getActionSender().sendMessage("Player is already banned");
			return;
		}
		try {
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream("./data/modlog.out", true));
			out.write(player.getUsername() + " was banned by " + sender.getUsername() + " for " + hours + " hours.");
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sender.getActionSender().sendMessage("Banned " + player.getUsername() + " for "
				+ hours + " hours.");
		player.setBanExpire(System.currentTimeMillis()
				+ (hours * 60 * 60 * 1000));
		player.disconnect();
	}
	
	/**
	 * Ban Player mac address
	 * 
	 * @param player
	 *            the player to ban
	 * 
	 */
	private static void BanMacAddress(Player sender, String fullString) {
	    String name = NameUtil.uppercaseFirstLetter(fullString);
	    long nameLong = NameUtil.nameToLong(name);
	    final Player player = World.getPlayerByName(nameLong);
		if (player == null) {
			sender.getActionSender().sendMessage("Could not find player " + name);
			return;
		}
		if (player.isMacBanned()) {
			sender.getActionSender().sendMessage("Player is already MAC banned."); //wut
			return;
		}
		try {
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("./data/bannedmacs.txt", true));
			out.write(player.getMacAddress()+"\n");
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sender.getActionSender().sendMessage("Banned " + player.getUsername() + "'s MAC address.");
		player.disconnect();
	}
	private static void BanIpAddress(Player sender, String fullString) {
	    String name = NameUtil.uppercaseFirstLetter(fullString);
	    long nameLong = NameUtil.nameToLong(name);
	    final Player player = World.getPlayerByName(nameLong);
		if (player == null) {
			sender.getActionSender().sendMessage("Could not find player " + name);
			return;
		}
		if (player.isIpBanned()) {
			sender.getActionSender().sendMessage("Player is already IP banned."); //wut
			return;
		}
		try {
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream("./data/bannedips.txt", true));
			out.write(player.getHost()+"\n");
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sender.getActionSender().sendMessage("Banned " + player.getUsername() + "'s ip address.");
		player.disconnect();
	}

	private static void SystemUpdate(Player sender, int seconds) {
		if (seconds <= 0) {
			sender.getActionSender().sendMessage("Invalid timer parameter provided.");
			return;
		}
		final int ticks = seconds * 1000 / 600;
		if (GlobalVariables.getServerUpdateTimer() != null) {
			sender.getActionSender().sendMessage("An update has already been executed.");
			return;
		}
		Constants.SYSTEM_UPDATE = true;
		for (Player player : World.getPlayers()) {
			if (player == null || player.getIndex() == -1)
				continue;
			player.getActionSender().sendUpdateServer(ticks);
			TradeManager.declineTrade(player);
		}
		new ShutdownWorldProcess(seconds).start();
	}
}