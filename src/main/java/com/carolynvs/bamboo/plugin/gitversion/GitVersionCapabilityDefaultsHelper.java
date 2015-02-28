package com.carolynvs.bamboo.plugin.gitversion;

import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityImpl;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilitySet;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class GitVersionCapabilityDefaultsHelper implements CapabilityDefaultsHelper
{
    @NotNull
    @Override
    public CapabilitySet addDefaultCapabilities(CapabilitySet capabilitySet)
    {
        String programData = System.getenv("PROGRAMDATA");
        if(programData != null)
        {
            File defaultPath = new File(programData, "Chocolatey\\Bin\\GitVersion.exe");
            if(defaultPath.exists())
            {
                CapabilityImpl capability = new CapabilityImpl(GitVersionTask.CAPABILITY_KEY, defaultPath.getAbsolutePath());
                capabilitySet.addCapability(capability);
            }
        }

        return capabilitySet;
    }
}
