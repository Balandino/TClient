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
			
			HashMap<String, Object> parsedFile = (HashMap<String, Object>)BencodeParser.parseTorrent(Paths.get("./Arch ISO/archlinux-2020.07.01-x86_64.iso.torrent"));
			
			assertEquals(5, parsedFile.size());
			
			HashMap<String, Object> info = (HashMap<String, Object>)parsedFile.get("info");
			
			assertEquals(4, info.size());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	



}
