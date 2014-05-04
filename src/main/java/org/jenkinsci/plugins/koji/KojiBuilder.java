package org.jenkinsci.plugins.koji;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.MyTypeFactory;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;


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
    private BuildListener listener;
    private XmlRpcClient koji;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public KojiBuilder(String kojiBuild) {
        this.kojiBuild = kojiBuild;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getKojiBuild() {
        return kojiBuild;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        this.listener = listener;

        printDebugInfo();

        XmlRpcClient koji = connect();
        this.koji = koji;

        sayHello();


        return true;
    }

    private void sayHello() {
        try {
            Object result = koji.execute("hello", new Object[] {"Hello"});
            listener.getLogger().println("Jenkins-Koji Plugin: Hello Koji server running at " + getDescriptor().getKojiInstanceURL());
            listener.getLogger().println("Koji: " + result);
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
    }

    private XmlRpcClient connect() {
        String kojiInstanceURL = getDescriptor().getKojiInstanceURL();
        XmlRpcClient koji = new XmlRpcClient();
        koji.setTransportFactory(new XmlRpcCommonsTransportFactory(koji));
        koji.setTypeFactory(new MyTypeFactory(koji));
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setEnabledForExtensions(true);
        config.setEnabledForExceptions(true);

        try {
          config.setServerURL(new URL(kojiInstanceURL));
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }

        koji.setConfig(config);

        return koji;
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

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'kojiBuild'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckKojiBuild(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a Koji build");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the Koji build coordinate too short?");
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
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public String getKojiInstanceURL() {
            return kojiInstanceURL;
        }
    }
}

