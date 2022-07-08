package com.palmergames.bukkit.towny.object.comparators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.nation.DisplayedNationsListSortEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumOnlinePlayersCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumResidentsCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumTownBlocksCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumTownsCalculationEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class ComparatorCaches {
	
	private static LoadingCache<ComparatorType, List<TextComponent>> townCompCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<ComparatorType, List<TextComponent>>() {
				public List<TextComponent> load(ComparatorType compType) throws Exception {
					return gatherTownLines(compType);
				}
			});
	
	private static LoadingCache<ComparatorType, List<TextComponent>> nationCompCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<ComparatorType, List<TextComponent>>() {
				public List<TextComponent> load(ComparatorType compType) throws Exception {
					return gatherNationLines(compType);
				}
			}); 
	
	public static List<TextComponent> getTownListCache(ComparatorType compType) {
		try {
			return townCompCache.get(compType);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	public static List<TextComponent> getNationListCache(ComparatorType compType) {
		try {
			return nationCompCache.get(compType);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	@SuppressWarnings("unchecked")
	private static List<TextComponent> gatherTownLines(ComparatorType compType) {
		List<TextComponent> output = new ArrayList<>();
		List<Town> towns = new ArrayList<>(TownyUniverse.getInstance().getTowns());
		towns.sort((Comparator<? super Town>) compType.getComparator());
		
		for (Town town : towns) {
			TextComponent townName = Component.text(StringMgmt.remUnderscore(town.getName()), NamedTextColor.AQUA)
					.clickEvent(ClickEvent.runCommand("/towny:town spawn " + town + " -ignore"));
				
			String slug = "";
			switch (compType) {
			case BALANCE:
				slug = Colors.AQUA + "(" + TownyEconomyHandler.getFormattedBalance(town.getAccount().getCachedBalance()) + ")";
				break;
			case TOWNBLOCKS:
				slug = Colors.AQUA + "(" + town.getTownBlocks().size() + ")";
				break;
			case RUINED:
				slug = Colors.AQUA + "(" + town.getResidents().size() + ") " + (town.isRuined() ? Translation.of("msg_ruined"):"");
				break;
			case BANKRUPT:
				slug = Colors.AQUA + "(" + town.getResidents().size() + ") " + (town.isBankrupt() ? Translation.of("msg_bankrupt"):"");
				break;
			case ONLINE:
				slug = Colors.AQUA + "(" + TownyAPI.getInstance().getOnlinePlayersInTown(town).size() + ")";
				break;
			case FOUNDED:
				if (town.getRegistered() != 0)
					slug = Colors.AQUA + "(" + TownyFormatter.registeredFormat.format(town.getRegistered()) + ")";
				break;
			default:
				slug = Colors.AQUA + "(" + town.getResidents().size() + ")";
				break;
			}
			townName = townName.append(TownyComponents.miniMessageAndColour(Colors.DARK_GRAY + " - " + slug));
			
			if (town.isOpen())
				townName = townName.append(TownyComponents.miniMessageAndColour(" " + Colors.AQUA + Translation.of("status_title_open")));
			
			String spawnCost = "Free";
			if (TownyEconomyHandler.isActive())
				spawnCost = ChatColor.RESET + Translation.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(town.getSpawnCost()));

			townName = townName.hoverEvent(HoverEvent.showText(TownyComponents.miniMessageAndColour(Colors.GOLD + Translation.of("msg_click_spawn", town) + "\n" + spawnCost)));
			output.add(townName);
		}
		return output;
	}
	
	@SuppressWarnings("unchecked")
	private static List<TextComponent> gatherNationLines(ComparatorType compType) {
		List<TextComponent> output = new ArrayList<>();
		List<Nation> nations = new ArrayList<>(TownyUniverse.getInstance().getNations());

		//Sort nations
		nations.sort((Comparator<? super Nation>) compType.getComparator());
		DisplayedNationsListSortEvent nationListSortEvent = new DisplayedNationsListSortEvent(nations, compType);
		Bukkit.getPluginManager().callEvent(nationListSortEvent);
		nations = nationListSortEvent.getNations();

		for (Nation nation : nations) {
			TextComponent nationName = Component.text(StringMgmt.remUnderscore(nation.getName()), NamedTextColor.AQUA)
					.clickEvent(ClickEvent.runCommand("/towny:nation spawn " + nation + " -ignore"));

			String slug = "";
			switch (compType) {
			case BALANCE:
				slug = TownyEconomyHandler.getFormattedBalance(nation.getAccount().getCachedBalance());
				break;
			case TOWNBLOCKS:
				int rawNumTownsBlocks = nation.getTownBlocks().size();
				NationListDisplayedNumTownBlocksCalculationEvent tbEvent = new NationListDisplayedNumTownBlocksCalculationEvent(nation, rawNumTownsBlocks);
				Bukkit.getPluginManager().callEvent(tbEvent);
				slug = tbEvent.getDisplayedValue() + "";
				break;
			case TOWNS:
				int rawNumTowns = nation.getTowns().size();
				NationListDisplayedNumTownsCalculationEvent tEvent = new NationListDisplayedNumTownsCalculationEvent(nation, rawNumTowns);
				Bukkit.getPluginManager().callEvent(tEvent);
				slug = tEvent.getDisplayedValue() + "";
				break;
			case ONLINE:
				int rawNumOnlinePlayers = TownyAPI.getInstance().getOnlinePlayersInNation(nation).size();
				NationListDisplayedNumOnlinePlayersCalculationEvent opEvent = new NationListDisplayedNumOnlinePlayersCalculationEvent(nation, rawNumOnlinePlayers);
				Bukkit.getPluginManager().callEvent(opEvent);
				slug = opEvent.getDisplayedValue() + "";
				break;
			case FOUNDED:
				if (nation.getRegistered() != 0)
					slug = TownyFormatter.registeredFormat.format(nation.getRegistered());
				break;
			default:
				int rawNumResidents = nation.getResidents().size();
				NationListDisplayedNumResidentsCalculationEvent rEvent = new NationListDisplayedNumResidentsCalculationEvent(nation, rawNumResidents);
				Bukkit.getPluginManager().callEvent(rEvent);
				slug = rEvent.getDisplayedValue() + "";
				break;
			}
			
			nationName = nationName.append(TownyComponents.miniMessageAndColour(Colors.DARK_GRAY + " - " + Colors.AQUA + "(" + slug + ")"));

			if (nation.isOpen())
				nationName = nationName.append(TownyComponents.miniMessageAndColour(" " + Colors.AQUA + Translation.of("status_title_open")));

			String spawnCost = "Free";
			if (TownyEconomyHandler.isActive())
				spawnCost = ChatColor.RESET + Translation.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(nation.getSpawnCost()));
			
			nationName = nationName.hoverEvent(HoverEvent.showText(TownyComponents.miniMessageAndColour(Colors.GOLD + Translation.of("msg_click_spawn", nation) + "\n" + spawnCost)));
			output.add(nationName);
		}
		return output;
	}
}
