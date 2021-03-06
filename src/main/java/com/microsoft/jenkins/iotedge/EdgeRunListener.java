package com.microsoft.jenkins.iotedge;

import com.microsoft.jenkins.iotedge.util.Constants;
import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.tasks.BuildStep;

import java.util.HashMap;
import java.util.Map;

@Extension
public class EdgeRunListener extends RunListener<Run<?, ?>> {
    Map<Run, Long> timeMap;

    public EdgeRunListener() {
        timeMap = new HashMap<>();
    }
    @Override
    public void onStarted(Run r, TaskListener listener) {
        if(System.getenv(Constants.JENKINS_TEST_ENVIRONMENT_ENV_KEY) != "true") {
            timeMap.put(r, System.currentTimeMillis());
        }
    }

    @Override
    public void onCompleted(Run<?, ?> r, TaskListener listener) {
        if(System.getenv(Constants.JENKINS_TEST_ENVIRONMENT_ENV_KEY) != "true") {
            String x = r.getDescription();
            long start = timeMap.get(r);
            long span = (System.currentTimeMillis() - start) / 1000;
            boolean success = r.getResult() == Result.SUCCESS;
            AzureIoTEdgePlugin.sendEvent(success, span, r.getParent().getRootDir().getAbsolutePath(), r.getId(), r.getClass().getSimpleName());
        }
    }
}
