import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.torrent.bencoding.TorrentFile.PieceSelectionPolicy;
import org.torrent.nio.TClient;

/**
 * 
 * @author mkg
 * Example class used to initiate and run a download
 */
public class Tester {

	public static void main(String[] args) throws IOException {
		
		
	
		TClient client = new TClient();
		
		
		if(args.length < 1) {
			System.out.println("Error: No torrent supplied!");
		} else {
			// Remove previous output
			Files.deleteIfExists(Paths.get("./Output/Debian-ISO.iso"));
			
			client.downloadFile(Paths.get(args[0]), Paths.get("../Output/Debian-ISO.iso"), PieceSelectionPolicy.RarestFirst);
			//client.downloadFile(Paths.get("./Debian ISO/debian-10.4.0-amd64-netinst.iso.torrent"), Paths.get("./Output/Debian-ISO.iso"), PieceSelectionPolicy.RarestFirst);

		}
		
	}

}
