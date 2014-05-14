package org.jenkinsci.plugins.koji;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.File;
import java.io.IOException;

public class KojiLauncher {

    private final String workspacePath;
    private String[] command;

    private final AbstractBuild<?,?> build;
    private final BuildListener listener;
    private Launcher launcher;

    public KojiLauncher(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) {
        this.build = build;
        this.listener = listener;
        this.launcher = launcher;

        workspacePath = initWorkspacePath();
    }

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

    void moshiMoshiCommand() {
        command =  new String[]{"koji", "moshimoshi"};
    }

    void mavenBuildCommand(String options, String target, String sources) {
        String[] tmpCommand =  new String[]{"koji", "maven-build", "-Dmaven.test.skip=true", target, sources};
        if (options.equals("")) {
            command = tmpCommand;
        } else {
            command = concatenate(tmpCommand, new String[] {options});
        }
    }

    void watchTaskCommand(String taskId) {
        command =  new String[]{"koji", "watch-task", taskId};
    }

    void downloadCommand(String kojiBuild) {
        command =  new String[]{"koji", "download-build", "--type=maven", kojiBuild};
    }

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

    public static String[] concatenate(String[] A, String[] B) {
        int aLength = A.length;
        int bLength = B.length;
        String[] C = new String[aLength + bLength];
        System.arraycopy(A, 0, C, 0, aLength);
        System.arraycopy(B, 0, C, aLength, bLength);
        return C;
    }
}
