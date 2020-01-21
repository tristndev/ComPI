package factories.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.stream.Collectors;

import blogbuilder.ConfigSingle;
import blogbuilder.World;
import elements.Factor;
import elements.LogVar;
import elements.RandVar;
import factories.ElementFactory;

public class RandomSampleFactory implements ElementFactory {

	/**
	 * Creates logvars with following strategy: Create logVarCount logVars named
	 * according to the world's naming function.
	 * 
	 * If called more than once (e.g. after incrementing the logVarCount), adds 
	 * missing logvars.
	 */
	@Override
	public void insertLogVars(World w) {
		while(w.getLogVars().size() < w.getSpecContainer().getLogVarCount()) {
			w.addLogVar(new LogVar(w.getNextLogVarIndex()));
		}
	}

	/**
	 * Creates randvars with the following strategy: Create randVarCount randvars.
	 * <br>
	 * For each randvar:
	 * <ul>
	 * <li>pick a random argCount between 0 and maxRandVarArgs.</li>
	 * <li>Then shuffle the logVars and pick the argCount first elements as
	 * arguments for the RandVar.</li>
	 * </ul>
	 * 
	 * If called more than once (e.g. after incrementing the randVarCount), adds 
	 * missing randVars.
	 */
	@Override
	public void insertRandVars(World w) {
		while(w.getRandVars().size() < w.getSpecContainer().getRandVarCount()) {
			ArrayList<LogVar> args = this.selectLogVarArgsForRandVar(w);
			w.addRandVar(new RandVar(w.getNextRandVarIndex(), args));
		}
	}
	
	/**
	 * <ul>
	 * <li>pick a random argCount between 0 and maxRandVarArgs.</li>
	 * <li>Then shuffle the logVars and pick the argCount first elements as
	 * arguments for the RandVar.</li>
	 * </ul>
	 * @param w to pick the logvars from
	 * @return ArrayList of LogVars
	 */
	public ArrayList<LogVar> selectLogVarArgsForRandVar(World w) {
		Random r = ConfigSingle.getInstance().getRandom();
		int argCount = r.nextInt(Math.min(w.getSpecContainer().getMaxRandVarArgs(), w.getLogVars().size()) + 1);
		ArrayList<LogVar> args = new ArrayList<LogVar>();

		ArrayList<LogVar> shuffleList = new ArrayList<LogVar>(w.getLogVars());
		Collections.shuffle(shuffleList);
		args.addAll(shuffleList.subList(0, argCount));
		return args;
	}

	/**
	 * Creates factors with the following strategy: Create factorCount factors with
	 * a randomly picked argCount between 1 and factorArgCount. Fill args with
	 * following priority:
	 * <ol>
	 * <li>randVars w/o occurrence</li>
	 * <li>randVars not yet at maxOccurence</li>
	 * <li>randVars at maxOccurence</li>
	 * <ol>
	 * 
	 * If called more than once (e.g. after incrementing the factorCount), adds 
	 * missing factors.
	 */
	@Override
	public void insertFactors(World w) {
		Random r = ConfigSingle.getInstance().getRandom();
		while(w.getFactors().size() < w.getSpecContainer().getFactorCount()) {
			int argCount = r.nextInt(w.getSpecContainer().getFactorArgCount()) + 1; // interval: [1, factorArgCount]
			ArrayList<RandVar> args = new ArrayList<RandVar>();
			
			if (argCount > 1) {
				args = this.collectParfactorRandVars(argCount, w);
			} else {
				args.add(this.chooseFactorRandVar(w));
			}
				
			if (argCount > args.size() && ConfigSingle.getInstance().verbose) {
				System.err.printf(" > Warning: Just found %d instead of %d arguments for factor #%d.\n", args.size(), argCount, w.getNextFactorIndex());
			}
			w.addFactor(new Factor(w.getNextFactorIndex(), args));
		}
		
		if (!w.checkAllRVMentioned()) {
			System.err.println("   Initial RandVar-Factor allocation left RandVars not-mentioned.\n   Forcing all RVs to be mentioned...");
			this.forceAllRVToMentioned(w);
		}
	}


	private void forceAllRVToMentioned(World w) {
		ArrayList<RandVar> nonMentionedRVs = w.searchNonMentionedRV();
		for (RandVar rv: nonMentionedRVs) {
			// Pick random Factor
			Factor fac = w.getFactors().get(ConfigSingle.getInstance().getRandom().nextInt(w.getFactors().size()));
			
			fac.augmentWithRandVar(rv);
		}		
	}

	/**
	 * Chooses a random RandVar out of all randvars where hasFactor == false.
	 * 
	 * If no such candidate exists, randomly take RandVar where hasFactor already is true.
	 * 
	 * @param w world to sample the randvars from.
	 * @return RandVar object
	 */
	private RandVar chooseFactorRandVar(World w) {
		ArrayList<RandVar> candidates = new ArrayList<RandVar> ( w.getRandVars().stream().filter(x -> !x.hasFactor()).collect(Collectors.toList()));
		
		// No candidates with hasFactor == false?
		if (candidates.size() == 0) {
			candidates = w.getRandVars();
		}
			
		int rdnIndex = ConfigSingle.getInstance().getRandom().nextInt(candidates.size());
		return candidates.get(rdnIndex);
	}
	
	/**
	 * Collect argCount RandVars with the following priority:
	 * <ol>
	 * <li>randVars w/o occurence</li>
	 * <li>randVars not yet at maxOccurence</li>
	 * <li>randVars at maxOccurence</li>
	 * </ol>
	 * 
	 * The following restrictions are made:
	 * A RandVar can occur only (at most) once with each other RandVar.
	 * 
	 * @param argCount number of args (randVars) to be collected.
	 * @return ArrayList of collected RandVars
	 */
	private ArrayList<RandVar> collectParfactorRandVars(int argCount, World w) {
		// 1. Collect all candidates in order
		LinkedHashSet<RandVar> cand = new LinkedHashSet<RandVar>();
		
		ArrayList<RandVar> temp = new ArrayList<RandVar>(w.getNonMentionedRandVars());
		Collections.shuffle(temp);
		cand.addAll(temp);
		temp = new ArrayList<RandVar>(w.getNonMaxedRandVars());
		cand.addAll(temp);
		temp = new ArrayList<RandVar>(w.getMaxedRandVars());
		cand.addAll(temp);
		
		// 2. While not enough found and not empty: add to collected
		HashSet<RandVar> collected = new HashSet<RandVar>();
		HashSet<RandVar> connected = new HashSet<RandVar>();
		int missing = argCount;
		Iterator<RandVar> it = cand.iterator();
		while (missing > 0 & it.hasNext()) {
			RandVar next = it.next();
			if (!collected.contains(next) && !connected.contains(next)) {
				collected.add(next);
				connected.addAll(next.getConnectedRandVars());
				missing--;
			}
		}
		
		return new ArrayList<RandVar>(collected);
	}
	
	/**
	 * Helper function that prints a HashSet of RandVars.
	 * Used for debugging (the collected and connected HashSets in the previous methods).
	 * @param name of the hashset to be printed in the console.
	 * @param hashset to be printed in the console.
	 */
	private void printHashSet(String name, HashSet<RandVar> hashset) {
		System.out.println(name + ": " + hashset.stream().map(x -> x.constructName()).collect(Collectors.toList()));

	}

	@Override
	public ElementFactory getBaseFactory() {
		// Was used as base factory = return this.
		return this;
	}

	@Override
	public void initLogVars(World w) {
		// No init needed
	}

	@Override
	public void initRandVars(World w) {
		// No init needed
	}

	@Override
	public void initFactors(World w) {
		// No init needed
	}

	
}
