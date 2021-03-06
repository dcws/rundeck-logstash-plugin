import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.logging.LogLevel;

/**
 * Opens a TCP connection, and writes JSON event messages to the socket
 */
rundeckPlugin(StreamingLogWriterPlugin){
    configuration{
        host defaultValue:"localhost", required:true, description: "Hostname to connect to"
        port required:true, description: "Port to connect to", type: 'Integer'
        logtype defaultValue:"rundeck", required: true, description: "The type to set"
    }
    /**
     * open the socket, prepare some metadata
     */
    open { Map execution, Map config ->
        def socket = new Socket(config.host, config.port.toInteger());
        def socketStream = socket.getOutputStream();
        def e2 = [:]
        execution.each{ e2["execution.${it.key}"]=it.value }
        def json=new ObjectMapper()
        
        [socket:socket, count:0, executionid:execution.execid, write:{
            socketStream<< json.writeValueAsString(e2 + it) + "\n"            
        }, logtype:config.logtype]
    }

    /**
     * write the log event and metadata as json to the socket
     */
    addEvent { Map context, LogEvent event->
        
        context.count++
        
        def emeta=[:]
        
        event.metadata?.each{ emeta["event.${it.key}"]=it.value }

        def data= emeta + [
            type:context.logtype,
            line:context.count,
            datetime:event.datetime.time,
            loglevel:event.loglevel.toString(),
            message:event.message,
            eventType:event.eventType,
        ]

        context.write data
    }
    /**
     * close the socket
     */
    close { 
        context.write ending:true, totallines:context.count, message: 'Execution '+context.executionid+' finished.'
        context.socket.close();
    }
}
