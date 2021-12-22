import cnss.simulator.Node;
import ft21.FT21_AckPacket;
import ft21.FT21_DataPacket;
import ft21.FT21_FinPacket;
import ft21.FT21_UploadPacket;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;


public class FT21SenderSR extends FT21SenderGBN {

    private final int RECEIVER = 1;
    private static final int DEFAULT_TIMEOUT = 1000;

    private SortedMap<Integer,Boolean> window;
    private SortedMap<Integer,Integer> timers; // might not need "now" value in there
    private State state;
    private int blocksize, windowsize, lastSeqNumber, windowBase, latestSeqN;
    private File file;
    private RandomAccessFile rFile;

    public FT21SenderSR() {
        super();
    }

    @Override
    public int initialise(int now, int node_id, Node nodeObj, String[] args) {
        try {
            this.file = new File(args[0]);
            this.rFile = new RandomAccessFile(file, "r");
            this.blocksize = Integer.parseInt(args[1]);
            this.windowsize = Integer.parseInt(args[2]);
            this.window = new TreeMap<>();
            this.timers = new TreeMap<>();
            this.lastSeqNumber = (int) Math.ceil((double) file.length() / (double) blocksize);
            this.latestSeqN = 0;
            this.windowBase = 0;
            this.state = State.BEGINNING;


        } catch(IOException e) {
            throw new Error("File not found");
        }
        return 1;
    }

    @Override
    public void sendNextPacket(int now) {
        if(latestSeqN < lastSeqNumber + 1 && window.size() > windowsize) {
            FT21_DataPacket packet = readData(latestSeqN);
            // add rest
        }
    }

    @Override
    protected void on_receive_ack(int now, int src, FT21_AckPacket ack) {
        switch(state) { // add nack case? -> end timeout of the seqN refered (on_timeout)
            case UPLOADING:  // just don't know how to identify nacks
                if (window.containsKey(ack.cSeqN)) {
                    window.replace(ack.cSeqN, true);
                    timers.remove(ack.cSeqN);
                    while (window.size() > 0 && window.get(window.firstKey())) {
                        window.remove(window.firstKey());
                    }
                } else
                    // ignore and loop again
                break;
            case FINISHING:
                if(ack.cSeqN == window.lastKey())
                    state = State.FINISHED;
                break;
            case FINISHED:
                if(ack.cSeqN == lastSeqNumber + 1) {
                    super.log(now, "All Done. Transfer complete...");
                    super.printReport(now);
                }
                break;

            default:
        }


    }

    public void on_timeout(int now, int seqN) {
        if(window.containsKey(seqN)) {
            super.sendPacket(now, RECEIVER, readData(seqN));
            timers.put(seqN, setTimer(now));
        }
    }

    private int setTimer(int now) { // the first to time is always lowest?
        self.set_timeout(DEFAULT_TIMEOUT);
        return now;
    }

    // couldn't get from super??
    private FT21_DataPacket readData(int seqNumber) {
        try {
            rFile.seek((long) blocksize * (seqNumber - 1));

            byte[] data = new byte[blocksize];
            int size = rFile.read(data);

            return new FT21_DataPacket(seqNumber, data, size);
        } catch (IOException e) {
            throw new Error();
        }
    }

}

