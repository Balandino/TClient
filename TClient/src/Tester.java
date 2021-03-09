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
		
		
		if(args.length < 2) {
			System.out.println("Error: Torrent file and output location needed!");
		} else {
			// Remove previous output
			// Files.deleteIfExists(Paths.get("./Output/Debian-ISO.iso"));
			
			client.downloadFile(Paths.get(args[0]), Paths.get(args[1]), PieceSelectionPolicy.RarestFirst);
			//client.downloadFile(Paths.get("./Debian ISO/debian-10.4.0-amd64-netinst.iso.torrent"), Paths.get("./Output/Debian-ISO.iso"), PieceSelectionPolicy.RarestFirst);

		}
		
	}

}
