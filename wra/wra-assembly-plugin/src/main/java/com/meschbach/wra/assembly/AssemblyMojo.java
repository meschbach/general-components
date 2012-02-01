/*
 * Copyright 2011 Mark Eschbach.
 *
 * $HeadURL$
 * $Id$
 */
package com.meschbach.wra.assembly;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolver;
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
 * @requiresDependencyResolution compile
 * @phase compile
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
     * This will allow us to resolve any missing artifacts.
     *
     * @component
     * @required
     */
    private ArtifactResolver artifactResolver;
    /**
     * The artifact repository to use.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;
    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List<ArtifactRepository> remoteRepositories;
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

    /**
     * The output directory to use for output.
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;
    /**
     * @parameter default-value="wra.js"
     * @required
     */
    private String jsFileName;
    /**
     * @parameter default-value="wra.css"
     * @required
     */
    private String cssFileName;

    private void ensureDirectoryExists(File file ) throws MojoExecutionException {
	if (file.exists()) {
	    if (!file.isDirectory()) {
		throw new MojoExecutionException("Expected '" + file.getAbsolutePath() + "' to be a driectory, but was not");
	    }
	} else {
	    if (!file.mkdirs()) {
		throw new MojoExecutionException("Unable to create directory hierarchy " + file.getAbsolutePath());
	    }
	}
    }
    private void ensureDirectoriesExist( File targetFile ) throws MojoExecutionException{
	File parent = targetFile.getParentFile();
	ensureDirectoryExists(parent);
    }
    public void execute() throws MojoExecutionException, MojoFailureException {
	byte buffer[] = new byte[1024];
	try {
	    OutputStream cssOut = null, jsOut = null;
	    try {
		/*
		 * Ensure our output directory exists
		 */
		ensureDirectoryExists(outputDirectory);
		/*
		 * Open our target files
		 */
		File cssFile = new File(outputDirectory, cssFileName);
		ensureDirectoriesExist(cssFile);
		cssOut = new BufferedOutputStream(new FileOutputStream(cssFile));
		File jsFile = new File(outputDirectory, jsFileName);
		ensureDirectoriesExist(jsFile);
		jsOut = new BufferedOutputStream(new FileOutputStream(jsFile));
		/*
		 * Figure out the nodes we need to include
		 */
		DependencyNode rootNode;

		rootNode = dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFactory, artifactMetadataSource, null, artifactCollector);
		List<DependencyNode> nodes = new LinkedList<DependencyNode>();
		accumulate(nodes, rootNode);
		resolveDependencies(nodes);
		/*
		 * For each node, extract ou the JS and CSS
		 */
		Iterator<DependencyNode> nit = nodes.iterator();
		while (nit.hasNext()) {
		    DependencyNode n = nit.next();
		    File target = n.getArtifact().getFile();
		    if( target == null){
			throw new MojoExecutionException("Unable to locate "+n+ " because the attached artifact is null");
		    }
		    //So we know which artifacts we need
		    getLog().info("Using dependency " + n.getArtifact().toString() + " from " + target.getAbsolutePath());
		    /*
		     * Process the resource files
		     */
		    InputStream is = new BufferedInputStream(new FileInputStream(target));
		    try {
			ZipInputStream zis = new ZipInputStream(is);
			ZipEntry ze ;
			while( ( ze= zis.getNextEntry()) != null){
			    String name = ze.getName();
			    OutputStream outputStream;
			    if(name.endsWith(".js")){
				getLog().info("Procesing entry "+name+" as JS file");
				outputStream = jsOut;
			    }else if(name.endsWith(".css")){
				getLog().info("Procesing entry "+name+" as CSS file");
				outputStream = cssOut;
			    }else{
				getLog().info("Ignoring entry "+name+" becuase I don't know how to process it.");
				//Ignore this entry
				continue;
			    }
			    int i;
			    while( (i = zis.read(buffer)) != -1){
				outputStream.write(buffer, 0 , i);
			    }
			}
		    }finally{
			is.close();
		    }
		}

	    } catch (DependencyTreeBuilderException dtbe) {
		throw new MojoFailureException("Failed to figure out dependency tree", dtbe);
	    } finally {
		if (cssOut != null) {
		    cssOut.close();
		}
		if (jsOut != null) {
		    jsOut.close();
		}
	    }
	} catch (IOException ioe) {
	    throw new MojoFailureException("Generic IO exception", ioe);
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

    public void resolveDependencies(List<DependencyNode> nodes) throws MojoExecutionException{
	/*
	 * Convert our dependency nodes into artifacts
	 */
	TreeSet<Artifact> artifacts = new TreeSet<Artifact>();
	Iterator<DependencyNode> dit =nodes.iterator();
	while(dit.hasNext()){
	    DependencyNode dn = dit.next();
	    artifacts.add(dn.getArtifact());
	}
	/*
	 * Resolve our artifacts
	 */
	try {
	    artifactResolver.resolveTransitively(artifacts, null, remoteRepositories, localRepository, artifactMetadataSource);
	} catch (Exception e) {
	    throw new MojoExecutionException("Unable to resolve dependencies: "+artifacts, e);
	}
    }
}
