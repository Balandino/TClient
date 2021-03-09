# TClient
The code here is the result of a successful university project carried out with the goal of implementing enough of the BitTorrent specification to download a file.  The code is written in pure Java with no external libraries and uses non-blocking I/O to handle multiple connections.  Coloured log statememnts will display information during the download process and a folder containing documentation has been included to better understand how the classes fit together.

The code is initiated by calling the Tester class and supplying an appropriate torrent file and output path for the destination file as demonstrated in the below screenshots:

![Image showing how to start the TClient program](/TClient/Screenshots/Beginning.png?raw=true "Initiate Download")
![Image showing the ending if the TClient program](/TClient/Screenshots/Ending.png?raw=true "Download Concluded")
