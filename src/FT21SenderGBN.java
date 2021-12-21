import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import cnss.simulator.Node;
import ft21.*;

public class FT21SenderGBN extends FT21AbstractSenderApplication {

    enum State {
        BEGINNING, UPLOADING, FINISHING, FINISHED
    };

    private static final int DEFAULT_TIMEOUT = 1000;
    private final int RECEIVER = 1;
    private final int N = 3;

    private Queue<FT21Packet> window;

    private File file;
    private String filename;
    private RandomAccessFile rFile;
    private int blocksize, windowsize;
    private int lastRewind;
    private int nextSeqN, maxSeqN;
    private int lastACK, last;


    private State state;

    public FT21SenderGBN() {
        super(true, "FT21SenderGBN");
    }

    @Override
    public int initialise(int now, int node_id, Node nodeObj, String[] args) {
        try {
            super.initialise(now, node_id, nodeObj, args);

            this.file = new File(args[0]);
            this.rFile = new RandomAccessFile(file, "r");
            this.filename = file.getName();
            this.blocksize = Integer.parseInt(args[1]);
            this.windowsize = Integer.parseInt(args[2]);
            this.nextSeqN = 0;
            this.maxSeqN = (int) file.length()/blocksize;
            this.lastACK = 0;
            this.state = State.BEGINNING;
            this.window = new LinkedBlockingQueue<>(N);

        } catch(IOException e) {
            throw new Error("File not found");
        }
        sendNextPacket(now);
        return 1;
    }


    public void on_clock_tick(int now) {
        if (state == State.UPLOADING && nextSeqN < maxSeqN)
            sendNextPacket(now);
    }

    public void sendNextPacket(int now) {

        switch (state) {
            case BEGINNING:
                super.sendPacket(now, RECEIVER, new FT21_UploadPacket(file.getName()));
                break;
            case UPLOADING:
                
                super.sendPacket(now, RECEIVER, readData(nextSeqN));
                break;
            case FINISHING:
                super.sendPacket(now, RECEIVER, new FT21_FinPacket(maxSeqN));
                break;
            case FINISHED:
        }

        last = now;
    }

    public FT21_DataPacket readData(int seqN) {
        return null;
    }
}