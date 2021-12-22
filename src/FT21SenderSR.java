import cnss.simulator.Node;
import ft21.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;


public class FT21SenderSR extends FT21AbstractSenderApplication {

    private final int RECEIVER = 1;
    private static final int DEFAULT_TIMEOUT = 1000;

    private SortedMap<Integer,FT21_DataPacket> window;
    private Queue<Integer> timerSequence;
    private State state;
    private int blocksize, windowsize, lastSeqNumber, windowBase, latestSeqN;
    private File file;
    private RandomAccessFile rFile;

    enum State {
        BEGINNING, UPLOADING, FINISHING, FINISHED
    };

    public FT21SenderSR() {
        super(true, "FT21SenderSr");
    }

    @Override
    public int initialise(int now, int node_id, Node nodeObj, String[] args) {
        try {
            super.initialise(now, node_id, nodeObj, args);
            this.file = new File(args[0]);
            this.rFile = new RandomAccessFile(file, "r");
            this.blocksize = Integer.parseInt(args[1]);
            this.windowsize = Integer.parseInt(args[2]);
            this.window= new TreeMap<>();
            this.lastSeqNumber = (int) Math.ceil((double) file.length() / (double) blocksize);
            this.latestSeqN = 0;
            this.windowBase = 0;
            this.state = State.BEGINNING;

            sendNextPacket(now); // bc on_clock_tick does nothing while in beginning state
        } catch(IOException e) {
            throw new Error("File not found");
        }
        return 1;
    }

    private void sendNextPacket(int now) {
        switch(state) {
            case BEGINNING:
                FT21_Packet packet = new FT21_Packet(file.getName());
                super.sendPacket(now, RECEIVER, packet);
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
                self.set_timeout(DEFAULT_TIMEOUT);
                break;
            default:
        }
    }

    @Override
    protected void on_receive_ack(int now, int src, FT21_AckPacket ack) {
        switch(state) {
            case BEGINNING:
                state = State.UPLOADING;
                sendNextPacket(now);
            case UPLOADING:
                if (window.containsKey(ack.cSeqN)) {
                    window.get(ack.cSeqN).setACK();
                    window.get(ack.cSeqN).setTime(-1);
                    while (window.size() > 0 && (window.get(window.firstKey())).getACK()) {
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

    @Override
    public void on_clock_tick(int now) {
        if(state == State.UPLOADING && window.size() <= windowsize) {
            sendNextPacket(now);
        }
    }

    @Override
    public void on_timeout(int now) {  // in the SW cases it sends packet and resets timer only
        if(state == State.BEGINNING) {
            FT21_Packet packet = new FT21_Packet(file.getName());
            super.sendPacket(now, RECEIVER, packet);
            self.set_timeout(DEFAULT_TIMEOUT);
        } else if(state == State.FINISHING) {
            FT21_FinPacket fPacket = new FT21_FinPacket(++latestSeqN);
            super.sendPacket(now, RECEIVER, fPacket);
            self.set_timeout(DEFAULT_TIMEOUT);
        } else { // UPLOADING
            assert timerSequence.size() != 0;
            if (window.containsKey(timerSequence.peek())) {
                super.sendPacket(now, RECEIVER, readData(timerSequence.peek()));
                timerSequence.remove();
                self.set_timeout(DEFAULT_TIMEOUT - now + window.get(timerSequence.peek()).getTime());
            }
       }
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

