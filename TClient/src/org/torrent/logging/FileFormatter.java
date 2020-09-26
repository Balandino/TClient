package org.torrent.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A custom formatter that directs logs to an output file
 * @author mkg
 *
 */
public class FileFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		//Pulled and altered from: https://stackoverflow.com/questions/53211694/change-color-and-format-of-java-util-logging-logger-output-in-eclipse
			    
		StringBuilder builder = new StringBuilder();
        builder.append("[" + calcDate(record.getMillis()) + "]");
        builder.append(String.format("%-15s", new String(" [" + record.getSourceClassName().substring(record.getSourceClassName().lastIndexOf(".") + 1) + "]")));
        builder.append(String.format(" %-10s", new String(" [" + record.getLevel().getName()) + "]"));
        builder.append(" - " + record.getMessage());
        
        
        
        
        Object[] params = record.getParameters();
        if (params != null){
            builder.append("\t");
            for (int i = 0; i < params.length; i++) {
                builder.append(params[i]);
                if (i < params.length - 1) {
                    builder.append(", ");
                }
            }
        }
        
        
        builder.append(System.lineSeparator());
        return builder.toString();
    }
	
    private String calcDate(long millisecs) {
        SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date resultdate = new Date(millisecs);
        return date_format.format(resultdate);
    }

    
   
}
	
	
	


