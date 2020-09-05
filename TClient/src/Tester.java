import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.torrent.bencoding.TorrentFile.PieceSelectionPolicy;
import org.torrent.nio.TClient;

public class Tester {

	public static void main(String[] args) throws IOException {
		
		
	
		TClient client = new TClient();
		
		Files.deleteIfExists(Paths.get("./Output/Debian-ISO.iso"));
		client.downloadFile(Paths.get("./Debian ISO/debian-10.4.0-amd64-netinst.iso.torrent"), Paths.get("./Output/Debian-ISO.iso"), PieceSelectionPolicy.RarestFirst);
		
		
	}

}
