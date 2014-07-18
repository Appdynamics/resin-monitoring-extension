# AppDynamics Resin Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case

Resin is a high-performance, scalable Java application server. This eXtension monitors Resin instance and collects useful statistics exposed through MBeans and reports to AppDynamics Controller.

##Prerequisites

To use this extension please configure JMX in your instance. For information on enabling JMX, please refer to [http://caucho.com/resin-4.0/admin/resin-admin-console.xtp#JMXConsole] (http://caucho.com/resin-4.0/admin/resin-admin-console.xtp#JMXConsole).

<b>Example Configuration</b>
  '''
  <server-multi id-prefix="app-" address-list="${app_servers}" port="6800">
    <jvm-arg>-Dcom.sun.management.jmxremote.port=9999</jvm-arg>
    <jvm-arg>-Dcom.sun.management.jmxremote.ssl=false</jvm-arg>
    <jvm-arg>-Dcom.sun.management.jmxremote.password.file=/home/AppDynamics/Resin/jmxremote.password</jvm-arg>
  </server-multi>
  '''

##Installation

1. Run 'mvn clean install' from the resin-monitoring-extension directory and find the ResinMonitor.zip in the 'target' directory.
2. Unzip ResinMonitor.zip and copy the 'ResinMonitor' directory to `<MACHINE_AGENT_HOME>/monitors/`
3. Configure the extension by referring to the below section.
5. Restart the Machine Agent.

In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | Resin in case of default metric path

## Configuration

Note : Please make sure not to use tab (\t) while editing yaml files. You can validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the Resin Extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ResinMonitor/`.
2. Specify the Resin instance host, JMX port, username and password in the config.yml. Configure the MBeans for this extension to report the metrics to Controller. By default, "resin" is the domain name. Specify the keyproperty 'type' of MBeans you are interested. Resin MBean ObjectName is of the form 'resin:name=name,type=type,Host=name,WebApp=name'. Please refer [here](http://caucho.com/resin-4.0/admin/resin-admin-console.xtp#ResinsJMXInterfaces) for detailed Resin MBean Names.
You can also add excludePatterns (regex) to exclude any metric tree from showing up in the AppDynamics controller.

   For eg.
   ```
        # Resin instance
        server:
            host: "localhost"
            port: 9999
            username: "monitorRole"
            password: "admin"
            

        # Resin MBeans
          mbeans:
              domainName: "resin"
              types: [CacheStore,ClusterServer,ConnectionPool,Memory,Server,ThreadPool,TransactionManager,WebApp]
              # Uses regular expressions to match the pattern
              excludePatterns: [.*ClusterIndex$]
                  
          
          #prefix used to show up metrics in AppDynamics
          metricPrefix:  "Custom Metrics|Resin|"

   ```
   In the above config file, metrics are being pulled from the specified MBeans.
   Note that the patterns mentioned in the "excludePatterns" will be excluded from showing up in the AppDynamics Metric Browser.


3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/ResinMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/ResinMonitor/config.yml" />
          ....
     </task-arguments>
    ```



##Metrics



## Custom Dashboard


##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere]() community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).

