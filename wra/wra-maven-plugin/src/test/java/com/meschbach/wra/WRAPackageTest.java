/*
 * Copyright 2011 Mark Eschbach.
 *
 * $HeadURL$
 * $Id$
 */
package com.meschbach.wra;

import java.io.File;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

/**
 *
 * @author "Mark Eschbach" (meschbach@gmail.com)
 */
public class WRAPackageTest extends AbstractMojoTestCase {
    //I do not know how to automate my system tests yet, so I'm resorting to
    //hand testing.
    public void testIWishIKnewHowToUseTheHarness(){
	
    }
//
//    public void testWRAOutputFileCreated() throws Exception {
//	File testPOM = new File(getBasedir(), "target/test-classes/system/plugin-zip.xml");
//	File base = testPOM.getParentFile();
//	File source = new File( base, "zip-test" );
//	File output = new File( base, "target");
//	assertTrue(testPOM.exists());
//
//	container.initialize();
//	WRAPackageMojo mojo = (WRAPackageMojo) lookupMojo("wra-package", testPOM);
////	setVariableValueToObject(mojo, "wraSourceDirectory", source.getAbsolutePath());
////	setVariableValueToObject(mojo, "outputDirectory", output);
//
//	mojo.execute();
//    }
}
