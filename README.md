# Logging Investigation

<b>Goal:</b> A flexible platform-independent logging solution that supports full-text indexing.

<b>Pony:</b> Can easily include non-java based logs (e.g. syslog, etc.)

<b>Note:</b> If platforms must be considered, let's only consider the [ELK](https://www.elastic.co/products) (Elasticsearch, Logstash, Kibana
and [Cloudera](http://www.cloudera.com/content/www/en-us/products.html) technology stacks.

## Idea

Since most indexing products sport RESTful JSON APIs, let's focus on getting logs newline-delimited files of JSON objects.
From there, it should be relatively simple to push the file into an index using some sort of agent or shim.

## Log4J Appender

Log4J 2.x ships with a JSON appender. Oddly, the 1.x line (in more common use) does not.
Creating a Log4J appender is a straightforward exercise (see JsonLayout).
I chose [Gson](https://github.com/google/gson) to handle the JSON for this project, but Jackson would have worked just as well.

The appender extracts the following standard log4j fields:

* level
* category
* priority
* thread
* timestamp
* message
* java class
* java file
* source code line
* method name

Further, the appender can be configured to include <i>some</i> JVM and/or ENV properties.
(See resources/log4j.properties for examples).

MDC properties, if used, will be included in the JSON.

Finally, the appender can be configured to include the host name and IP address in log data.

## ELK

Note: Marvel is not required, but it is a useful tool for visualizing Elasticsearch performance.

### Prerequisites

1. Install Java 8

### Elasticsearch (Full-text search)

1. Install Elasticsearch


    wget https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/tar/elasticsearch/2.1.1/elasticsearch-2.1.1.tar.gz
    tar -xvzf elasticsearch-2.1.1.tar.gz
    sudo mv elasticsearch-2.1.1 /opt/
    
1. Modify the configuration

    vi /opt/elasticsearch-2.1.1/config/elasticsearch.yml

You'll need to modify the following config variables:
  * cluster.name
  * node.name
  * path.data
  * path.logs
  * network.host - set to 0.0.0.0 for the purpose of testing.
  
1. Start Elasticsearch


    /opt/elasticsearch-2.1.1/bin/elasticsearch
    
### Kibana + Marvel (Visualization)

1. Install Kibana


    wget https://download.elastic.co/kibana/kibana/kibana-4.3.1-linux-x64.tar.gz
    tar -xvzf kibana-4.3.1-linux-x64.tar.gz 
    sudo mv kibana-4.3.1-linux-x64 /opt/
    
1. Install the Marvel plugin


    /opt/kibana-4.3.1-linux-x64/bin/kibana plugin --install elasticsearch/marvel/latest
    
1. Start Kibana

    /opt/kibana-4.3.1-linux-x64/bin/kibana

1. Install the Marvel agent


    /opt/elasticsearch-2.1.1/bin/plugin list
    /opt/elasticsearch-2.1.1/bin/plugin install license
    /opt/elasticsearch-2.1.1/bin/plugin install marvel-agent

Then restart Elasticsearch

### Logstash (Log shipping + indexing)

1. Install Logstash

1. Create a simple configuration


    # this is the place where you tell logstash how to pull fields out of your log messages.
    
    input {
      file {
        path => "/mnt/hgfs/codes/not_saved/logtests/json_events.log"
        start_position => beginning
        codec => "json"
      }
    }
    
    # no need to filter, right?
    
    output {
      elasticsearch {}
      stdout {}
    }

1. Start Logstash


    /opt/logstash-2.1.1/bin/logstash -f /mnt/hgfs/codes/not_saved/logtests/src/main/resources/logstash.conf

## Cloudera

