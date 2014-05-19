package org.jenkinsci.plugins.koji;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.xmlrpc.XmlRpcException;
import org.jenkinsci.plugins.koji.xmlrpc.KojiClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;


/**
 * KojiBuilder is the main extension point for Koji integration with Jenkins.
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
@SuppressWarnings("UnusedDeclaration")
public class KojiBuilder extends Builder {

    /**
     * Koji build ID can be Name-Version-Release (NVR) or a maven coordinate.
     */
    private final String kojiBuild;
    /**
     * Koji build tag / target, a set holding builds and packages in Koji (to which builds are *tagged* into).
     */
    private final String kojiTarget;
    /**
     * Koji package is a set of builds, build can belong tu multiple packages.
     */
    private final String kojiPackage;
    /**
     * Additional options passed to Koji.
     */
    private final String kojiOptions;
    /**
     * Koji task that is to be performed.
     */
    private final String kojiTask;
    /**
     * Scratch build is a special Koji build, that does not get tagged and stored permanently in Koji.
     * Used for debugging and testing purposes, scratch builds should never be shipped.
     */
    private boolean kojiScratchBuild;
    /**
     * Koji SCM URL for e.g. git repository. Koji is somehow selective about the format, for example for proper git URL
     * for Koji is git+https://[repo]#[commitHash] - must be readonly, https is better than http and must include commit
     * hash as Koji never builds from moving target.
     * Example: git+https://github.com/vtunka/buildmetadata-maven-plugin#ce68bfc08000ada70a3aa04d92d7c88271ac5b5e
     */
    private final String kojiScmUrl;

    private transient BuildListener listener;
    /**
     * KojiClient is handling XML-RPC communication for the Koji plugin.
     */
    private transient KojiClient koji;

    /**
     * Currently all fields are persisted in single constructor when user submits project configuration form.
     * @param kojiBuild
     * @param kojiTarget
     * @param kojiPackage
     * @param kojiOptions
     * @param kojiTask
     * @param kojiScratchBuild
     * @param kojiScmUrl
     */
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    @SuppressWarnings("UnusedDeclaration")
    public KojiBuilder(String kojiBuild, String kojiTarget, String kojiPackage, String kojiOptions, String kojiTask, boolean kojiScratchBuild, String kojiScmUrl) {
        this.kojiBuild = kojiBuild;
        this.kojiTarget = kojiTarget;
        this.kojiPackage = kojiPackage;
        this.kojiOptions = kojiOptions;
        this.kojiTask = kojiTask;
        this.kojiScmUrl = kojiScmUrl;
        this.kojiScratchBuild = kojiScratchBuild;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getKojiBuild() {
        return kojiBuild;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getKojiTarget() {
        return kojiTarget;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getKojiPackage() {
        return kojiPackage;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getKojiOptions() {
        return kojiOptions;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getKojiTask() {
        return kojiTask;
    }

    public boolean isKojiScratchBuild() {
        return kojiScratchBuild;
    }

    public void setKojiScratchBuild(boolean kojiScratchBuild) {
        this.kojiScratchBuild = kojiScratchBuild;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getKojiScmUrl() {
        return kojiScmUrl;
    }

    /**
     * Main method for plugin execution containing all logic for BuildStep.
     * At first init method is called providing initialization to XML-RPC and Koji-CLI
     * integration.
     *
     * User selected Koji task then gets executed.
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
//        printDebugInfo();
        try {
            init(listener);
        } catch (XmlRpcException e) {
            listener.getLogger().println("\n[Koji integration] " + e.getMessage());
        }

        boolean kojiRunSucceeded = false;
        KojiLauncher kojiLauncher = new KojiLauncher(build, launcher, listener);

        if (kojiTask.equals(KojiTask.mavenBuild.name())) {
            listener.getLogger().println("\n[Koji integration] Running maven build build for package " + kojiPackage + " in tag " + kojiTarget);
            kojiRunSucceeded = kojiLauncher.mavenBuildCommand(isScratchToString(), kojiTarget, kojiScmUrl).callKoji();
        } else if (kojiTask.equals(KojiTask.download.name())) {
            listener.getLogger().println("\n[Koji integration] Downloading artifacts for build " + kojiBuild);
            kojiRunSucceeded = kojiLauncher.downloadCommand(kojiBuild).callKoji();
        } else if (kojiTask.equals(KojiTask.listLatest.name())) {
            listener.getLogger().println("\n[Koji integration] Listing latest build information for package " + kojiPackage + " in tag " + kojiTarget);
            kojiRunSucceeded = getLatestBuilds(kojiPackage, kojiTarget);
        } else if (kojiTask.equals(KojiTask.moshimoshi.name())) {
            kojiLauncher.moshiMoshiCommand().callKoji();
            // always return true, as moshimoshi sometimes returns non-international characters, that cannot be logged
            kojiRunSucceeded = true;
        }

//        listener.getLogger().println("\n[Koji integration] Watching task");
//        kojiLauncher.watchTaskCommand("366");
//        kojiLauncher.callKoji();

        return kojiRunSucceeded;
    }

    /**
     * Translate boolean to Koji CLI accepted string.
     */
    private String isScratchToString() {
        if (kojiScratchBuild) {
            return "--scratch";
        } else
            return "";
    }

    /**
     * Fetch and print latest builds into build console.
     * @param pkg Koji package.
     * @param tag Koji tag.
     * @return Run successful?
     */
    private boolean getLatestBuilds(String pkg, String tag) {
        Map<String, String> result = null;

        listener.getLogger().println("\n[Koji integration] Searching latest build for package " + pkg + " in tag " + tag);
        try {
            result = koji.getLatestBuilds(tag, pkg);
        } catch (XmlRpcException e) {
            if (e.getMessage() == "empty") {
                listener.getLogger().println("[Koji integration] No package " + pkg + " found for tag " + tag);
                return false;
            }
            else {
                listener.getLogger().println("[Koji integration] Error executing Koji command.");
                listener.getLogger().println(e.getMessage());
                return false;
            }
        }
        for (Map.Entry<String, String> entry : result.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            listener.getLogger().println(key + ": " + value);
        }

        return true;
    }

    /**
     * Fetches and prints metadata about a build.
     * @param build Build id, can be NVR or maven coordinate. See javadoc for field kojiBuild for syntax details.
     */
    private void getBuildInfo(String build) {
        Map<String, String> buildInfo = null;

        listener.getLogger().println("\n[Koji integration] Searching for build information for " + build);
        try {
            buildInfo = koji.getBuildInfo(build);
        } catch (XmlRpcException e) {
            if (e.getMessage() == "empty") {
                listener.getLogger().println("[Koji integration] No build with id=" + build + " found in the database.");
                return;
            }
            else {
                listener.getLogger().println("[Koji integration] Error executing Koji command.");
                listener.getLogger().println(e.getMessage());
            }
        }
        for (Map.Entry<String, String> entry : buildInfo.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            listener.getLogger().println(key + ": " + value);
        }
    }

    /**
     * Initializes the Koji XML-RPC session and other preconditions.
     * @param listener Build listener from BuildStep.
     * @throws XmlRpcException when Koji login fails.
     */
    private void init(BuildListener listener) throws XmlRpcException {
        this.listener = listener;
        try {
            this.koji = KojiClient.getKojiClient(getDescriptor().getKojiInstanceURL());
        } catch (MalformedURLException e) {
            listener.getLogger().println("[Koji integration] Error executing Koji command.");
            listener.getLogger().println(e.getMessage());
        }

        if (getDescriptor().getAuthentication().equals(Authentication.plain.name())) {
            koji.login(getDescriptor().getKojiUsername(), getDescriptor().getKojiPassword());
        }
    }

    /**
     * For debugging purposes.
     */
    private void printDebugInfo() {
        listener.getLogger().println("This is the selected Koji Instance: " + getDescriptor().getKojiInstanceURL());
        listener.getLogger().println("This is the selected Koji Build: " + kojiBuild);
    }

    /**
     * Gets the descriptor for this BuildStep.
     * @return
     */
    // Overridden for better type safety.
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

        /**
         * Koji Instance URL, must be URL ending with "kojihub" suffix to connect to XML-RPC hub (that can run on entirely different URL),
         * base Koji URL is not enough. For this reason users have to set full URL to hub and there is no pseudo-intelligent resolving.
         */
        private String kojiInstanceURL;
        /**
         * Selected authentication, see Authentication enum.
         */
        private String authentication;
        /**
         * Used only for plain username/password auth, not for any other authentication method.
         */
        private String kojiUsername;
        /**
         * Same restrictions apply as for kojiUsername field.
         */
        private String kojiPassword;
        /**
         * Path to SSL certificate used for Koji authentication on a  local file system.
         */
        private String sslCertificatePath;


        /**
         * In order to load the persisted global configuration, method load() has to be called in the constructor.
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

        /**
         * This buildstep is applicable to all project types.
         * @return Always true.
         */
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * Fills the items for authentication for global configuration.
         * @return
         */
        @SuppressWarnings("UnusedDeclaration")
        public ListBoxModel doFillAuthenticationItems(){
            ListBoxModel authModel = new ListBoxModel(
                    new ListBoxModel.Option("Username / Password", Authentication.plain.name()),
                    new ListBoxModel.Option("OpenSSL", Authentication.openSSL.name()),
                    new ListBoxModel.Option("Kerberos (TBD)", Authentication.kerberos.name())
            );


            if (authentication == null) {
                authModel.get(0).selected = true;
                return authModel;
            }

            for (ListBoxModel.Option option : authModel) {
                if (option.value.equals(Authentication.plain.name()))
                    option.selected = authentication.equals(Authentication.plain.name());
                else if (option.value.equals(Authentication.openSSL.name()))
                    option.selected = authentication.equals(Authentication.openSSL.name());
                else if (option.value.equals(Authentication.kerberos.name()))
                    option.selected = authentication.equals(Authentication.kerberos.name());
            }

            return authModel;
        }

        /**
         * Fills the Koji task options for project configurations.
         * @return
         */
        @SuppressWarnings("UnusedDeclaration")
        public ListBoxModel doFillKojiTaskItems(){
            ListBoxModel kojiTaskModel = new ListBoxModel(
                    new ListBoxModel.Option("Koji moshimoshi (validate client configuration)", KojiTask.moshimoshi.name()),
                    new ListBoxModel.Option("Run a new maven build", KojiTask.mavenBuild.name()),
                    new ListBoxModel.Option("Download maven build", KojiTask.download.name()),
                    new ListBoxModel.Option("List latest build for package", KojiTask.listLatest.name())
            );
            return kojiTaskModel;
        }

        public static DescriptorImpl get() {
            return Builder.all().get(DescriptorImpl.class);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Koji integration";
        }

        /**
         * Fetches data from Jelly views and configures instance variables for glabal Jenkins configuration.
         * @param req Staple Request.
         * @param formData JSON Form data.
         * @return Successful?
         * @throws FormException
         */
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

        public String getKojiInstanceURL() {
            return kojiInstanceURL;
        }

        public void setKojiInstanceURL(String kojiInstanceURL) {
            this.kojiInstanceURL = kojiInstanceURL;
        }

        @SuppressWarnings("UnusedDeclaration")
        public String getAuthentication() {
            return authentication;
        }

        public void setAuthentication(String authentication) {
            this.authentication = authentication;
        }

        @SuppressWarnings("UnusedDeclaration")
        public String getKojiUsername() {
            return kojiUsername;
        }

        public void setKojiUsername(String kojiUsername) {
            this.kojiUsername = kojiUsername;
        }

        @SuppressWarnings("UnusedDeclaration")
        public String getKojiPassword() {
            return kojiPassword;
        }

        public void setKojiPassword(String kojiPassword) {
            this.kojiPassword = kojiPassword;
        }

        @SuppressWarnings("UnusedDeclaration")
        public String getSslCertificatePath() {
            return sslCertificatePath;
        }

        public void setSslCertificatePath(String sslCertificatePath) {
            this.sslCertificatePath = sslCertificatePath;
        }
    }

    /**
     * There are there authentication options currently supported by this plugin.
     * Plain - Username / Password (has to be specifically enabled in Koji server DB). Only working method for XML-RPC authentication.
     * OpenSSL - preffered method by Koji developers, however has broken support in XML-RPC Koji API. So far only works for
     * Koji CLI.
     * Kerberos - same as OpenSSL, has no support in XML-RPC API, works only for Koji CLI.
     */
    enum Authentication {
        plain, openSSL, kerberos
    }

    /**
     * So far this plugin supports 4 basic Koji tasks supporting release process:
     * List latest build - for a given package tagged in a [tag].
     * Download build - downloads build's artifacts and logs for a Koji build, needs buildId.
     * Run a new maven build - runs a new maven build in freshly provisioned clean-room Koji environment.
     * Moshi Moshi - verifies Koji CLI configuration.
     */
    enum KojiTask {
        mavenBuild, download, listLatest, moshimoshi
    }
}