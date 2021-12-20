import java.io.File;
import java.io.RandomAccessFile;

import cnss.simulator.Node;
import ft21.FT21AbstractSenderApplication;
import ft21.FT21_AckPacket;
import ft21.FT21_DataPacket;
import ft21.FT21_FinPacket;
import ft21.FT21_UploadPacket;

public class FT21SenderGBN extends FT21AbstractSenderApplication {

    private static final int DEFAULT_TIMEOUT = 1000;

    private String filename;
    private int blocksize, windowsize;

    public FT21SenderGBN() {
        super(true, "FT21SenderGBN");
    }

    public int initialize(int now, int node_id, Node nodeObj, String[] args) {
        super.initialise(now, node_id, nodeObj, args);

        this.filename = args[0];
        this.blocksize = Integer.parseInt(args[1]);
        this.windowsize = Integer.parseInt(args[2]);

        return 0; //TODO: change
    }

    public void on_clock_tick(int now) {
        //TODO: create
    }

    public void sendNextPacket(int now) {
        //TODO: create
    }


}