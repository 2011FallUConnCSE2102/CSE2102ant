/*
 * Copyright 2004-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.taskdefs.repository.AssertDownloaded;
import org.apache.tools.ant.taskdefs.repository.Libraries;
import org.apache.tools.ant.taskdefs.repository.Library;
import org.apache.tools.ant.taskdefs.repository.Maven2Layout;

/**
 * test the test libraries stuff.
 * skip all the tests if we are offline
 */
public class LibrariesTest extends BuildFileTest {
    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/";


    public LibrariesTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject(TASKDEFS_DIR + "libraries.xml");
    }

    protected boolean offline() {
        return "true".equals(System.getProperty("offline"));
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testEmpty() {
        expectBuildException("testEmpty", Libraries.ERROR_NO_LIBRARIES);
    }

    public void testEmpty2() {
        expectBuildException("testEmpty2", Libraries.ERROR_NO_LIBRARIES);
    }

    public void testEmpty3() {
        expectBuildException("testEmpty3", Libraries.ERROR_NO_LIBRARIES);
    }

    public void testNoRepo() {
        execIfOnline("testNoRepo");
    }

    public void testUnknownReference() {
        expectBuildException("testUnknownReference", "Reference unknown not found.");
    }

    /**
     * refs are  broken
     * */
    public void testFunctionalInline() {
        execIfOnline("testFunctionalInline");
    }
    
    public void testMavenInline() {
        String targetName = "testMavenInline";
        execIfOnline(targetName);
    }

    /**
     * exec a target, but only if we are online
     * @param targetName
     */
    private void execIfOnline(String targetName) {
        if (offline()) {
            return;
        }
        executeTarget(targetName);
    }

    public void testTwoRepositories() {
        expectBuildException("testTwoRepositories",
                Libraries.ERROR_ONE_REPOSITORY_ONLY);
    }

    public void testMavenInlineBadURL() {
        expectExceptionIfOnline("testMavenInlineBadURL",
                "testMavenInlineBadURL",
                Libraries.ERROR_INCOMPLETE_RETRIEVAL);
    }

    /**
     * exec a target if we are online; expect an eception
     * @param target
     * @param cause cause of the fault
     * @param message
     */
    private void expectExceptionIfOnline(String target, String cause,String message) {
        if (offline()) {
            return;
        }
        expectBuildExceptionContaining(target,cause,
                message);
    }

    public void testRenaming() {
        execIfOnline("testRenaming");
    }

    public void testOverwrite() {
        execIfOnline("testOverwrite");
    }

    public void testIf() {
        execIfOnline("testIf");
    }

    public void testUnless() {
        execIfOnline("testUnless");
    }

    public void testPathID() {
        execIfOnline("testPathID");
    }

    public void testSecurity() {
        execIfOnline("testSecurity");
    }

    public void testSchedule() {
        execIfOnline("testSchedule");
    }

    public void testForceEnabled() {
        execIfOnline("testForceEnabled");
    }

    public void testForceDisabled() {
        execIfOnline("testForceDisabled");
    }

    public void testAbsentFiles() {
        execIfOnline("testAbsentFiles");
    }

    public void testAbsentFilesTwice() {
        execIfOnline("testAbsentFilesTwice");
    }

    public void testNoUpdate() {
        expectExceptionIfOnline("testNoUpdate",
                "update disabled; dest file missing",
                Libraries.ERROR_INCOMPLETE_RETRIEVAL);
    }

    public void testTimestamp() {
        execIfOnline("testTimestamp");
    }

    public void testAssertDownloadedCountSet() {
        expectExceptionIfOnline("testAssertDownloadedCountSet",
                "No count in assertdownloaded",
                AssertDownloaded.ERROR_NO_COUNT);
    }

    public void testAssertDownloadedCountTested() {
        expectExceptionIfOnline("testAssertDownloadedCountTested",
                "Wrong count in assertdownloaded",
                AssertDownloaded.ERROR_DOWNLOAD_FAILURE);
    }

    public void testNoVersion() {
        expectBuildException("testNoVersion",
                Library.ERROR_NO_PROJECT);
    }

    public void testNoProject() {
        expectBuildException("testNoProject",
                Library.ERROR_NO_PROJECT);
    }

    public void testNoArchiveName() {
        execIfOnline("testNoArchiveName");
    }

    public void testEmptyArchive() {
        expectBuildException("testEmptyArchive",
                Library.ERROR_NO_ARCHIVE);
    }

    public void testNoSuffix() {
        execIfOnline("testNoSuffix");
    }

    public void testFlatten() {
        execIfOnline("testFlatten");
    }

    public void testMavenNaming() {
        Library lib=new Library();
        lib.setProject("unknown");
        lib.setArchive("test");
        lib.setVersion("3.4");
        assertEquals(".jar",lib.getSuffix());
        assertNull("lib.getClassifier()!=null", lib.getClassifier());
        String shortname=Maven2Layout.createFilename(lib);
        assertEquals("test-3.4.jar",shortname);
        //add a classifierand test that works
        lib.setClassifier("src");
        assertNotNull("lib.getClassifier()==null", lib.getClassifier());
        shortname = Maven2Layout.createFilename(lib);
        assertEquals("test-3.4-src.jar", shortname);
    }
}
