import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Yet another json layout for log4j. This one has the ability to include JVM and ENV variables, as well as host 
 * information.
 */
public class JsonLayout extends Layout {
    private static final Gson gson = new GsonBuilder().create();
    private static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    
    // a list of environment properties to include
    private String envPropertyList = "";
    private String[] envProperties = {};
    
    private String jvmPropertyList = "";
    private String[] jvmProperties = {};
    
    private String includeHostStr = Boolean.FALSE.toString();
    private boolean hostSet = false;
    private String cname;
    private String ipaddr;
    
    private boolean shouldLogSlowProperties = false;
    private String logSlowProperties;
    
    private boolean pretty = false;
    
    @Override
    public String format(LoggingEvent event) {
        
        event.getMDCCopy();
        
        JsonObject root = new JsonObject();
        root.addProperty("id", UUIDGen.getTimeUUID().toString());
        root.addProperty("level", event.getLevel().toString());
        root.addProperty("category", event.getLoggerName());
        root.addProperty("priority", event.getLevel().toString());
        root.addProperty("thread", event.getThreadName());
        root.addProperty("timestamp", event.getTimeStamp());
        root.addProperty("message", event.getMessage().toString());
        root.addProperty("ndc", event.getNDC());
        
        // these properties are exensive. only log them if explicitly set or we have an exception.
        if (shouldLogSlowProperties || event.getThrowableInformation() != null) {
            root.addProperty("class", event.getLocationInformation().getClassName());
            root.addProperty("file", event.getLocationInformation().getFileName());
            root.addProperty("line", event.getLocationInformation().getLineNumber());
            root.addProperty("method", event.getLocationInformation().getMethodName());
        }
        
        // fold in MDC, JVM and ENV properties
        mergeMdcProperties(event, root);
        mergeJvmProperties(event, root, jvmProperties);
        mergeEnvProperties(event, root, envProperties);
        
        // include host details.
        if (hostSet) {
            root.addProperty("host_cname", cname);
            root.addProperty("host_ip", ipaddr);
        }

        // if there was an exception.
        ThrowableInformation exinfo = event.getThrowableInformation();
        if (exinfo != null) {
            StringBuilder sb = new StringBuilder();
            // flatten that puppy.
            for (String str : exinfo.getThrowableStrRep()) {
                sb = sb.append(str).append("\n");
            }
            root.addProperty("exception_trace", sb.toString());
        }
        
        // let subclasses muck with things.
        augmentJson(event, root);
        
        if (pretty) {
            return prettyGson.toJson(root) + "\n";
        } else {
            return gson.toJson(root) + "\n";
        }
    }
    
    // override this to augment behavior.
    protected void augmentJson(LoggingEvent event, JsonObject json) {}
    
    private static void mergeJvmProperties(LoggingEvent event, JsonObject obj, String[] properties) {
        for (String key : properties) {
            String value = System.getProperty(key);
            if (value != null) {
                obj.addProperty("jvm_" + key, value);
            }
        }
    }
    
    private static void mergeEnvProperties(LoggingEvent event, JsonObject obj, String[] properties) {
        for (String key : properties) {
            String value = System.getenv(key);
            if (value != null) {
                obj.addProperty("env_" + key, value);
            }
        }
    }
    
    private static void mergeMdcProperties(LoggingEvent event, JsonObject obj) {
        for (Object key : event.getPropertyKeySet()) {
            String keyString = key.toString();
            String value = event.getProperty(keyString);
            if (value != null) {
                obj.addProperty(keyString, value);
            }
        }
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public boolean ignoresThrowable() {
        return false;
    }

    // no real need for this since options do not depend on each other.
    public void activateOptions() {}
    
    //
    // My stuff
    //

    public String getEnvPropertyList() {
        return envPropertyList;
    }

    public void setEnvPropertyList(String envPropertyList) {
        this.envPropertyList = envPropertyList;
        envProperties = safeStringToArray(envPropertyList);
    }

    public String getJvmPropertyList() {
        return jvmPropertyList;
    }

    public void setJvmPropertyList(String jvmPropertyList) {
        this.jvmPropertyList = jvmPropertyList;
        jvmProperties = safeStringToArray(jvmPropertyList);
    }

    public String getIncludeHost() {
        return includeHostStr;
    }

    public void setIncludeHost(String includeHostStr) {
        hostSet = false;
        this.includeHostStr = includeHostStr;
        if (Boolean.parseBoolean(includeHostStr)) {
            try {
                InetAddress addr = InetAddress.getLocalHost();
                if (addr != null) {
                    cname = addr.getCanonicalHostName();
                    ipaddr = addr.getHostAddress();
                    hostSet = true;
                }
            } catch (Exception ex) {
                // guh.
            }
        }
    }

    public String getLogSlowProperties() {
        return logSlowProperties;
    }

    public void setLogSlowProperties(String logSlowProperties) {
        this.logSlowProperties = logSlowProperties;
        shouldLogSlowProperties = Boolean.parseBoolean(logSlowProperties);
    }

    public boolean isPretty() {
        return pretty;
    }

    public void setPretty(boolean pretty) {
        this.pretty = pretty;
    }

    // make sure all the keys are good (not null, not empty, etc.).
    private static String[] safeStringToArray(String str) {
        if (str == null || str.trim().length() == 0) {
            return new String[] {};
        }
        List<String> good = new ArrayList<String>();
        String[] splits = str.split(",", -1);
        for (int i = 0; i < splits.length; i++) {
            if (splits[i] != null) {
                String key = splits[i].trim().replaceAll("\\s", "_");
                if (key.length() > 0) {
                    good.add(key);
                }
            }
        }
        
        return good.toArray(new String[good.size()]);
    }
}
