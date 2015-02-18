package com.carolynvs.bamboo.plugin.gitversion;

import com.atlassian.bamboo.build.logger.BuildLogger;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

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
    public TaskResult execute(@NotNull TaskContext taskContext)
    {
        TaskResultBuilder resultBuilder = TaskResultBuilder.create(taskContext);
        BuildLogger buildLogger = taskContext.getBuildLogger();

        GitVersionInput input = readConfiguration(taskContext);
        GitVersionOutput output = executeGitVersion(input, buildLogger);
        if(!output.Succeeded)
        {
            buildLogger.addErrorLogEntry(output.ErrorMessage, output.Exception);
            return resultBuilder.failed().build();
        }

        Map<String, String> buildMetadata = taskContext.getBuildContext().getBuildResult().getCustomBuildData();
        saveVariables(output.Variables, buildMetadata, buildLogger);

        return resultBuilder.success().build();
    }

    private GitVersionOutput executeGitVersion(GitVersionInput input, BuildLogger buildLogger)
    {
        StringOutputHandler processOutputHandler = new StringOutputHandler();
        ExternalProcess gitVersionProcess = buildGitVersionProcess(input, processOutputHandler);
        buildLogger.addBuildLogEntry(String.format("Executing %s", gitVersionProcess.getCommandLine()));
        gitVersionProcess.execute();

        return new GitVersionOutput(gitVersionProcess.getHandler(), processOutputHandler);
    }

    private GitVersionInput readConfiguration(TaskContext taskContext)
    {
        String repoPath =  taskContext.getConfigurationMap().get(GitVersionTaskConfigurator.REPO_PATH);
        Path repo = taskContext.getWorkingDirectory().toPath().resolve(repoPath);
        return new GitVersionInput(repo.toString());
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
    private String getGitVersionCapability()
    {
        return capabilityContext.getCapabilityValue(GitVersionCapability.CAPABILITY_KEY);
    }

    private ExternalProcess buildGitVersionProcess(GitVersionInput input, OutputHandler outputHandler)
    {
        String gitversionExecutable = getGitVersionCapability();

        final PluggableProcessHandler handler = new BambooProcessHandler(outputHandler, outputHandler);
        ExternalProcess process = new ExternalProcessBuilder()
                .command(Lists.newArrayList(gitversionExecutable, input.Path))
                .handler(handler)
                .build();

        return process;
    }

    private class GitVersionInput
    {
        public final String Path;

        public GitVersionInput(String path)
        {
            Path = path;
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
                return;
            }
        }
    }
}