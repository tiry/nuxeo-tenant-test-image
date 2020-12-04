/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.runtime.metrics.reporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.runtime.api.Framework;

import com.google.api.MonitoredResource;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.opencensus.common.Duration;
import io.opencensus.contrib.dropwizard5.DropWizardMetrics;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.Metrics;

/**
 * Reports metrics to Google Stackdriver.
 *
 * @since 11.4
 */
public class K8StackdriverReporter extends StackdriverReporter {

    private static final Logger log = LogManager.getLogger(K8StackdriverReporter.class);

    protected String getValue(String path) {
    	try {
			return FileUtils.readFileToString(new File(path), Charset.defaultCharset());
		} catch (IOException e) {
			log.error("Unable to read " + path, e);
		}
    	
    	return null;  	
    }
    
    protected Map<LabelKey, LabelValue> getLabels(String projectId) {
    	    	
    	Map<LabelKey, LabelValue> labels = new HashMap<>();
    	
    	labels.put(
    			LabelKey.create("pod_name", "Pod name"),
    			LabelValue.create(getValue("/etc/podinfo/podname")));

    	labels.put(
    			LabelKey.create("namespace_name", "Namespace"),
    			LabelValue.create(getValue("/etc/podinfo/namespace")));

    	labels.put(
    			LabelKey.create("project_id", "ProjectId"),
    			LabelValue.create(projectId));

    	labels.put(
    			LabelKey.create("cluster_name", "cluster-name"),
    			LabelValue.create(options.get("cluster-name")));

    	labels.put(
    			LabelKey.create("location", "Cluster location"),
    			LabelValue.create(options.get("cluster-location")));

    	for (LabelKey key : labels.keySet()) {
    		System.out.println(key.getKey() + " : '"  + labels.get(key).getValue() + "'");
    	}
    	
    	return labels;
    	
    }
    
    protected Map<String, String> getLabelsAsMap(String projectId) {
    	Map<LabelKey, LabelValue> labels = getLabels(projectId);
    	Map<String, String> result = new HashMap<>();
    	for (LabelKey key : labels.keySet()) {
    		result.put(key.getKey(),labels.get(key).getValue());
    	}
    	return result;
    }
        
    
    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        log.warn("Creating Stackdriver metrics reporter");
        DropWizardMetrics registries = new DropWizardMetrics(Collections.singletonList(registry), filter);
        Metrics.getExportComponent().getMetricProducerManager().add(registries);
        Duration timeout = Duration.create(
                DurationUtils.parsePositive(options.get(TIMEOUT_OPTION), DEFAULT_TIMEOUT).getSeconds(), 0);
        String projectId = StackdriverTraceReporter.getGcpProjectId(options);
        
        
        String prefix = options.getOrDefault(PREFIX_OPTION, DEFAULT_PREFIX);
        Duration interval = Duration.fromMillis(getPollInterval() * 1000);
        
        // k8s_pod vs k8s_container
        String forceResourceType = Framework.getProperty("nuxeo.stackdriver.forceResourceType");
        
        MonitoredResource resource=null;
        if (forceResourceType!=null) {
        	 resource =
             	    MonitoredResource.newBuilder().setType(forceResourceType).putAllLabels(getLabelsAsMap(projectId)).build(); 	
        }
        
        StackdriverStatsConfiguration.Builder builder = StackdriverStatsConfiguration.builder()
                .setDeadline(timeout)
                .setProjectId(projectId)
                .setMetricNamePrefix(prefix)
                .setConstantLabels(getLabels(projectId))
                .setExportInterval(interval);
        
        if (resource!=null) {
        	builder.setMonitoredResource(resource);            
        }
        
        StackdriverStatsConfiguration configuration = builder.build();   
        
        try {
            StackdriverStatsExporter.createAndRegister(configuration);
        } catch (IOException e) {
            log.error("Fail to create a Stackdriver metric reporter", e);
            return;
        }
        activated = true;
    }

}
