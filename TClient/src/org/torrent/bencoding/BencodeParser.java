package org.torrent.bencoding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * This class parses Bencoding and returns a HashMap of the values.  Due to how the parsing occurs, the HashMap contains Object values which trigger warnings.
 * 
 * @author mkg
 *
 */
@SuppressWarnings("unchecked")
public abstract class BencodeParser {
	
	// Hide constructor
	private BencodeParser() {
		
	}
	
/**
 * Parses the Bencoded argument
 * 
 * @param bencodedBytes Bencoded bytes 
 * @return A HashMap of the values and keys stored in the Bencoded argument, with values stored as Objects
 */
public static HashMap<String, Object> parseBencoding(byte[] bencodedBytes) {
	return (HashMap<String, Object>)BencodeParser.getNextToken(bencodedBytes, Integer.valueOf(0), Integer.valueOf(0), false, "UTF-8")[0];
}
	
/**
 * Reads the Bencoded bytes from the argument file and parses the resultant bytes
 * 	
 * @param bencodedFile A Bencoded file
 * @return A HashMap of the values and keys stored in the Bencoded argument, with values stored as Objects
 * @throws IOException If an error occurs reading the file
 */
public static HashMap<String, Object> parseBencoding(Path bencodedFile) throws IOException {
		return (HashMap<String, Object>)BencodeParser.getNextToken(Files.readAllBytes(bencodedFile), Integer.valueOf(0), Integer.valueOf(0), false, "UTF-8")[0];
}

	private static Object[] getNextToken(byte[] torrentBytes, Integer laggingIndex, Integer leadingIndex, boolean piecesEncodingFlag, String piecesEncoding) {
		
		//Progress
		//System.out.printf("Bytes Processed: %d/%d%s", leadingIndex,  torrentBytes.length -1, (torrentBytes.length -1) == leadingIndex ? System.lineSeparator() : "\r");
		
		while(leadingIndex < torrentBytes.length) {
			String nextCharacter = new String(new byte[] {torrentBytes[leadingIndex]}, StandardCharsets.UTF_8);
			if(nextCharacter.matches(":|i|d|l|e")) {
				switch(nextCharacter) {
								
					case ":":
						Integer tokenLength = Integer.valueOf(new String(Arrays.copyOfRange(torrentBytes, laggingIndex, leadingIndex), StandardCharsets.US_ASCII));
						laggingIndex = leadingIndex += 1; //Move lagging index past colon
						leadingIndex += tokenLength; //Leading index is now the correct distance ahead
						
						String tokenToReturn = null;
						
						if(tokenLength == 0) {
							tokenToReturn = "";
						} else {
							// If we are parsing the pieces, then return the bytes only for later processing
							if(piecesEncodingFlag) {
								//tokenToReturn = new String(Arrays.copyOfRange(torrentBytes, laggingIndex, leadingIndex), Charset.forName(piecesEncoding));
								piecesEncodingFlag = false;
								return new Object[] {Arrays.copyOfRange(torrentBytes, laggingIndex, leadingIndex), leadingIndex, piecesEncodingFlag, piecesEncoding};
							} else {
								tokenToReturn = new String(Arrays.copyOfRange(torrentBytes, laggingIndex, leadingIndex), StandardCharsets.UTF_8);
							}
						}
						
						
						// On the next run through ":" we will be processing the pieces
						if(tokenToReturn.equals("pieces")) {
							piecesEncodingFlag = true;
						}
						
						return new Object[] {tokenToReturn, leadingIndex, piecesEncodingFlag, piecesEncoding};
						
						 
					case "i":
						while(!new String(new byte[] {torrentBytes[leadingIndex]}, StandardCharsets.UTF_8).equals(new String(new byte[] {101}, StandardCharsets.UTF_8))){
							leadingIndex++;
						}
						
						
						laggingIndex++; // Move past i.  the number should now be between the indexes
						// Long number = Long.valueOf(new String(Arrays.copyOfRange(torrentBytes, laggingIndex, leadingIndex), StandardCharsets.US_ASCII)); //Long to handle large sizes
						
						
						String numberToConvert = new String(Arrays.copyOfRange(torrentBytes, laggingIndex, leadingIndex), StandardCharsets.US_ASCII);
						
						// Throws exception if invalid number appears
						if(numberToConvert.startsWith("00") || numberToConvert.startsWith("-0")) {
							throw new NumberFormatException();
						}
						
						Long number = Long.valueOf(numberToConvert); //Long to handle large sizes
						
						leadingIndex++; // Move leading index past 'e' for Integer
						return new Object[] {number, leadingIndex};
						
					case "l":
											
						laggingIndex = leadingIndex += 1; // Move past l
						List<Object> newList = new LinkedList<Object>();
						
						while(true) {
							Object[] newToken = BencodeParser.getNextToken(torrentBytes, laggingIndex, leadingIndex, piecesEncodingFlag, piecesEncoding);
							
							if(newToken[0] instanceof String && newToken[0].equals(new String(new byte[] {101}, StandardCharsets.UTF_8))) {
								return new Object[] {newList, newToken[1]};
							} else {
								laggingIndex = leadingIndex = (Integer)newToken[1];
								newList.add(newToken[0]);
							}
						}
						
					case "d":
						HashMap<String, Object> newDict = new HashMap<String, Object>();
						laggingIndex = leadingIndex += 1; // Move past d
						
						while(true) {
							Object[] tokens = new Object[2];
							for (int i = 0; i < 2; i++) {
								Object[] newToken = BencodeParser.getNextToken(torrentBytes, laggingIndex, leadingIndex, piecesEncodingFlag, piecesEncoding); 
								if(i == 0 && newToken[0].equals(new String(new byte[] {101}, StandardCharsets.UTF_8))) {
									return new Object[] {newDict, newToken[1]};
								} else {
									if(newToken.length == 4) {
										piecesEncodingFlag = (boolean)newToken[2];
										piecesEncoding = (String)newToken[3];
									}
									laggingIndex = leadingIndex = (Integer)newToken[1]; //Indexes now reset for next token
									tokens[i] = newToken[0];
								}
							}
							// Set encoding for pieces if one is set
							if(((String)tokens[0]).equals("encoding")) {
								piecesEncoding = (String)tokens[1];
							}
							newDict.put((String)tokens[0], tokens[1]);
						}
					
					case "e":
						laggingIndex = leadingIndex += 1; //Move indexes past "e"
						return new Object[] {nextCharacter, leadingIndex};
					}
				}
				leadingIndex++;
		}
		// Shouldn't reach here
		return new Object[] {};
	}
}
