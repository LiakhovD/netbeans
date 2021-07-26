/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.java.mx.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.spi.java.project.support.ProjectPlatform;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.SpecificationVersion;
import org.openide.util.Exceptions;

final class Jdks {
    private final SuiteProject prj;

    Jdks(SuiteProject prj) {
        this.prj = prj;
    }

    final JavaPlatform find(Compliance compliance) {
        for (File f : jdks()) {
            FileObject fo = FileUtil.toFileObject(f);
            if (fo == null) {
                continue;
            }
            JavaPlatform p = ProjectPlatform.forProject(prj, fo, fo.getName(), "j2se");
            if (p == null) {
                if (fo.getName().equals("jre")) {
                    fo = fo.getParent();
                    p = ProjectPlatform.forProject(prj, fo, fo.getName(), "j2se");
                }
                if (p == null) {
                    continue;
                }
            }
            final Specification spec = p.getSpecification();
            if (spec == null) {
                continue;
            }
            SpecificationVersion version = spec.getVersion();
            if (version == null) {
                continue;
            }
            Compliance pVersion = Compliance.parse(version.toString());
            if (compliance.matches(pVersion)) {
                return p;
            }
        }
        return JavaPlatform.getDefault();
    }

    final Iterable<File> jdks() {
        Set<File> jdks = new LinkedHashSet<>();
        String home = System.getProperty("user.home");
        if (home != null) {
            File userEnv = new File(new File(new File(home), ".mx"), "env");
            findJdksInEnv(jdks, userEnv);
        }
        FileObject dir = prj.getProjectDirectory();
        FileObject suiteEnv = dir.getFileObject("mx." + dir.getNameExt() + "/env");
        if (suiteEnv != null) {
            findJdksInEnv(jdks, FileUtil.toFile(suiteEnv));
        }

        String javaHomeEnv = System.getenv("JAVA_HOME");
        if (javaHomeEnv != null) {
            jdks.add(new File(javaHomeEnv));
        }
        String javaHomeProp = System.getProperty("java.home");
        if (javaHomeProp != null) {
            jdks.add(new File(javaHomeProp));
        }
        return jdks;
    }

    private void findJdksInEnv(Set<File> jdks, File env) {
        if (env == null || !env.isFile()) {
            return;
        }
        try (final FileInputStream is = new FileInputStream(env)) {
            Properties p = new Properties();
            p.load(is);

            String javaHome = p.getProperty("JAVA_HOME");
            if (javaHome != null) {
                jdks.add(new File(javaHome));
            }

            String extraJavaHomes = p.getProperty("EXTRA_JAVA_HOMES");
            if (extraJavaHomes != null) {
                for (String extraHome : extraJavaHomes.split(File.pathSeparator)) {
                    jdks.add(new File(extraHome));
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }


}
