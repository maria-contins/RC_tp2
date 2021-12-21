import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Queue;

import cnss.simulator.Node;
import ft21.*;

public class FT21SenderGBN extends FT21AbstractSenderApplication {

    enum State {
        BEGINNING, UPLOADING, FINISHING, FINISHED
    };

    private static final int DEFAULT_TIMEOUT = 1000;
    private final int RECEIVER = 1;

    private Queue<Tuple> sentPackages;

    private File file;
    private RandomAccessFile rFile;
    private int blocksize, windowsize;

    private int seqNumber;
    private int lastSeqNumber;

    private State state;

    record Tuple(int seqN, int now) {
    };

    public FT21SenderGBN() {
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

            this.sentPackages = new LinkedList<Tuple>();

            lastSeqNumber = (int) Math.ceil((double) file.length() / (double) blocksize);

            state = State.BEGINNING;
            seqNumber = 0;

        } catch(IOException e) {
            throw new Error("File not found");
        }

        return 1;
    }

//=============================================================================
// Event listeners

    @Override
    public void on_clock_tick(int now) {
        boolean canSend = sentPackages.size() <= windowsize
                || (now - sentPackages.peek().now) > DEFAULT_TIMEOUT;

        if(state != State.FINISHED && canSend)
            sendNextPacket(now);
    }

    //TODO
    @Override
    public void on_timeout(int now) {
        super.on_timeout(now);

        goBackN(sentPackages.poll().seqN);
    }

    @Override
    protected void on_receive_ack(int now, int src, FT21_AckPacket ack) {
        switch(state) {
            case UPLOADING:
                Tuple tuple = sentPackages.remove();

                if(tuple.seqN > ack.cSeqN) {
                    goBackN(ack.cSeqN);
                } else {
                    int oldestPacketSent = tuple.now;

                    self.set_timeout((oldestPacketSent - now) + 1000);
                }
                break;

            case FINISHING:
                super.log(now, "All Done. Transfer complete...");
                super.printReport(now);
                state = State.FINISHED;
                break;

            default:
        }
    }

    private void goBackN(int newSeqNumber) {
       //seqNumber = sentPackages.remove().seqN;
        seqNumber = newSeqNumber;

        if(seqNumber == 0) state = State.BEGINNING;
        sentPackages.clear();
    }

    //=============================================================================

    /**
     *
     * @param now
     */
    public void sendNextPacket(int now) {
       switch(state) {
           case BEGINNING:
               sentPackages.add(new Tuple(seqNumber, now));

               super.sendPacket(now, RECEIVER, new FT21_UploadPacket(file.getName()));
               self.set_timeout(DEFAULT_TIMEOUT);
               state = State.UPLOADING;
               break;

           case UPLOADING:
               seqNumber++;
               sentPackages.add(new Tuple(seqNumber, now));

               super.sendPacket(now, RECEIVER, readData(seqNumber));

               if (seqNumber > lastSeqNumber)
                   state = State.FINISHING;
               break;

           case FINISHING:
               sentPackages.add(new Tuple(seqNumber, now));

               super.sendPacket(now, RECEIVER, new FT21_FinPacket(seqNumber));
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
            rFile.seek(blocksize * seqNumber);

            byte[] data = new byte[blocksize];
            int size = rFile.read(data);

            return new FT21_DataPacket(seqNumber, data, size);
        } catch (IOException e) {
            throw new Error();
        }
    }
}