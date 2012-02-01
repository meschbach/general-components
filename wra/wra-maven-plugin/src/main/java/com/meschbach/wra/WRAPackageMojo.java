/*
 * Copyright 2011 Mark Eschbach.
 *
 * $HeadURL$
 * $Id$
 */
package com.meschbach.wra;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 *
 * @author "Mark Eschbach" (meschbach@gmail.com)
 * @goal wra-package
 * @phase package
 * @requiresProject
 * @threadSafe
 * @requiresDependencyResolution runtime
 */
public class WRAPackageMojo extends AbstractMojo {
    /**
     * Build the project
     *
     * @parater expresion="${project}"
     * @required
     */
    MavenProject project;
    /**
     * @component
     */
    MavenProjectHelper mavenProjectHelper;
    /**
     * @parameter expression="${project.artifact}" default-value="${project.artifact}"
     */
    protected Artifact result;
    /**
     * @parameter expression="${wra.source}" default-value="${project.basedir}/src/main/wra"
     */
    protected String wraSourceDirectory;
    /**
     * Name of the generated JAR.
     *
     * @parameter alias="wraName" expression="${wra.finalName}" default-value="${project.build.finalName}.zip"
     * @required
     */
    protected String finalName;
    /**
     * Directory containing the generated JAR.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
	assert result != null : "Maven artifact is null";
	/*
	 * Grab the current directory
	 */
	File inputDirectory = new File(wraSourceDirectory);
	if (!inputDirectory.isDirectory()) {
	    throw new MojoFailureException("WRA target '" + inputDirectory.getAbsolutePath() + "' is not a directory");
	}
	/*
	 * Figure out the output file
	 */
	if (!outputDirectory.mkdirs()) {
	    throw new MojoExecutionException("Unable to create output directory '" + outputDirectory.getAbsolutePath() + "'.");
	}
	File targetFile = new File(outputDirectory, finalName);
	/*
	 * Create our zip file
	 */
	try {
	    FileOutputStream fos = new FileOutputStream(targetFile);
	    BufferedOutputStream bos = new BufferedOutputStream(fos);
	    ZipOutputStream zos = new ZipOutputStream(bos);
	    zos.setLevel(9);
	    try {
		/*
		 * Attach the directory
		 */
		processDir(zos, inputDirectory);
	    } finally {
		zos.close();
	    }
	} catch (IOException ioe) {
	    throw new MojoFailureException("Unable to create file '" + targetFile.getAbsolutePath() + "'", ioe);
	}
	/*
	 * Set our artifact as the default
	 */
	result.setFile(targetFile);
	/*
	 * Attach the artifact
	 */
    }

    protected void processDir(ZipOutputStream zos, File dir) throws IOException {
	Iterator<File> fit = Arrays.asList(dir.listFiles()).iterator();
	while (fit.hasNext()) {
	    File i = fit.next();
	    if (i.isDirectory()) {
		processDir(zos, i);
	    } else {
		String name = i.getName();
		if (name.endsWith(".js")) {
		    /*
		     */
		    attachFile(i, "js/" + name, zos);
		} else if (name.endsWith(".css")) {
		    attachFile(i, "css/" + name, zos);
		}
	    }
	}
    }

    protected void attachFile(File input, String targetName, ZipOutputStream zos) throws IOException {
	/*
	 * Open the target file
	 */
	InputStream is = new BufferedInputStream(new FileInputStream(input));
	try {
	    /*
	     *
	     */
	    ZipEntry ze = new ZipEntry(targetName);
	    zos.putNextEntry(ze);
	    /*
	     * Copy the file
	     */
	    for (int b = is.read(); b != -1; b = is.read()) {
		zos.write(b);
	    }
	} finally {
	    is.close();
	}
    }
}
