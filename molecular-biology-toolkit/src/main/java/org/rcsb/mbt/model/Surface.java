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
 * Created on 2008/12/22
 *
 */ 
package org.rcsb.mbt.model;

import javax.vecmath.Color4f;
import javax.vecmath.Vector3f;

import org.rcsb.mbt.model.StructureComponentRegistry.ComponentType;
import org.rcsb.mbt.surface.datastructure.TriangulatedSurface;


public class Surface extends StructureComponent {
	private Chain chain;
	private TriangulatedSurface triangulatedSurface;
	private boolean backfaceRendered = false;
	private boolean meshSurface = false;
	private boolean dotSurface = false;
	private Color4f[] colors;
	private Vector3f alignment;

	public Surface(final Chain chain, final Structure structure) {
		super();
		super.structure = structure;
		this.chain = chain;
	}

	public void copy(final StructureComponent structureComponent) {
		// TODO copy other members
	}
	
	public ComponentType getStructureComponentType() {
		return ComponentType.SURFACE;
	}
	
	public TriangulatedSurface getTriangulatedSurface() {
		return triangulatedSurface;
	}
	
	public void setTriangulatedSurface(TriangulatedSurface triangulatedSurface) {
		this.triangulatedSurface = triangulatedSurface;
	}

	public Color4f[] getColors() {
		return colors;
	}

	public void setColors(Color4f[] colors) {
		this.colors = colors;
	}

	public Chain getChain() {
		return chain;
	}
		
	public boolean setBackfaceRendered(boolean backFaceRendered) {
		return this.backfaceRendered = backFaceRendered;
	}
	
	public boolean isBackfaceRendered() {
		return backfaceRendered;
	}
	
	public boolean isTransparent() {
		return colors[0].w < 1.0f;
	}

	/**
	 * @param meshSurface
	 */
	public void setMeshSurface(boolean meshSurface) {
		this.meshSurface = meshSurface;
	}

	/**
	 * @return the meshSurface
	 */
	public boolean isMeshSurface() {
		return meshSurface;
	}

	/**
	 * @param drawDots the drawDots to set
	 */
	public void setDotSurface(boolean dotSurface) {
		this.dotSurface = dotSurface;
	}

	/**
	 * @return the dotSurface
	 */
	public boolean isDotSurface() {
		return dotSurface;
	}
	
	public Vector3f getAlignment() {
		return alignment;
	}
	
	public void setAlignment(Vector3f alignment) {
		this.alignment = alignment;
	}
}