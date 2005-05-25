/*
 * Copyright  2003-2005 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.io.File;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.util.FileUtils;

/**
 * Base class that deals with JProbe version incompatibilities.
 *
 * @since Ant 1.6
 *
 */
public abstract class CovBase extends Task {
    private File home;
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private boolean isJProbe4 = false;
    private static boolean isDos = Os.isFamily("dos");

    /**
     * The directory where JProbe is installed.
     * @param value the JProbe directory
     */
    public void setHome(File value) {
        this.home = value;
    }

    /**
     * Get the JProbe directory.
     * @return the JProbe directory
     */
    protected File getHome() {
        return home;
    }

    /**
     * Get the location of the JProbe coverage jar file.
     * @return the location of the JProbe coverage jar file
     */
    protected File findCoverageJar() {
        File loc = null;
        if (isJProbe4) {
            loc = FILE_UTILS.resolveFile(home, "lib/coverage.jar");
        } else {
            loc = FILE_UTILS.resolveFile(home, "coverage/coverage.jar");
            if (!loc.canRead()) {
                File newLoc = FILE_UTILS.resolveFile(home, "lib/coverage.jar");
                if (newLoc.canRead()) {
                    isJProbe4 = true;
                    loc = newLoc;
                }
            }
        }

        return loc;
    }

    /**
     * Find the JProbe executable.
     * @param relativePath the name of the executuable without the trailing .exe on dos
     * @return the absolute path to the executable
     */
    protected String findExecutable(String relativePath) {
        if (isDos) {
            relativePath += ".exe";
        }

        File loc = null;
        if (isJProbe4) {
            loc = FILE_UTILS.resolveFile(home, "bin/" + relativePath);
        } else {
            loc = FILE_UTILS.resolveFile(home, relativePath);
            if (!loc.canRead()) {
                File newLoc = FILE_UTILS.resolveFile(home, "bin/" + relativePath);
                if (newLoc.canRead()) {
                    isJProbe4 = true;
                    loc = newLoc;
                }
            }
        }
        return loc.getAbsolutePath();
    }

    /**
     * Create a temporary file.
     * @param prefix a prefix to use in the filename
     * @return a File reference to the temporary file
     */
    protected File createTempFile(String prefix) {
        return FILE_UTILS.createTempFile(prefix, ".tmp", null);
    }

    /**
     * Get the param file arguement.
     * This checks the version of jprobe to return the correct name of
     * the parameter.
     * @return the name of the argument
     */
    protected String getParamFileArgument() {
        return "-" + (!isJProbe4 ? "jp_" : "") + "paramfile=";
    }

    /**
     * Are we running on a version of JProbe 4.x or higher?
     * @return true if we are running JProbe 4 or higher
     */
    protected boolean isJProbe4Plus() {
        return isJProbe4;
    }
}
