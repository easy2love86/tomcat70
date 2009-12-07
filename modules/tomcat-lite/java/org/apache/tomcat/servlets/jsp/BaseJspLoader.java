/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.servlets.jsp;

import java.io.IOException;
import java.util.Vector;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;


/** 
 * Load a JSP generated by jasper.
 * 
 * Requires a 'jspc' servlet.
 * 
 * This class has no dependencies on Jasper, it uses 2 servlets to integrate.
 */
public abstract class BaseJspLoader {
    boolean usePrecompiled = false;
    
    public static interface JspRuntime {
        public void init(ServletContext ctx);
    }

    public static interface JspCompiler {
        public void compileAndInit(ServletContext ctx, String jspUri, 
                ServletConfig cfg,
                String classPath, String pkg);
    }
    
    /** 
     * Load the proxied jsp, if any.
     * @param config 
     * @throws ServletException 
     * @throws IOException 
     */
    public Servlet loadProxy(String jspFile, 
            ServletContext ctx, 
            ServletConfig config) throws ServletException, IOException {
        synchronized(this.getClass()) {
            // So we don't have a direct dep on jasper...
            Object attribute = ctx.getAttribute("jasper.jspRuntimeContext");
            if (attribute == null) {
                try {
                    Class jsprt = Class.forName("org.apache.tomcat.servlets.jspc.JasperRuntime");
                    JspRuntime rt = (JspRuntime)jsprt.newInstance();
                    rt.init(ctx);
                } catch (Throwable t) {
                    t.printStackTrace();
                    return null;
                }
            }
        }
        String mangledClass = getClassName(ctx, jspFile );

        // TODO: reloading filter: get the class file, 
        // compare with jsp file, use dependants


        HttpServlet jsp = null;
        Class jspC = null;
    
        String cp = getClassPath(ctx);
        ClassLoader cl = getClassLoader(ctx);

        // Already created
        if (usePrecompiled) {
            try {
                jspC = cl.loadClass(mangledClass);
            } catch( Throwable t ) {
                //t.printStackTrace();
                // Not found - first try 
            }
        }

        if (jspC == null) {
            System.err.println("Recompile " + jspFile);
            // Class not found - needs to be compiled
            compileAndInitPage(ctx, jspFile, config, cp);
        } else {
            System.err.println("Pre-compiled " + jspFile);            
        }

        if( jspC == null ) {
            try {
                jspC = cl.loadClass(mangledClass);
            } catch( Throwable t ) {
                //t.printStackTrace();
            }
        }
        if (jspC == null) {
            throw new ServletException("Class not found " + mangledClass);
        }

        try {
            jsp=(HttpServlet)jspC.newInstance();
        } catch( Throwable t ) {
            t.printStackTrace();
        }
        jsp.init(config);
        return jsp;
    }

    public ClassLoader getClassLoader(ServletContext ctx) {
        return null;
    }

    public String getClassPath(ServletContext ctx) {
        return null;
    }
    
    protected void compileAndInitPage(ServletContext ctx, 
            String jspUri, 
            ServletConfig cfg,
            String classPath) 
                throws ServletException, IOException {
        try {
            Class jsprt = Class.forName("org.apache.tomcat.servlets.jspc.JspcServlet");
            JspCompiler rt = (JspCompiler) jsprt.newInstance();
            rt.compileAndInit(ctx, jspUri, cfg, classPath, getPackage(ctx, jspUri));
        } catch (Throwable t) {
            t.printStackTrace();
        }        
    }    

    public boolean needsReload(String jspFile, Servlet s) {
        return false;
    }
    
    public String getPackage(ServletContext ctx, String jspUri) {
        String ver = "v" + ctx.getMajorVersion() + 
        ctx.getMinorVersion();

        int iSep = jspUri.lastIndexOf('/') + 1;
        String className = makeJavaIdentifier(jspUri.substring(iSep));
        String basePackageName = JSP_PACKAGE_NAME;

        iSep--;
        String derivedPackageName = (iSep > 0) ?
                makeJavaPackage(jspUri.substring(1,iSep)) : "";

        return ver + "." + basePackageName;

    }
    
    /** Convert an identifier to a class name, using jasper conventions
     * @param ctx 
     * 
     * @param jspUri a relative JSP file
     * @return class name that would be generated by jasper
     */
    public String getClassName( ServletContext ctx, String jspUri ) {
        // Generated code is different for different servlet API versions
        // We could have a context running in both 2.5 and 3.0 with precompiled
        // jsps
        String ver = "v" + ctx.getMajorVersion() + 
            ctx.getMinorVersion();

        int iSep = jspUri.lastIndexOf('/') + 1;
        String className = makeJavaIdentifier(jspUri.substring(iSep));
        String basePackageName = JSP_PACKAGE_NAME;

        iSep--;
        String derivedPackageName = (iSep > 0) ?
            makeJavaPackage(jspUri.substring(1,iSep)) : "";
        if (derivedPackageName.length() == 0) {
            return basePackageName + "." + className;
        }
        
        return ver + "." + basePackageName + '.' + derivedPackageName + "." + 
            className;
    }

    // ------------- Copied from jasper ---------------------------

    private static final String JSP_PACKAGE_NAME = "org.apache.jsp";

    private static final String makeJavaIdentifier(String identifier) {
        StringBuffer modifiedIdentifier = 
            new StringBuffer(identifier.length());
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            modifiedIdentifier.append('_');
        }
        for (int i = 0; i < identifier.length(); i++) {
            char ch = identifier.charAt(i);
            if (Character.isJavaIdentifierPart(ch) && ch != '_') {
                modifiedIdentifier.append(ch);
            } else if (ch == '.') {
                modifiedIdentifier.append('_');
            } else {
                modifiedIdentifier.append(mangleChar(ch));
            }
        }
        if (isJavaKeyword(modifiedIdentifier.toString())) {
            modifiedIdentifier.append('_');
        }
        return modifiedIdentifier.toString();
    }

    private static final String javaKeywords[] = {
        "abstract", "assert", "boolean", "break", "byte", "case",
        "catch", "char", "class", "const", "continue",
        "default", "do", "double", "else", "enum", "extends",
        "final", "finally", "float", "for", "goto",
        "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package",
        "private", "protected", "public", "return", "short",
        "static", "strictfp", "super", "switch", "synchronized",
        "this", "throws", "transient", "try", "void",
        "volatile", "while" };

    private static final String makeJavaPackage(String path) {
        String classNameComponents[] = split(path,"/");
        StringBuffer legalClassNames = new StringBuffer();
        for (int i = 0; i < classNameComponents.length; i++) {
            legalClassNames.append(makeJavaIdentifier(classNameComponents[i]));
            if (i < classNameComponents.length - 1) {
                legalClassNames.append('.');
            }
        }
        return legalClassNames.toString();
    }

    private static final String [] split(String path, String pat) {
        Vector comps = new Vector();
        int pos = path.indexOf(pat);
        int start = 0;
        while( pos >= 0 ) {
            if(pos > start ) {
                String comp = path.substring(start,pos);
                comps.add(comp);
            }
            start = pos + pat.length();
            pos = path.indexOf(pat,start);
        }
        if( start < path.length()) {
            comps.add(path.substring(start));
        }
        String [] result = new String[comps.size()];
        for(int i=0; i < comps.size(); i++) {
            result[i] = (String)comps.elementAt(i);
        }
        return result;
    }
            

    /**
     * Test whether the argument is a Java keyword
     */
    private static boolean isJavaKeyword(String key) {
        int i = 0;
        int j = javaKeywords.length;
        while (i < j) {
            int k = (i+j)/2;
            int result = javaKeywords[k].compareTo(key);
            if (result == 0) {
                return true;
            }
            if (result < 0) {
                i = k+1;
            } else {
                j = k;
            }
        }
        return false;
    }

    /**
     * Mangle the specified character to create a legal Java class name.
     */
    private static final String mangleChar(char ch) {
        char[] result = new char[5];
        result[0] = '_';
        result[1] = Character.forDigit((ch >> 12) & 0xf, 16);
        result[2] = Character.forDigit((ch >> 8) & 0xf, 16);
        result[3] = Character.forDigit((ch >> 4) & 0xf, 16);
        result[4] = Character.forDigit(ch & 0xf, 16);
        return new String(result);
    }
}