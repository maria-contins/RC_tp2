import cnss.simulator.Node;
import ft21.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;


public class FT21SenderSR extends FT21AbstractSenderApplication {

    private final int RECEIVER = 1;
    private static final int DEFAULT_TIMEOUT = 1000;

    record Tuple(int seqN, int now){}

    private SortedMap<Integer,FT21_DataPacket> window;
    private LinkedList<Tuple> timerSequence;
    private LinkedList<Integer> acked;

    private State state;
    private int blocksize, windowsize, lastSeqNumber, windowBase, latestSeqN;
    private File file;
    private RandomAccessFile rFile;

    enum State {
        BEGINNING, UPLOADING, FINISHING, FINISHED
    }

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
            this.timerSequence = new LinkedList<>();
            this.acked = new LinkedList<Integer>();

            this.lastSeqNumber = (int) Math.ceil((double) file.length() / (double) blocksize);
            this.latestSeqN = 0;
            this.windowBase = 0;

            state = State.BEGINNING;

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
                timerSequence.addLast(new Tuple(latestSeqN, now));
                break;

            case UPLOADING:
                FT21_DataPacket dPacket = readData(++latestSeqN);
                window.put(latestSeqN, dPacket);

                super.sendPacket(now, RECEIVER, dPacket);
                timerSequence.addLast(new Tuple(latestSeqN, now));

                if (latestSeqN == lastSeqNumber)
                    state = State.FINISHING;
                break;

            case FINISHING:
                FT21_FinPacket fPacket = new FT21_FinPacket(++latestSeqN);

                super.sendPacket(now, RECEIVER, fPacket);
                timerSequence.addLast(new Tuple(latestSeqN, now));
                break;
            default:
        }
    }

    @Override
    protected void on_receive_ack(int now, int src, FT21_AckPacket ack) {
        switch(state) {
            case BEGINNING:
                state = State.UPLOADING;
                break;

            case UPLOADING:
                acked.add(ack.trueSeq);
                timerSequence.removeFirstOccurrence(ack.trueSeq);

                if(acked.size() == windowsize) {
                    System.out.print("\n[");
                    for(int k: acked) {
                        System.out.print(k + ", ");
                    }
                    System.out.print("]\n");
                    for (int i = 0; i < acked.size(); i++) {
                        window.remove(acked.getFirst());
                        acked.removeFirst();
                    }
                }
            break;

            case FINISHING:
                if(ack.trueSeq == window.lastKey())
                    state = State.FINISHED;
                break;

            case FINISHED:
                if(ack.trueSeq == lastSeqNumber + 1) {
                    super.log(now, "All Done. Transfer complete...");
                    super.printReport(now);
                }
                break;

            default:
        }

        removeTimer(ack.cSeqN);
    }

    private void removeTimer(int cSeqN) {
        for(int i = 0; i < timerSequence.size(); i++) {
            if(timerSequence.get(i).seqN == cSeqN) {
                timerSequence.remove(i);
                return;
            }
        }
    }

    @Override
    public void on_clock_tick(int now) {
        if (Math.abs(now - (timerSequence.isEmpty() ? now : timerSequence.peek().now)) <= DEFAULT_TIMEOUT) {
            if (state == State.UPLOADING && window.size() <= windowsize) {
                sendNextPacket(now);
            }
        } else {
            on_timeout(now);
        }
    }

    @Override
    public void on_timeout(int now) {  // in the SW cases it sends packet and resets timer only
        System.out.println("timeout");
        switch(state) {

            case BEGINNING:
            case FINISHING:
                sendNextPacket(now);
                break;

            case UPLOADING:
                assert timerSequence.size() != 0;
                System.out.print(timerSequence.peek().seqN);
                System.out.print("\n[");
                for(int k: acked) {
                    System.out.print(k + ", ");
                }
                if (window.containsKey(timerSequence.peek().seqN)) {
                    sendPacket(now, RECEIVER, readData(timerSequence.remove().seqN));
                }
                break;

            default:
        }
    }

    private FT21_DataPacket readData(int seqNumber) {
        try {
            rFile.seek((long) blocksize * (seqNumber - 1));

            byte[] data = new byte[blocksize];
            int size = rFile.read(data);

            return new FT21_DataPacket(seqNumber, data, size, seqNumber);
        } catch (IOException e) {
            throw new Error();
        }
    }

}

