import java.nio.file.Paths;

import org.torrent.bencoding.TorrentFile.PieceSelectionPolicy;
import org.torrent.nio.TClient;

public class Tester {

	public static void main(String[] args) {
		TClient client = new TClient();
		client.downloadFile(Paths.get("./Arch ISO/archlinux-2020.07.01-x86_64.iso.torrent"), 
							Paths.get("./Output/Arch-ISO.iso"), 
							PieceSelectionPolicy.RarestFirst);
	}

}
