import cnss.simulator.DataPacket;
import cnss.simulator.Node;
import ft21.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;


public class FT21SenderSR extends FT21SenderGBN_FR {

    private final int RECEIVER = 1;
    private static final int DEFAULT_TIMEOUT = 1000;

    private SortedMap<Integer,Boolean> window2;
    private SortedMap<Integer,Integer> timers; // might not need "now" value in there
    private SortedMap<Integer, FT21_DataPacket> window;
    private State state;
    private int blocksize, windowsize, lastSeqNumber, windowBase, latestSeqN;
    private File file;
    private RandomAccessFile rFile;

    private boolean blocked;

    public FT21SenderSR() {
        super(true, "FT21SenderSR");
    }

    @Override
    public int initialise(int now, int node_id, Node nodeObj, String[] args) {
        try {
            super.initialise(now, node_id, nodeObj, args);

            this.file = new File(args[0]);
            this.rFile = new RandomAccessFile(file, "r");
            this.blocksize = Integer.parseInt(args[1]);
            this.windowsize = Integer.parseInt(args[2]);

            this.window = new TreeMap<>();
            this.timers = new TreeMap<>();

            this.lastSeqNumber = (int) Math.ceil((double) file.length() / (double) blocksize);
            this.latestSeqN = 0;
            this.windowBase = 0;

            this.blocked = false;

            this.state = State.BEGINNING;

        } catch(IOException e) {
            throw new Error("File not found");
        }
        return 1;
    }

    @Override
    public void on_clock_tick(int now) {
        if(!blocked && (state != State.FINISHED && window.size() <= windowsize)) {
            sendNextPacket(now);
        }
    }

    @Override
    public void sendNextPacket(int now) {
        switch(state) {
            case BEGINNING:
                super.sendPacket(now, RECEIVER, new FT21_UploadPacket(file.getName()));
                self.set_timeout(DEFAULT_TIMEOUT);
                break;

            case UPLOADING:
                FT21_DataPacket dPacket = readData(++latestSeqN);
                super.sendPacket(now, RECEIVER, dPacket);
                window.put(latestSeqN,dPacket);
                if (latestSeqN == lastSeqNumber)
                    state = State.FINISHING;
                break;

            case FINISHING:
                FT21_FinPacket fPacket = new FT21_FinPacket(++latestSeqN);
                super.sendPacket(now, RECEIVER, fPacket);
                break;
            default:
        }
    }

    @Override
    protected void on_receive_ack(int now, int src, FT21_AckPacket ack) {
        switch(state) { // add nack case? -> end timeout of the seqN refered (on_timeout)
            case UPLOADING:
                state = State.UPLOADING;
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

    @Override
    public void on_timeout(int now) {
      //  if(window.containsKey(seqN)) {
       //     super.sendPacket(now, RECEIVER, readData(seqN));
       //     setTimer(now,seqN);
      // }
    }

    private void setTimer(int now, int seqN) {   //TODO (???)
        self.set_timeout(DEFAULT_TIMEOUT);
        window.get(seqN); // cast????
    }

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

