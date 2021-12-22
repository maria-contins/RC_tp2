import cnss.simulator.Node;
import ft21.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;


public class temp extends FT21AbstractSenderApplication {

    enum State {
        BEGINNING, BLOCKED, UPLOADING, FINISHING, FINISHED
    };

    private static final int DEFAULT_TIMEOUT = 1000;
    private final int RECEIVER = 1;

    record WindowElement(int seqN, int now, FT21_Packet packet) implements Comparable<WindowElement> {
        @Override
        public int compareTo(WindowElement o) {
            return now - o.now;
        }
    };

    private SortedMap<Integer, FT21_DataPacket> window;
    private SortedMap<Integer, Integer> timers; // K -> timer, V -> cSeq
    private LinkedList<Integer> acked;

    private File file;
    private RandomAccessFile rFile;
    private int blocksize;
    private int windowsize;


    private int sequenceNumber;
    private int lastSeqNumber;

    private State state;

    public temp() {
        super(true, "FT21SenderGBN");
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
            this.acked = new LinkedList<>();

            lastSeqNumber = (int) Math.ceil((double) file.length() / (double) blocksize);
            state = State.BEGINNING;
            sequenceNumber = 0;

        } catch(IOException e) {
            throw new Error("File not found");
        }

        return 1;
    }

//=============================================================================
// Event listeners

    @Override
    public void on_clock_tick(int now) {
        if (Math.abs(now - (timers.isEmpty() ? now : timers.firstKey() )) <= 1000) {
            if (window.size() <= windowsize) {
                sendNextPacket(now);
            }
        } else {
            on_timeout(now);
        }
    }

    public void on_timeout(int now) {
        if(state != State.FINISHED) {
            int auxSeq = timers.firstKey();

            FT21_DataPacket packet = window.get(auxSeq);

            super.sendPacket(now, RECEIVER, packet);
        }
    }

    @Override
    protected void on_receive_ack(int now, int src, FT21_AckPacket ack) {
        switch(state) {
            case BLOCKED:
                window.clear();
                state = State.UPLOADING;
                break;

            case UPLOADING:
                assert !window.isEmpty();

                }
                break;

            case FINISHING:
                assert window.peek() != null;

                if(window.peek().seqN >= ack.cSeqN)
                    state = State.UPLOADING;
                else if (ack.cSeqN == lastSeqNumber) {
                    sendNextPacket(now);
                }
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

    //=============================================================================

    /**
     *
     * @param now
     */
    public void sendNextPacket(int now) {
        FT21Packet packet;

        switch(state) {
           case BEGINNING:
               packet = new FT21_Packet(file.getName());

               super.sendPacket(now, RECEIVER, packet);

               state = State.BLOCKED;
               break;

           case UPLOADING:
               sequenceNumber++;
               packet = readData(sequenceNumber);

               window.put(sequenceNumber, (FT21_DataPacket)  packet);

               super.sendPacket(now, RECEIVER, packet);


               if (sequenceNumber == lastSeqNumber) {
                   state = State.FINISHING;
               }
               break;

           case FINISHING:
               sequenceNumber++;
               packet = new FT21_FinPacket(sequenceNumber);

               super.sendPacket(now, RECEIVER, packet);

               window.clear();
               state = State.FINISHED;
               break;

           default:
       }
    }


    /**
     * Reads a new data packet from <code>file</code>.
     *
     * Has max size equaling <code>blocksize</code>.
     *
     * @param seqNumber - the sequence number of the current file
     * @return a new data packet
     */
    private FT21_DataPacket readData(int seqNumber) {
        try {
            rFile.seek(blocksize * (seqNumber - 1));

            byte[] data = new byte[blocksize];
            int size = rFile.read(data);

            return new FT21_DataPacket(seqNumber, data, size);
        } catch (IOException e) {
            throw new Error();
        }
    }
}