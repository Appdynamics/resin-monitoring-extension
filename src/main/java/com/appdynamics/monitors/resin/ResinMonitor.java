package com.appdynamics.monitors.resin;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.jmx.JMXConnectionConfig;
import com.appdynamics.extensions.jmx.JMXConnectionUtil;
import com.appdynamics.monitors.resin.config.ConfigUtil;
import com.appdynamics.monitors.resin.config.Configuration;
import com.appdynamics.monitors.resin.config.MBeanData;
import com.appdynamics.monitors.resin.config.ResinMBeanKeyPropertyEnum;
import com.appdynamics.monitors.resin.config.ResinMonitorConstants;
import com.appdynamics.monitors.resin.config.Server;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanAttributeInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.apache.log4j.Logger;

public class ResinMonitor extends AManagedMonitor {
    private static final Logger logger = Logger.getLogger(ResinMonitor.class);

    public static final String METRICS_SEPARATOR = "|";
    private static final String CONFIG_ARG = "config-file";
    private static final String FILE_NAME = "monitors/ResinMonitor/config.yml";

    private static final ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();

    public ResinMonitor() {
        String details = ResinMonitor.class.getPackage().getImplementationTitle();
        String msg = "Using Monitor Version [" + details + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        if (taskArgs != null) {
            logger.info("Starting the Resin Monitoring task.");
            String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
            try {
                Configuration config = configUtil.readConfig(configFilename, Configuration.class);
                Map<String, Number> metrics = populateStats(config);
                printStats(config, metrics);
                logger.info("Completed the Resin Monitoring Task successfully");
                return new TaskOutput("Resin Monitor executed successfully");
            } catch (FileNotFoundException e) {
                logger.error("Config File not found: " + configFilename, e);
            } catch (Exception e) {
                logger.error("Metrics Collection Failed: ", e);
            }
        }
        throw new TaskExecutionException("Resin Monitor completed with failures");
    }

    private Map<String, Number> populateStats(Configuration config) throws Exception {
        JMXConnectionUtil jmxConnector = null;
        Map<String, Number> metrics = new HashMap<String, Number>();
        Server server = config.getServer();
        MBeanData mbeanData = config.getMbeans();
        try {
            jmxConnector = new JMXConnectionUtil(new JMXConnectionConfig(server.getHost(), server.getPort(), server.getUsername(),
                    server.getPassword()));
            if (jmxConnector != null && jmxConnector.connect() != null) {
                Set<ObjectInstance> allMbeans = jmxConnector.getAllMBeans();
                if (allMbeans != null) {
                    metrics = extractMetrics(jmxConnector, mbeanData, allMbeans);
                    metrics.put(ResinMonitorConstants.METRICS_COLLECTED, ResinMonitorConstants.SUCCESS_VALUE);
                }
            }
        } catch (Exception e) {
            logger.error("Error JMX-ing into Resin Server ", e);
            metrics.put(ResinMonitorConstants.METRICS_COLLECTED, ResinMonitorConstants.ERROR_VALUE);
        } finally {
            jmxConnector.close();
        }
        return metrics;
    }

    private Map<String, Number> extractMetrics(JMXConnectionUtil jmxConnector, MBeanData mbeanData, Set<ObjectInstance> allMbeans) {
        Map<String, Number> metrics = new HashMap<String, Number>();
        Set<String> excludePatterns = mbeanData.getExcludePatterns();
        for (ObjectInstance mbean : allMbeans) {
            ObjectName objectName = mbean.getObjectName();
            if (isDomainAndKeyPropertyConfigured(objectName, mbeanData)) {
                MBeanAttributeInfo[] attributes = jmxConnector.fetchAllAttributesForMbean(objectName);
                if (attributes != null) {
                    for (MBeanAttributeInfo attr : attributes) {
                        if (attr.isReadable()) {
                            Object attribute = jmxConnector.getMBeanAttribute(objectName, attr.getName());
                            if (attribute != null && attribute instanceof Number) {
                                String metricKey = getMetricsKey(objectName, attr);
                                if (!isKeyExcluded(metricKey, excludePatterns)) {
                                    metrics.put(metricKey, (Number)attribute);
                                } else {
                                    if (logger.isDebugEnabled()) {
                                        logger.info(metricKey + " is excluded");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return metrics;
    }

    private boolean isKeyExcluded(String metricKey, Set<String> excludePatterns) {
        for (String excludePattern : excludePatterns) {
            if (metricKey.matches(escapeText(excludePattern))) {
                return true;
            }
        }
        return false;
    }

    private String escapeText(String excludePattern) {
        return excludePattern.replaceAll("\\|", "\\\\|");
    }

    private boolean isDomainAndKeyPropertyConfigured(ObjectName objectName, MBeanData mbeanData) {
        String domain = objectName.getDomain();
        String keyProperty = objectName.getKeyProperty(ResinMBeanKeyPropertyEnum.TYPE.toString());
        Set<String> types = mbeanData.getTypes();
        boolean configured = mbeanData.getDomainName().equals(domain) && types.contains(keyProperty);
        return configured;
    }

    private String getMetricsKey(ObjectName objectName, MBeanAttributeInfo attr) {
        String type = objectName.getKeyProperty(ResinMBeanKeyPropertyEnum.TYPE.toString());
        String host = objectName.getKeyProperty(ResinMBeanKeyPropertyEnum.HOST.toString());
        String webApp = objectName.getKeyProperty(ResinMBeanKeyPropertyEnum.WEBAPP.toString());
        String name = objectName.getKeyProperty(ResinMBeanKeyPropertyEnum.NAME.toString());

        StringBuilder metricsKey = new StringBuilder();
        metricsKey.append(Strings.isNullOrEmpty(type) ? "" : type + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(host) ? "" : host + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(webApp) ? "" : webApp + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(name) ? "" : name + METRICS_SEPARATOR);
        metricsKey.append(attr.getName());

        return metricsKey.toString();
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }

        if ("".equals(filename)) {
            filename = FILE_NAME;
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }

    private void printStats(Configuration config, Map<String, Number> metrics) {
        String metricPath = config.getMetricPrefix();
        for (Map.Entry<String, Number> entry : metrics.entrySet()) {
            printMetric(metricPath + entry.getKey(), entry.getValue());
        }
    }

    private void printMetric(String metricPath, Number metricValue) {
        printMetric(metricPath, metricValue, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
    }

    private void printMetric(String metricPath, Number metricValue, String aggregation, String timeRollup, String cluster) {
        MetricWriter metricWriter = super.getMetricWriter(metricPath, aggregation, timeRollup, cluster);
        if (metricValue != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Metric [" + metricPath + " = " + metricValue + "]");
            }
            if (metricValue instanceof Double) {
                metricWriter.printMetric(String.valueOf(Math.round((Double) metricValue)));
            } else if (metricValue instanceof Float) {
                metricWriter.printMetric(String.valueOf(Math.round((Float) metricValue)));
            } else {
                metricWriter.printMetric(String.valueOf(metricValue));
            }
        }
    }

    public static void main(String[] args) throws TaskExecutionException {

        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put(CONFIG_ARG, "/home/satish/AppDynamics/Code/extensions/resin-monitoring-extension/src/main/resources/config/config.yml");

        ResinMonitor resintMonitor = new ResinMonitor();
        resintMonitor.execute(taskArgs, null);
    }
}
