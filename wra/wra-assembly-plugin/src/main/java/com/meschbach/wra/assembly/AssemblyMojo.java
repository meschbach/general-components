/*
 * Copyright 2011 Mark Eschbach.
 *
 * $HeadURL$
 * $Id$
 */
package com.meschbach.wra.assembly;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

/**
 *
 * @author "Mark Eschbach" (meschbach@gmail.com)
 * @goal wra-assemble
 * @requiresDependencyResolution test
 */
public class AssemblyMojo extends AbstractMojo {

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;
    /**
     * The artifact repository to use.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;
    /**
     * The artifact factory to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;
    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;
    /**
     * The artifact collector to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactCollector artifactCollector;
    /**
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    public void execute() throws MojoExecutionException, MojoFailureException {
	try {
	    DependencyNode rootNode;

	    rootNode = dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFactory, artifactMetadataSource, null, artifactCollector);
	    List<DependencyNode> nodes = new LinkedList<DependencyNode>();
	    accumulate(nodes, rootNode);
	    Iterator<DependencyNode> nit = nodes.iterator();
	    while(nit.hasNext()){
		DependencyNode n = nit.next();
		getLog().info("Using dependency "+n.getArtifact().toString());
	    }
	} catch (DependencyTreeBuilderException dtbe) {
	    throw new MojoFailureException("Failed to figure out dependency tree", dtbe);
	}
    }

    public void accumulate(List<DependencyNode> usableNodes, DependencyNode current) {
	/*
	 * Attach the children
	 */
	if (current.hasChildren()) {
	    Iterator<DependencyNode> nodes = current.getChildren().iterator();
	    while (nodes.hasNext()) {
		DependencyNode node = nodes.next();
		accumulate(usableNodes, node);
	    }
	}
	/*
	 * Is this node a WRA node?
	 */
	Artifact me = current.getArtifact();
	if ("wra".equals(me.getType())) {
	    /*
	     * Do we have another articat with the same group and artifact?
	     */
	    boolean foundDuplicateError = false, attachDep = true;
	    Iterator<DependencyNode> deps = usableNodes.iterator();
	    while (!foundDuplicateError && deps.hasNext()) {
		DependencyNode dn = deps.next();
		Artifact test = dn.getArtifact();
		if (me.getGroupId().equals(test.getGroupId())
			&& me.getArtifactId().equals(test.getArtifactId())) {
		    if (me.getVersion().equals(test.getVersion())) {
			//do nothing, we already have this node
			attachDep = false;
		    } else {
			getLog().warn("Found multiple versions of " + test.toString());
			foundDuplicateError = true;
			attachDep = false;
		    }
		}
	    }
	    /*
	     * Attach our node if we don't have duplicates
	     */
	    if (attachDep) {
		usableNodes.add(current);
	    }
	}
    }
}
