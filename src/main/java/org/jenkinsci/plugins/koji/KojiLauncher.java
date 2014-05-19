package org.jenkinsci.plugins.koji;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.File;
import java.io.IOException;

/**
 * This class server for invocation of the Koji CLI. This is required especially for OpenSSL and Kerberos authentication
 * which are currently not properly supported in the Koji XML-RPC API.
 */
public class KojiLauncher {

    /**
     * Workspace path
     */
    private final String workspacePath;
    /**
     * Concatenated command
     */
    private String[] command;

    /**
     * Build reference taken from KojiBuilder.
     */
    private final AbstractBuild<?,?> build;
    /**
     * Build listener reference taken from KojiBuilder.
     */
    private final BuildListener listener;

    /**
     * Launcher object taking care of the actual invocation
     */
    private Launcher launcher;

    /**
     * Initializes the base infrastructure for proper Koji CLI invocation.
     * @param build See field reference.
     * @param launcher See field reference.
     * @param listener See field reference.
     */
    public KojiLauncher(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) {
        this.build = build;
        this.listener = listener;
        this.launcher = launcher;

        workspacePath = initWorkspacePath();
    }

    /**
     * Initializes the workspace path.
     * @return Workspace path.
     */
    private String initWorkspacePath() {
        String workspace = "";
        try {
            workspace = build.getWorkspace().absolutize().getRemote();
        } catch (IOException e) {
            listener.getLogger().println("[Koji integration] Error executing Koji command.");
            listener.getLogger().println(e.getMessage());
            return null;
        } catch (InterruptedException e) {
            listener.getLogger().println("[Koji integration] Error executing Koji command.");
            listener.getLogger().println(e.getMessage());
            return null;
        }

        return workspace;
    }

    /**
     * Construct command for Koji CLI validation.
     */
    KojiLauncher moshiMoshiCommand() {
        command =  new String[]{"koji", "moshimoshi"};
        return this;
    }

    /**
     * Construct a new maven build command.
     * @param options Options for a build.
     * @param target Target to which build is tagged.
     * @param sources Sources in format of git+https://[repo]#[hash]
     */
    KojiLauncher mavenBuildCommand(String options, String target, String sources) {
        // Tests are always skipped in Koji. Koji isn't built to act as CI or test execution environment,
        // as most executors are offline on purpose.
        String[] tmpCommand =  new String[]{"koji", "maven-build", "-Dmaven.test.skip=true", target, sources};
        if (options.equals("")) {
            command = tmpCommand;
        } else {
            command = concatenate(tmpCommand, new String[] {options});
        }

        return this;
    }

    /**
     * Watch a Koji task.
     * @param taskId TaskId, usually a number.
     */
    KojiLauncher watchTaskCommand(String taskId) {
        command =  new String[]{"koji", "watch-task", taskId};
        return this;
    }

    /**
     * Download a Koji build artifacts and logs.
     * @param kojiBuild Koji build can be either Name Version Release (NVR) or can have maven coordinates.
     */
    KojiLauncher downloadCommand(String kojiBuild) {
        command =  new String[]{"koji", "download-build", "--type=maven", kojiBuild};
        return this;
    }

    /**
     * Tries to call Koji with a given command. Be sure to call one of the *Command() method first.
     * @return
     */
    public boolean callKoji() {
        boolean successfull = true;

        successfull = (workspacePath != null);
        if (!successfull) return successfull;

//        listener.getLogger().println("[Koji integration] Workspace path: " + workspacePath);

        try {
            int exitCode = launcher.launch().cmds(command).envs(build.getEnvironment(listener)).pwd(build.getWorkspace()).stdout(listener).join();
            successfull = (exitCode == 0);
        } catch (IOException e) {
            listener.getLogger().println("[Koji integration] Error executing Koji command.");
            listener.getLogger().println(e.getMessage());
            return false;
        } catch (InterruptedException e) {
            listener.getLogger().println("[Koji integration] Error executing Koji command.");
            listener.getLogger().println(e.getMessage());
            return false;
        }

        return successfull;
    }

    /**
     * Convenience method to concatenate two arrays.
     * @param A First array.
     * @param B Second array.
     * @return Resulting concatenated array.
     */
    public static String[] concatenate(String[] A, String[] B) {
        int aLength = A.length;
        int bLength = B.length;
        String[] C = new String[aLength + bLength];
        System.arraycopy(A, 0, C, 0, aLength);
        System.arraycopy(B, 0, C, aLength, bLength);
        return C;
    }
}
