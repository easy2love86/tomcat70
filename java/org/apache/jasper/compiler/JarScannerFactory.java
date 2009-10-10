/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.jasper.compiler;

import javax.servlet.ServletContext;

import org.apache.tomcat.JarScanner;

/**
 * Provide a mechanism for Jasper to obtain a reference to the JarScanner
 * impementation.
 */
public class JarScannerFactory {

    /*
     * Don't want any instances so hide the default constructor.
     */
    private JarScannerFactory() {
    }

    /**
     * Obtain the {@link JarScanner} associated with the specificed {@link
     * ServletContext}. It is obtained via a context parameter.
     */
    public static JarScanner getJarScanner(ServletContext ctxt) {
        JarScanner jarScanner = 
        	(JarScanner) ctxt.getAttribute(JarScanner.class.getName());
        if (jarScanner == null) {
            ctxt.log(Localizer.getMessage("jsp.warning.noJarScanner"));
        }
        return jarScanner;
    }

}