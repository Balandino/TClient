import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.torrent.bencoding.BencodeParser;

class BencodeParseTest {

	@SuppressWarnings("unchecked")
	@Test
	void test() {
		
		try {
			
			HashMap<String, Object> parsedFile = (HashMap<String, Object>)BencodeParser.parseTorrent(Paths.get("./Debian ISO/debian-10.4.0-amd64-netinst.iso.torrent"));
			
			assertEquals(5, parsedFile.size());
			
			HashMap<String, Object> info = (HashMap<String, Object>)parsedFile.get("info");
			
			assertEquals(4, info.size());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	



}
