package org.torrent.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class ConsoleFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		//Pulled and altered from: https://stackoverflow.com/questions/53211694/change-color-and-format-of-java-util-logging-logger-output-in-eclipse
			    
		
		String colour = COLOUR_CODES[record.getLevel().intValue() / 100];
		
        
        StringBuilder builder = new StringBuilder();
        builder.append(colour);
        builder.append("[" + calcDate(record.getMillis()) + "]");
        builder.append(String.format("%-15s", new String(" [" + record.getSourceClassName().substring(record.getSourceClassName().lastIndexOf(".") + 1) + "]")));
        builder.append(String.format(" %-10s", new String(" [" + record.getLevel().getName()) + "]"));
        builder.append(ANSI_WHITE + " - " + record.getMessage());
        
        
        
        
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
        
        builder.append(ANSI_RESET);
        builder.append(System.lineSeparator());
        return builder.toString();
    }
	
    private String calcDate(long millisecs) {
        SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date resultdate = new Date(millisecs);
        return date_format.format(resultdate);
    }

    
    // ANSI escape code
    public final String ANSI_RESET = "\u001B[0m";
    public final String ANSI_BLACK = "\u001B[30m";
    public final String ANSI_RED = "\u001B[31m";
    public final String ANSI_GREEN = "\u001B[32m";
    public final String ANSI_YELLOW = "\u001B[33m";
    public final String ANSI_BLUE = "\u001B[34m";
    public final String ANSI_PURPLE = "\u001B[35m";
    public final String ANSI_CYAN = "\u001B[36m";
    public final String ANSI_WHITE = "\u001B[37m";
    
    private final String[] COLOUR_CODES= new String[] {"", "", "", ANSI_WHITE, ANSI_PURPLE, ANSI_BLUE, "", ANSI_CYAN, ANSI_GREEN, ANSI_YELLOW, ANSI_RED};
}
	
	
	


