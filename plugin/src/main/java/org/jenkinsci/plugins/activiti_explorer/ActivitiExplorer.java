package org.jenkinsci.plugins.activiti_explorer;

import com.cisco.step.jenkins.plugins.jenkow.JenkowBuilder.DescriptorImpl;
import com.cisco.step.jenkins.plugins.jenkow.JenkowEngine;
import com.cloudbees.vietnam4j.ProxiedWebApplication;
import hudson.Extension;
import hudson.Util;
import hudson.model.UnprotectedRootAction;
import hudson.model.User;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.activiti.engine.ProcessEngine;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jenkinsci.plugins.activiti_explorer.dto.UserDTO;
import org.jenkinsci.plugins.jenkow.activiti.override.ServletContextDataSource;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Activiti Explorer web application embedded inside Jenkins.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class ActivitiExplorer implements UnprotectedRootAction {
    private ProxiedWebApplication webApp;

    @Inject
    DescriptorImpl descriptor;

    public String getIconFileName() {
        return "/plugin/activiti-explorer/images/24x24/activiti.png";
    }

    public String getDisplayName() {
        return "Activiti Explorer";
    }

    public String getUrlName() {
        return "activiti-explorer";
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        ProxiedWebApplication webApp = getProxyWebApplication(req);

        webApp.getProxiedSession(req.getSession()).setAttribute("jenkins.user", createUserInfo());

        webApp.handleRequest(req, rsp);
    }

    /**
     * Creates {@link UserDTO} that represents the currently logged-in user.
     */
    private UserDTO createUserInfo() {
        Authentication a = Jenkins.getAuthentication();
        User u = User.current();

        UserDTO user = new UserDTO();
        user.id = a.getName();
        user.firstName = u != null ? u.getFullName() : a.getName();
        user.lastName = "";
        user.fullName = u != null ? u.getFullName() : a.getName();
        user.isAdmin = Jenkins.getInstance().getACL().hasPermission(a,Jenkins.ADMINISTER);
        user.isUser = true;

        return user;
    }

    /**
     * Extracts a war file into the specified directory.
     */
    private void extract(URL war, File dir) throws IOException {
        if (dir.exists())
            Util.deleteRecursive(dir);

        JarInputStream jar = new JarInputStream(war.openStream());
        try {
            JarEntry e;
            while ((e=jar.getNextJarEntry())!=null) {
                File dst = new File(dir,e.getName());
                if (e.isDirectory())
                    dst.mkdirs();
                else {
                    dst.getParentFile().mkdirs();
                    FileOutputStream out = new FileOutputStream(dst);
                    try {
                        IOUtils.copy(jar, out);
                    } finally {
                        out.close();
                    }

                    if (e.getTime()>=0)
                        dst.setLastModified(e.getTime());
                }
            }
        } finally {
            jar.close();
        }
    }

    /**
     * Patch activiti-explorer war file so that we can inject our stuff into it.
     */
    private void patch(File war) throws DocumentException, IOException {
        File xml = new File(war,"WEB-INF/applicationContext.xml");
        Document dom = new SAXReader().read(xml);

        // patch data source
        Element ds = (Element)dom.selectSingleNode("/*/*[@id='dataSource']");
        if (ds==null)
            throw new IllegalStateException("Can't find the dataSource bean in "+xml);
        ds.elements().clear();
        ds.addAttribute("class", ServletContextDataSource.class.getName());

        // patch login handler
        Element lh = (Element)dom.selectSingleNode("/*/*[@id='activitiLoginHandler']");
        if (lh==null)
            throw new IllegalStateException("Can't find the login handler bean in "+xml);
        lh.elements().clear();
        lh.addAttribute("class", "org.jenkinsci.plugins.jenkow.activiti.override.JenkinsLoginHandler");

        // inject our own ProcessEngine
        Element pe = (Element)dom.selectSingleNode("/*/*[@id='processEngine']");
        if (pe==null)
            throw new IllegalStateException("Can't find the processEngine bean in "+xml);
        pe.elements().clear();
        pe.attributes().clear();
        pe.addAttribute("id", "processEngine");
        pe.addAttribute("class", "org.jenkinsci.plugins.jenkow.activiti.override.JenkinsProcessEngineFactory");

        // no more demo data generation
        Element ddg = (Element)dom.selectSingleNode("/*/*[@id='demoDataGenerator']");
        if (ddg==null)
            throw new IllegalStateException("Can't find the demoDataGenerator bean in "+xml);
        ddg.getParent().remove(ddg);

        FileOutputStream out = new FileOutputStream(xml);
        try {
            new XMLWriter(out, OutputFormat.createPrettyPrint()).write(dom);
        } finally {
            out.close();
        }
    }

    private synchronized ProxiedWebApplication getProxyWebApplication(StaplerRequest req) throws ServletException {
        if (webApp==null) {
            try {
                final ClassLoader ourLoader = getClass().getClassLoader();

                File war = new File(Jenkins.getInstance().getRootDir(), "cache/activiti-explorer");
                extract(ourLoader.getResource("activiti-explorer.war"),war);
                patch(war);
                webApp = new ProxiedWebApplication(
                        war,
                        req.getContextPath()+'/'+getUrlName());

                webApp.setParentLoaderHasPriority(true);

                webApp.addClassPath(ourLoader.getResource("activiti-explorer-override.jar"));
                // inject DataSource
                webApp.getProxiedServletContext().setAttribute(ServletContextDataSource.class.getName(),
                        descriptor.getDatabase().getDataSource());
                webApp.getProxiedServletContext().setAttribute(ProcessEngine.class.getName(),
                        JenkowEngine.getEngine());

                // pass through to the servlet container so that Activiti won't get confused
                // by what Jenkins loads (e.g., log4j version inconsistency)
                // but we do expose DTO classes to share them between two apps.
                webApp.setParentClassLoader(new URLClassLoader(new URL[0],HttpServletRequest.class.getClassLoader()) {
                    @Override
                    protected Class<?> findClass(String name) throws ClassNotFoundException {
                        if (name.startsWith("org.jenkinsci.plugins.activiti_explorer.dto.")
                         || name.startsWith("org.activiti.engine.")) {
                            return getClass().getClassLoader().loadClass(name);
                        }
                        throw new ClassNotFoundException(name);
                    }
                });

                webApp.start();
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
        return webApp;
    }
}
