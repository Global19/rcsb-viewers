/*
 * BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence. This should
 * be distributed with the code. If you do not have a copy,
 * see:
 *
 * http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors. These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 * http://www.biojava.org/
 *
 * This code was contributed from the Molecular Biology Toolkit
 * (MBT) project at the University of California San Diego.
 *
 * Please reference J.L. Moreland, A.Gramada, O.V. Buzko, Qing
 * Zhang and P.E. Bourne 2005 The Molecular Biology Toolkit (MBT):
 * A Modular Platform for Developing Molecular Visualization
 * Applications. BMC Bioinformatics, 6:21.
 *
 * The MBT project was funded as part of the National Institutes
 * of Health PPG grant number 1-P01-GM63208 and its National
 * Institute of General Medical Sciences (NIGMS) division. Ongoing
 * development for the MBT project is managed by the RCSB
 * Protein Data Bank(http://www.pdb.org) and supported by funds
 * from the National Science Foundation (NSF), the National
 * Institute of General Medical Sciences (NIGMS), the Office of
 * Science, Department of Energy (DOE), the National Library of
 * Medicine (NLM), the National Cancer Institute (NCI), the
 * National Center for Research Resources (NCRR), the National
 * Institute of Biomedical Imaging and Bioengineering (NIBIB),
 * the National Institute of Neurological Disorders and Stroke
 * (NINDS), and the National Institute of Diabetes and Digestive
 * and Kidney Diseases (NIDDK).
 *
 * Created on 2011/11/08
 *
 */ 
package org.rcsb.uiApp.controllers.doc;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jcolorbrewer.ColorBrewer;
import org.rcsb.mbt.model.Atom;
import org.rcsb.mbt.model.Chain;
import org.rcsb.mbt.model.Residue;
import org.rcsb.mbt.model.Structure;
import org.rcsb.mbt.model.StructureMap;
import org.rcsb.mbt.model.Surface;
import org.rcsb.mbt.model.Residue.Classification;
import org.rcsb.mbt.model.StructureMap.BiologicUnitTransforms;
import org.rcsb.mbt.model.StructureMap.BiologicUnitTransforms.BiologicalUnitGenerationMapByChain;
import org.rcsb.mbt.model.attributes.AtomRadiusRegistry;
import org.rcsb.mbt.model.attributes.IAtomRadius;
import org.rcsb.mbt.model.attributes.SurfaceColorUpdater;
import org.rcsb.mbt.model.geometry.ModelTransformationList;
import org.rcsb.mbt.model.util.Status;
import org.rcsb.mbt.surface.BindingSiteSurfaceOrienter;
import org.rcsb.mbt.surface.EdtMolecularSurface;
import org.rcsb.mbt.surface.SurfaceCalculator;
import org.rcsb.mbt.surface.SurfaceScorer;
import org.rcsb.mbt.surface.datastructure.Sphere;
import org.rcsb.mbt.surface.datastructure.TriangulatedSurface;
import org.rcsb.mbt.surface.gamer.ImproveMesh;
import org.rcsb.uiApp.controllers.app.AppBase;
import org.rcsb.uiApp.controllers.app.ProgressPanelController;
import org.rcsb.uiApp.controllers.update.UpdateEvent;


/**
 * Actions create and start this thread to create the requested surface.
 * 
 * @author Peter Rose
 *
 */
//public class SurfaceThread extends Thread {
	public class SurfaceThread {
	private static float PROBE_RADIUS = 1.0f;
	private static float TEST_PROBE_RADIUS = 1.5f;
	private static float DISTANCE_THRESHOLD = 6.5f;
//	private SurfaceScorer scorer;
//	private ArrayList<SurfaceScorer> multiScorer;
	private boolean drawLines = false;
	private boolean drawDots = false;
	
	public void createSurface() {
		Runtime runtime = Runtime.getRuntime();
//		System.out.println("allocated memory: " + runtime.totalMemory() / 1024); 
//		System.out.println("free memory: " + runtime.freeMemory() / 1024); 
		IAtomRadius registry = AtomRadiusRegistry.get("By CPK");
		
		int numOfTriangles = 0;
		Structure structure = AppBase.sgetModel().getStructures().get(0);
		StructureMap smap = structure.getStructureMap();	
		List<Chain> polymerChains = getPolymerChains();
		float resolution = calcResolution(polymerChains);	
		if (drawLines) {
			resolution *= 0.75f;
		}
		long t0 = System.nanoTime();
		// create progress bar
//		multiScorer = new ArrayList<SurfaceScorer>();
		ProgressPanelController.StartProgress(AppBase.sgetActiveFrame());
		Status.progress(0, "Creating surfaces");
		int numSpheres = 0;
		for (Chain c: polymerChains) {
			List<Sphere> spheres = new ArrayList<Sphere>();
			Vector<Residue> residues = c.getResidues();
			
			// TODO How to deal with non-standard residues in a polymer?
			for (Residue r: residues) {
				if (r.getClassification().equals(Classification.AMINO_ACID) ||
						r.getClassification().equals(Classification.NUCLEIC_ACID)) {
					Vector<Atom> atoms = r.getAtoms();
					for (Atom a: atoms) {
						double[] coord = a.coordinate;
						Point3f location = new Point3f((float)coord[0], (float)coord[1], (float)coord[2]);
						//System.out.println("keke " + location);
						float radius = registry.getAtomRadius(a);
						// enlarge spheres by 10% to avoid that helices touch the surface
						spheres.add(new Sphere(location, radius *1.1f, a));
						numSpheres++;
					}
				}
			}
			// TODO should there be a size cutoff? I.e. min 24 residues?
			if (spheres.size() == 0) {
				continue;
			};
			
			// calculate molecular surface
			SurfaceCalculator s = new EdtMolecularSurface(spheres, PROBE_RADIUS, resolution, 2.4f);

			TriangulatedSurface ts = s.getSurface();
//			System.out.println("used memory: " +  (Runtime.getRuntime().totalMemory() - runtime.freeMemory()));
			s = null; // this is a very large object that should be garbage collected ASAP
			//System.out.println("used memory: " +  (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/ 1024);
			//System.out.println("allocated memory: " + runtime.totalMemory() / 1024); 
			//System.out.println("free memory2: " + runtime.freeMemory() / 1024);
			//
			// smooth surface
			ts.laplaciansmooth(1);
			Surface surface = new Surface(c, structure);
			surface.setTriangulatedSurface(ts);

			surface.setMeshSurface(drawLines);
			surface.setDotSurface(drawDots);
			
			numOfTriangles += surface.getTriangulatedSurface().getFaces().size();

			// set default surface color
			SurfaceColorUpdater.setPaletteColor(surface, ColorBrewer.BrBG, polymerChains.size(), smap.getSurfaceCount());

			smap.addSurface(surface);
			
			// update progress bar
			Status.progress(((int)(100* smap.getSurfaceCount()/(float)polymerChains.size())), "Creating surfaces");
			
			AppBase.sgetUpdateController().fireUpdateViewEvent(UpdateEvent.Action.SURFACE_ADDED, surface); // has no effect
	//		multiScorer.add( new SurfaceScorer(ts));
		}
		ProgressPanelController.EndProgress();
		long t5 = System.nanoTime();
		System.out.println("Surface calculation: " + (t5-t0)/1000000 + " ms");
		AppBase.sgetUpdateController().fireUpdateViewEvent(UpdateEvent.Action.VIEW_UPDATE);
		System.out.println(numOfTriangles);
		//System.out.println(multiScorer.size());
	}
	
	//test create Surface for C Alpha molecules
	public void createCAlphaSurface() {
//		Runtime runtime = Runtime.getRuntime();
		int i = 0;
		Map<String, Float> scanMap = new HashMap<String, Float>();
		String atomName;
		float aRadius = 0.0f;
		float largestRadius = 0.0f;
		double currNorm = 0.0f;
		double currSq = 0.0f;
		double totalNorm = 0.0f;
		double totalSq = 0.0f;
		
		/*
		 * Read residue radius for multiscale representation
		 */		
		Scanner scanner = new Scanner(SurfaceThread.class.getResourceAsStream("/CAlphaRadiusMed.txt"));
		
		while(scanner.hasNext()){
			atomName = scanner.next();
			
			/* skips over the mean radius, put this line after if statement 
			 * if you only want to use mean radius
			 */
			scanner.nextFloat();
			
			if(scanner.hasNextFloat() ){
				aRadius = scanner.nextFloat();
			}
			scanMap.put(atomName, aRadius);
		}
		scanner.close();
		
		Structure structure = AppBase.sgetModel().getStructures().get(0);
		StructureMap smap = structure.getStructureMap();	
		List<Chain> polymerChains = getPolymerChains();
		float resolution = calcResolutionCAlpha(polymerChains);	
		if (drawLines) {
			resolution *= 0.75f;
		}
		
		long t0 = System.nanoTime();
		// create progress bar
		ProgressPanelController.StartProgress(AppBase.sgetActiveFrame());
		Status.progress(0, "Creating surfaces");
		
		for (Chain c: polymerChains) {
			largestRadius = 0.0f;
			List<Sphere> spheres = new ArrayList<Sphere>();
			Vector<Residue> residues = c.getResidues();
			
			// TODO How to deal with non-standard residues in a polymer?
			for (Residue r: residues) {
				if (r.getClassification().equals(Classification.AMINO_ACID)) {
					Atom cAlphaAtom = r.getAlphaAtom();
					double[] coord = cAlphaAtom.coordinate;
					Float cAlphaRadius  = 0.0f;
					cAlphaRadius = scanMap.get(r.getCompoundCode());
					if (cAlphaRadius == null) {
						cAlphaRadius = 5.0f;
					} 
					if(cAlphaRadius > largestRadius) {
						largestRadius = cAlphaRadius;
					}

					Point3f location = new Point3f((float)coord[0], (float)coord[1], (float)coord[2]);
					spheres.add(new Sphere(location, cAlphaRadius, cAlphaAtom));
				}
			}
			// TODO should there be a size cutoff? I.e. min 24 residues?
			if (spheres.size() == 0) {
				continue;
			};
	
			// calculate molecular surface
			SurfaceCalculator s = new EdtMolecularSurface(spheres, TEST_PROBE_RADIUS, resolution, largestRadius+1.5f); 
//			MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
//			System.out.println("heap memory used: " + heapMemoryUsage.getUsed()/(1000*1000));
//			System.out.println("used memory: " +  (Runtime.getRuntime().totalMemory() - runtime.freeMemory())/(1000*1000));
			TriangulatedSurface ts = s.getSurface();
//			System.out.println("used memory: " +  (Runtime.getRuntime().totalMemory() - runtime.freeMemory()));
//			System.out.println("Triangles: " + ts.getFaces().size());
			
			s = null; // this is a very large object that should be garbage collected ASAP


			// smooth surface
//			System.out.println("Surface area before smoothing: " + ts.getSurfaceArea());
			ts.laplaciansmooth(1);
			Surface surface = new Surface(c, structure);
			surface.setTriangulatedSurface(ts);
			surface.setMeshSurface(drawLines);
			surface.setDotSurface(drawDots);
			

			SurfaceColorUpdater.setPaletteColor(surface, ColorBrewer.Spectral, polymerChains.size(), smap.getSurfaceCount());

			smap.addSurface(surface);
			
			// update progress bar
			Status.progress(((int)(100* smap.getSurfaceCount()/(float)polymerChains.size())), "Creating surfaces");
			
			AppBase.sgetUpdateController().fireUpdateViewEvent(UpdateEvent.Action.SURFACE_ADDED, surface); // has no effect
	//		AppBase.sgetUpdateController().fireUpdateViewEvent(UpdateEvent.Action.VIEW_UPDATE); 
			

			//Scoring methods: To use, create original surface first then create CAlpha surface
			//currNorm = multiScorer.get(i).scoringSurfaceA(ts);
			//currSq = multiScorer.get(i).scoringSurfaceASquared(ts);
			//System.out.println("Surface Score Reg: " + currNorm);
			//System.out.println("Surface Score Squared: " + currSq);

			totalNorm += currNorm;
			totalSq += currSq;
			i++;
		}
//		System.out.println("Total Surface Score Reg: " + totalNorm/i);
//		System.out.println("Total Surface Score Sq: " + totalSq/i);
		ProgressPanelController.EndProgress();
		long t5 = System.nanoTime();
		System.out.println("Surface calculation: " + (t5-t0)/1000000 + " ms");
		
		AppBase.sgetUpdateController().fireUpdateViewEvent(UpdateEvent.Action.VIEW_UPDATE); 
	}

	public void createBindingSiteSurface(Residue[] ligands, int surfaceRepresentation, float thresholdDistance) {
		IAtomRadius registry = AtomRadiusRegistry.get("By CPK");
		
		Structure structure = AppBase.sgetModel().getStructures().get(0);
		StructureMap smap = structure.getStructureMap();	
		List<Chain> polymerChains = getPolymerChains();
//		System.out.println("begin polymerchains: " + polymerChains.size() + " index: " + smap.getSurfaceCount());
		float resolution = 0.4f;
		
		long t0 = System.nanoTime();
		// create progress bar
		ProgressPanelController.StartProgress(AppBase.sgetActiveFrame());
		Status.progress(0, "Creating surfaces");
		
		for (Chain c: polymerChains) {
			List<Sphere> spheres = new ArrayList<Sphere>();
			Vector<Residue> residues = c.getResidues();
			
			// TODO How to deal with non-standard residues in a polymer?
			for (Residue r: residues) {
				if (r.getClassification().equals(Classification.AMINO_ACID) ||
						r.getClassification().equals(Classification.NUCLEIC_ACID)) {
					Vector<Atom> atoms = r.getAtoms();
					for (Atom a: atoms) {
						double[] coord = a.coordinate;
						Point3f location = new Point3f((float)coord[0], (float)coord[1], (float)coord[2]);
						float radius = registry.getAtomRadius(a);
						// enlarge spheres by 10% to avoid that helices touch the surface
						spheres.add(new Sphere(location, radius, a));
					}
				}
			}
			// TODO should there be a size cutoff? I.e. min 24 residues?
			if (spheres.size() == 0) {
				continue;
			};
			
			List<Sphere> ligandSpheres = new ArrayList<Sphere>();
			for (Residue r: ligands) {
				Vector<Atom> atoms = r.getAtoms();
				for (Atom a: atoms) {
					double[] coord = a.coordinate;
					Point3f location = new Point3f((float)coord[0], (float)coord[1], (float)coord[2]);
					float radius = registry.getAtomRadius(a);
					// enlarge spheres by 10% to avoid that helices touch the surface
					ligandSpheres.add(new Sphere(location, radius, a));
				}
			}
	
			// calculate binding site molecular surface
			SurfaceCalculator s = new EdtMolecularSurface(spheres, ligandSpheres, PROBE_RADIUS, thresholdDistance, resolution);
			TriangulatedSurface ts = s.getSurface();
			s = null; // this is a very large object that should be garbage collected ASAP
			if (ts.getVertices().size() == 0) {
				continue;
			}

			// smooth surface;
//			System.out.println("Surface area before smoothing: " + ts.getSurfaceArea());
			ts.edgesmooth(5);
			ts.laplaciansmooth(3);
//			System.out.println("Surface area after smoothing:  " + ts.getSurfaceArea());
			Surface surface = new Surface(c, structure);
			surface.setTriangulatedSurface(ts);
			surface.setBackfaceRendered(true);
			if (surfaceRepresentation == 1) {
				surface.setMeshSurface(true);
			} else if (surfaceRepresentation == 2) {
				surface.setDotSurface(true);
			}
			
			// set default surface color
			SurfaceColorUpdater.setHydrophobicSurfaceColor(surface);

			smap.addSurface(surface);
			
			// update progress bar
			Status.progress(((int)(100* smap.getSurfaceCount()/(float)polymerChains.size())), "Creating surfaces"); 
		}
		ProgressPanelController.EndProgress();
		long t5 = System.nanoTime();
		System.out.println("Surface calculation: " + (t5-t0)/1000000 + " ms");
	}

	/**
	 * @param chains
	 * @return
	 */
	private List<Chain> getPolymerChains() {
		Structure structure = AppBase.sgetModel().getStructures().get(0);
		StructureMap smap = structure.getStructureMap();
		Vector<Chain> chains = smap.getChains();
		List<Chain> polymerChains = new ArrayList<Chain>();
		for (Chain c: chains) {
			if (c.getClassification().equals(Residue.Classification.AMINO_ACID) ||
					c.getClassification().equals(Residue.Classification.NUCLEIC_ACID)) {
				polymerChains.add(c);
			}
		}
		return polymerChains;
	} 
	
	private float calcResolution(List<Chain> polymerChains) {		
		int residueCount = getResidueCount(polymerChains);
		int symOps = getSymmetryOperationCount(polymerChains);
		float resolution = 0.4f - 0.00005f * residueCount - 0.000005f * residueCount*symOps;
		System.out.println("Surface resolution: " + resolution + ", residues: " + residueCount + ", symmetry operations: " + symOps);
		// clamp lowest resolution
		resolution = Math.max(resolution, 0.1f);
		return resolution;
	}
	
	private float calcResolutionCAlpha(List<Chain> polymerChains) {		
		int residueCount = getResidueCount(polymerChains);
		int symOps = getSymmetryOperationCount(polymerChains);
		// empirical formula to adjust surface resolution based on size of the biological assembly
		float resolution = (float) (0.3 - 0.15*Math.log10(residueCount*symOps/60000.0));
		// clamp lowest resolution
		resolution = Math.max(resolution, 0.05f);
		System.out.println("Surface resolution: " + resolution + ", residues: " + residueCount + ", symmetry operations: " + symOps);
		return resolution;
	}

	/**
	 * @param polymerChains
	 * @return
	 */
	private int getResidueCount(List<Chain> polymerChains) {
		int sphereCount = 0;
		for (Chain c: polymerChains) {
			for (Residue r: c.getResidues()) {
				if (r.getClassification().equals(Classification.AMINO_ACID) ||
						r.getClassification().equals(Classification.NUCLEIC_ACID)) {
					sphereCount++;
				}
			}
		}
		return sphereCount;
	}
	
	private int getSymmetryOperationCount(List<Chain> polymerChains) {
		Structure structure = AppBase.sgetModel().getStructures().get(0);
		
		// TODO
		// this creates a dependency on other packages! How can this be avoided?
		final String showAsymmetricUnitOnly = AppBase.getApp().properties.getProperty("show_asymmetric_unit_only");
		if (showAsymmetricUnitOnly != null && showAsymmetricUnitOnly.equals("true")) {
			return 1;
		}
		
		if ( ! structure.getStructureMap().hasBiologicUnitTransforms()) {
			return 1;
		}

		BiologicUnitTransforms t = structure.getStructureMap().getBiologicUnitTransforms();
		BiologicalUnitGenerationMapByChain map = t.getBiologicalUnitGenerationMatricesByChain();
		
		if (map == null) {
			return 1;
		}
		
		int symOps = 0;
		
		for (Chain c: polymerChains) {
			ModelTransformationList list = map.get(c.getChainId());
			if (list != null) {
				symOps += list.size();
			}
		}

		return symOps;
	}
	
	
	
	public void run()
	{
		ProgressPanelController.StartProgress();

		if (AppBase.sgetModel().hasStructures())
            createSurface();
		ProgressPanelController.EndProgress();
		if (!AppBase.sgetModel().hasStructures())
			JOptionPane.showMessageDialog(null, "Structure not found: Cannot create surface"); 
	}

	public void handleUpdateEvent(UpdateEvent evt) {
		// TODO Auto-generated method stub
		
	}
};
