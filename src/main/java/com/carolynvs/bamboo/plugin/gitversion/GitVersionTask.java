package com.carolynvs.bamboo.plugin.gitversion;

import com.atlassian.bamboo.build.BuildOutputLogEntry;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.plugins.git.GitCapabilityTypeModule;
import com.atlassian.bamboo.process.BambooProcessHandler;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.utils.process.*;
import com.google.common.collect.Lists;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GitVersionTask implements TaskType
{
    private static final String PREFIX = "GitVersion";
    private final CapabilityContext capabilityContext;

    public GitVersionTask(CapabilityContext capabilityContext)
    {
        this.capabilityContext = capabilityContext;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        TaskResultBuilder resultBuilder = TaskResultBuilder.create(taskContext);
        BuildLogger buildLogger = taskContext.getBuildLogger();

        GitVersionTaskConfiguration config = readConfiguration(taskContext);

        boolean fixDetachedHeadSucceeded = fixDetachedHead(config, buildLogger);
        if (!fixDetachedHeadSucceeded)
            return resultBuilder.failed().build();

        GitVersionOutput gitVersionOutput = executeGitVersion(config.Repository, buildLogger);
        if(!gitVersionOutput.Succeeded)
            return resultBuilder.failed().build();

        Map<String, String> buildMetadata = taskContext.getBuildContext().getBuildResult().getCustomBuildData();
        saveVariables(gitVersionOutput.Variables, buildMetadata, buildLogger);

        return resultBuilder.success().build();
    }

    private boolean fixDetachedHead(GitVersionTaskConfiguration input, BuildLogger buildLogger)
    {
        buildLogger.addBuildLogEntry("Checking if the default repository is in a detached HEAD state...");
        GitOutput gitCurrentBranch = executeGitCommand(input.Repository, buildLogger, "rev-parse", "--abbrev-ref", "HEAD");
        if(!gitCurrentBranch.Succeeded)
            return false;

        // Check for detached head
        if(input.Branch.equals(gitCurrentBranch.Output))
            return true;

        buildLogger.addBuildLogEntry("Fixing detached HEAD state...");
        GitOutput gitCheckout = executeGitCommand(input.Repository, buildLogger, "checkout", "--force", input.Branch);
        if(!gitCheckout.Succeeded)
            return false;

        GitOutput gitReset = executeGitCommand(input.Repository, buildLogger, "reset", "--hard", input.Revision);
        if(!gitReset.Succeeded)
            return false;

        return true;
    }

    private GitVersionTaskConfiguration readConfiguration(TaskContext taskContext)
    {
        ConfigurationMap taskConfig = taskContext.getConfigurationMap();
        String repoPath =  taskConfig.get(GitVersionTaskConfigurator.REPO_PATH);
        File buildDirectory = taskContext.getWorkingDirectory();
        File repo = new File(buildDirectory, repoPath);

        // todo: Make this configurable, currently assumes we are only working with the default repository
        Map<String, String> buildMetaData = taskContext.getBuildContext().getCurrentResult().getCustomBuildData();
        String branch = buildMetaData.get("planRepository.branchName");
        String revision = buildMetaData.get("planRepository.revision");

        return new GitVersionTaskConfiguration(repo, branch, revision);
    }

    /**
     * Persist the GitVersion variables to the build's metadata
     */
    private void saveVariables(JsonNode variables, Map<String, String> buildMetadata, BuildLogger buildLogger)
    {
        for(Iterator<Map.Entry<String, JsonNode>> results = variables.getFields(); results.hasNext();)
        {
            Map.Entry<String, JsonNode> result = results.next();
            String key = String.format("%s.%s", PREFIX, result.getKey());

            JsonNode jsonValue = result.getValue();
            String value = jsonValue.isTextual() ? jsonValue.getTextValue() : jsonValue.toString();

            buildLogger.addBuildLogEntry(String.format("%s=%s", key, value));
            buildMetadata.put(key, value);
        }
    }

    @Nullable
    private String getGitVersionExecutable()
    {
        return capabilityContext.getCapabilityValue(GitVersionCapability.CAPABILITY_KEY);
    }

    @Nullable
    public String getGitExecutable()
    {
        return capabilityContext.getCapabilityValue(GitCapabilityTypeModule.GIT_CAPABILITY);
    }

    private GitVersionOutput executeGitVersion(File workingDirectory, BuildLogger buildLogger)
    {
        StringOutputHandler outputHandler = new StringOutputHandler();
        ExternalProcess process = new ExternalProcessBuilder()
                .command(Lists.newArrayList(getGitVersionExecutable()), workingDirectory)
                .handler(new BambooProcessHandler(outputHandler, outputHandler))
                .build();
        buildLogger.addBuildLogEntry(String.format("Executing %s", process.getCommandLine()));
        process.execute();

        GitVersionOutput gitVersionOutput = new GitVersionOutput(process.getHandler(), outputHandler);
        if(gitVersionOutput.Succeeded)
            buildLogger.addBuildLogEntry(gitVersionOutput.Variables.toString());
        else
            buildLogger.addErrorLogEntry(gitVersionOutput.ErrorMessage, gitVersionOutput.Exception);

        return gitVersionOutput;
    }

    private GitOutput executeGitCommand(File workingDirectory, BuildLogger buildLogger, String... commandArgs)
    {
        List<String> command = new ArrayList<String>();
        command.add(getGitExecutable());
        command.addAll(Arrays.asList(commandArgs));

        StringOutputHandler outputHandler = new StringOutputHandler();
        ExternalProcess process = new ExternalProcessBuilder()
                .command(command, workingDirectory)
                .handler(new BambooProcessHandler(outputHandler, outputHandler))
                .build();
        buildLogger.addBuildLogEntry(String.format("Executing %s", process.getCommandLine()));
        process.execute();

        GitOutput gitOutput = new GitOutput(process.getHandler(), outputHandler);
        if(gitOutput.Succeeded)
            buildLogger.addBuildLogEntry(gitOutput.Output);
        else
            buildLogger.addErrorLogEntry(gitOutput.Output);

        return gitOutput;
    }

    private class GitVersionTaskConfiguration
    {
        public final File Repository;
        public final String Branch;
        public final String Revision;

        public GitVersionTaskConfiguration(File repository, String branch, String revision)
        {
            Repository = repository;
            Branch = branch;
            Revision = revision;
        }
    }

    private class GitVersionOutput
    {
        public boolean Succeeded;
        public String ErrorMessage;
        public Exception Exception;
        public JsonNode Variables;

        public GitVersionOutput(ProcessHandler processHandler, StringOutputHandler processOutputHandler)
        {
            String processOutput = processOutputHandler.getOutput();
            Succeeded = processHandler.succeeded();

            if(!Succeeded)
            {
                ErrorMessage = String.format("Error executing GitVersion\r\n%s", processOutput);
                return;
            }

            ObjectMapper jsonParser = new ObjectMapper();
            try
            {
                Variables = jsonParser.readTree(processOutput);
            }
            catch (IOException e)
            {
                ErrorMessage = "Error parsing json processOutput of GitVersion";
                Exception = e;
                Succeeded = false;
            }
        }
    }

    private class GitOutput
    {
        public boolean Succeeded;
        public String Output;

        public GitOutput(ProcessHandler processHandler, StringOutputHandler outputHandler)
        {
            Succeeded = processHandler.succeeded();
            Output = outputHandler.getOutput().trim();
        }
    }
}