# RC_teorical project 2 - Go Back N

This program simulates reliable data transmission using the Go-Back-N protocol. It demonstrates how a sender can transmit multiple frames within a sliding window, ensuring data integrity through acknowledgment and retransmission mechanisms. If a frame is lost or corrupted, the sender retransmits that frame and all subsequent frames in the window, mimicking real-world network behavior.

### Run configs/commands

`javac -cp .:cnss-classes -d ft21-classes src/*/*/*.java src/*/*.java src/*.java`

`java -cp .:cnss-classes:ft21-classes cnss.simulator.Simulator configs/config-2.1.txt > results.txt && cat results.txt`
