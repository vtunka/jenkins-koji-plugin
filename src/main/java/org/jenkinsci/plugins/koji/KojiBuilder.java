package org.jenkinsci.plugins.koji;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.XStream2;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.xmlrpc.XmlRpcException;
import org.jenkinsci.plugins.koji.xmlrpc.KojiClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;


/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link KojiBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #kojiBuild})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * @author Vaclav Tunka
 */
public class KojiBuilder extends Builder {

    private final String kojiBuild;
    private final String kojiTarget;
    private final String kojiPackage;
    private final String kojiOptions;
    private final String kojiTask;

    private transient BuildListener listener;
    private transient KojiClient koji;
    private BuildListener listener;
    private KojiClient koji;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public KojiBuilder(String kojiBuild, String kojiTarget, String kojiPackage, String kojiOptions, String kojiTask) {
        this.kojiBuild = kojiBuild;
        this.kojiTarget = kojiTarget;
        this.kojiPackage = kojiPackage;
        this.kojiOptions = kojiOptions;
        this.kojiTask = kojiTask;
    }

    public String getKojiBuild() {
        return kojiBuild;
    }

    public String getKojiTarget() {
        return kojiTarget;
    }

    public String getKojiPackage() {
        return kojiPackage;
    }

    public String getKojiOptions() {
        return kojiOptions;
    }

    public String getKojiTask() {
        return kojiTask;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
//        printDebugInfo();
        init(listener);

        KojiLauncher kojiLauncher = new KojiLauncher(build, launcher, listener);
        kojiLauncher.callKoji();
        String hello = koji.sayHello();
        listener.getLogger().println(hello);

        String pkg = "kernel";
        String tag = "f21";
        getLatestBuilds(pkg, tag);

        String buildId = "kernel-3.15.0-0.rc3.git5.3.fc21";
        getBuildInfo(buildId);

        return true;
    }

    private void getLatestBuilds(String pkg, String tag) {
        Map<String, String> result = null;

        try {
            result = koji.getLatestBuilds(tag, pkg);
        } catch (XmlRpcException e) {
            if (e.getMessage() == "empty") {
                listener.getLogger().println("No package " + pkg + " found for tag " + tag);
                return;
            }
            else {
                listener.getLogger().println(e.getMessage());
                return;
            }
        }
        for (Map.Entry<String, String> entry : result.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            listener.getLogger().println(key + ": " + value);
        }
    }

    private void getBuildInfo(String build) {
        Map<String, String> buildInfo = null;
        try {
            buildInfo = koji.getBuildInfo(build);
        } catch (XmlRpcException e) {
            if (e.getMessage() == "empty") {
                listener.getLogger().println("No build with id=" + build + " found in the database.");
                return;
            }
            else
                e.printStackTrace();
        }
        for (Map.Entry<String, String> entry : buildInfo.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            listener.getLogger().println(key + ": " + value);
        }
    }

    private void init(BuildListener listener) {
        this.listener = listener;
        this.koji = KojiClient.getKojiClient(getDescriptor().getKojiInstanceURL());
    }

    private void printDebugInfo() {
        listener.getLogger().println("This is the selected Koji Instance: " + getDescriptor().getKojiInstanceURL());
        listener.getLogger().println("This is the selected Koji Build: " + kojiBuild);
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link KojiBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/KojiBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String kojiInstanceURL;
        private String authentication;
        private String kojiUsername;
        private String kojiPassword;
        private String sslCertificatePath;

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            super(KojiBuilder.class);
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'kojiInstanceURL'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckKojiInstanceURL(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a Koji instance URL");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the Koji instance URL too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Koji integration";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            kojiInstanceURL = formData.getString("kojiInstanceURL");
            authentication = formData.getString("authentication");
            kojiUsername = formData.getString("kojiUsername");
            kojiPassword = formData.getString("kojiPassword");
            sslCertificatePath = formData.getString("sslCertificatePath");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * Get Koji Instance URL.
         */
        public String getKojiInstanceURL() {
            return kojiInstanceURL;
        }

        public void setKojiInstanceURL(String kojiInstanceURL) {
            this.kojiInstanceURL = kojiInstanceURL;
        }

        public String getAuthentication() {
            return authentication;
        }

        public void setAuthentication(String authentication) {
            this.authentication = authentication;
        }

        public String getKojiUsername() {
            return kojiUsername;
        }

        public void setKojiUsername(String kojiUsername) {
            this.kojiUsername = kojiUsername;
        }

        public String getKojiPassword() {
            return kojiPassword;
        }

        public void setKojiPassword(String kojiPassword) {
            this.kojiPassword = kojiPassword;
        }

        public String getSslCertificatePath() {
            return sslCertificatePath;
        }

        public void setSslCertificatePath(String sslCertificatePath) {
            this.sslCertificatePath = sslCertificatePath;
        }
    }
}

