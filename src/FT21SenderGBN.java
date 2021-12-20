import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Queue;

import cnss.simulator.Node;
import ft21.FT21AbstractSenderApplication;
import ft21.FT21_DataPacket;
import ft21.FT21_UploadPacket;

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

    private State state;

    record Tuple(int seqN, int now) {
    };

    public FT21SenderGBN() {
        super(true, "FT21SenderGBN");
    }

    //TODO: Still unfinished
    @Override
    public int initialise(int now, int node_id, Node nodeObj, String[] args) {
        try {
            super.initialise(now, node_id, nodeObj, args);

            this.file = new File(args[0]);
            this.rFile = new RandomAccessFile(file, "r");
            this.blocksize = Integer.parseInt(args[1]);
            this.windowsize = Integer.parseInt(args[2]);

            this.sentPackages = new LinkedList<Tuple>();

            state = State.BEGINNING;
        } catch(IOException e) {
            throw new Error("File not found");
        }

        return 1;
    }

    public void on_clock_tick(int now) {
        boolean canSend = sentPackages.size() == windowsize
                || (now - sentPackages.peek().now) > DEFAULT_TIMEOUT;

        if(state != State.FINISHED && canSend)
            sendNextPacket(now);
    }

    public void sendNextPacket(int now) {
       switch(state) {
           case BEGINNING:
               super.sendPacket(now, RECEIVER, new FT21_UploadPacket(file));
               break;

           case UPLOADING:
               super.sendPacker(now, RECEIVER, new FT21_DataPacket())
               break;

           case FINISHING:
               break;

           default:
               break;
       }
    }

    public FT21_DataPacket readData(int seqN) {
        try {

        } catch (IOException e) {
            throw new IOException("Failed to read file");
        }
    }
}