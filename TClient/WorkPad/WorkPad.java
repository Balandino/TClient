import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.torrent.logging.FileFormatter;
import org.torrent.logging.ConsoleFormatter;

public class WorkPad {
	public static void main(String[] args) throws Exception {
		
	
		
		Logger logger = Logger.getLogger("TClient");
		
		// LOG this level to the log
        logger.setLevel(Level.CONFIG);
        logger.setUseParentHandlers(false);
        
        ConsoleHandler newHandler = new ConsoleHandler();
        newHandler.setLevel(Level.CONFIG);
        newHandler.setFormatter(new ConsoleFormatter());
        logger.addHandler(newHandler);
        
        FileHandler fileHandler = new FileHandler("/home/mkg/Desktop/Logs.txt");
        fileHandler.setFormatter(new FileFormatter());
        logger.addHandler(fileHandler);
        

        System.out.println("Logging level is: " + logger.getLevel());
        
        
        logger.log(Level.FINEST, "Test!");
        logger.log(Level.FINER, "Test!");
        logger.log(Level.FINE, "Test!");
        logger.log(Level.CONFIG, "Test!");
        logger.log(Level.INFO, "Test!");
        logger.log(Level.WARNING, "Test!");
        logger.log(Level.SEVERE, "Test!");
	    
	    
		
        
        
//		System.out.println(colourCodes[Level.FINEST.intValue() / 100] + "Finest" + ANSI_RESET);
//		System.out.println(colourCodes[Level.FINER.intValue() / 100] + "Finer" + ANSI_RESET);
//		System.out.println(colourCodes[Level.FINE.intValue() / 100] + "Fine" + ANSI_RESET);
//		System.out.println(colourCodes[Level.CONFIG.intValue() / 100] + "Config" + ANSI_RESET);
//		System.out.println(colourCodes[Level.INFO.intValue() / 100] + "Info" + ANSI_RESET);
//		System.out.println(colourCodes[Level.WARNING.intValue() / 100] + "Warning" + ANSI_RESET);
//		System.out.println(colourCodes[Level.SEVERE.intValue() / 100] + "Severe" + ANSI_RESET);
	}
	
	

	
	
	
	
	
	
}
	
	
	
