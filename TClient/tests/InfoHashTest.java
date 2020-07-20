import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;

class InfoHashTest {

	@Test
	void test() throws NoSuchAlgorithmException, IOException {
	
		String bFile = Files.readString(Paths.get("/home/mkg/Desktop/debian-10.4.0-amd64-netinst.iso.torrent"), StandardCharsets.ISO_8859_1);
		bFile = bFile.substring(bFile.lastIndexOf("4:info") + 6, bFile.length() - 1);
		
		
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] hash = md.digest(bFile.getBytes(StandardCharsets.ISO_8859_1));
		
		String infoHash = InfoHashTest.urlEncode(hash);
			
		System.out.println("Target: %14%7F%BB%21P%E6-%9A%B1%B2%8C%05%B2%25%17%92n%27%C8%82");
		System.out.println("Result: " + infoHash);
		assertEquals("%14%7F%BB%21P%E6-%9A%B1%B2%8C%05%B2%25%17%92n%27%C8%82", infoHash.toString());
	}
	
	
	private static String urlEncode(byte[] array) {
		StringBuilder encodedString = new StringBuilder(20);
		
		//Pulled from: https://stackoverflow.com/questions/11894945/convert-torrent-info-hash-from-bencoded-to-urlencoded-data
		for(byte entry : array) {
			if(entry == ' ') {
				encodedString.append("+");
			} else if(entry == '.' || entry == '-' || entry == '_' || entry == '~' || (entry >= 'a' && entry <= 'z') || (entry >= 'A' && entry <= 'Z') || (entry >= '0' && entry <= '9')) { 
				encodedString.append(String.format("%c", entry));
			} else {
				encodedString.append(String.format("%%%02X", entry));
			}
		}
		return encodedString.toString();
	}

}
