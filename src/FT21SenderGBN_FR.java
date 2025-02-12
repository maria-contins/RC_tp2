import cnss.simulator.Node;
import ft21.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Queue;

/**
 * As by the book provided for this course, "Fundamentos de Redes de Computadores, ilustrados com base na internet e nos protocolos TCP/IP" by Professor José Legatheaux Martins
 *
 * " O protocolo GBN (Go-Back-N) é i, protocolo de janela deslizante em que a janela do emissor tem dimensão N e a janela do receptor tem (pelo menos) dimensão 1.
 * O emissor desde que tenha pacotes para enviar, e enquanto a condição sobre a dimensão da janela de emissão ser <= N for respeitada, emite pacotes uns atrás dos outros."
 * [...]
 * " Quando dispara um alarme no emissor (timeout) este volta a reemitir os pacotes que estão na janela, começando pelo que tem o número de sequência mais baixo, pois
 * este é o pacote em trânsito mais antigo, e para poder voltar a colocar os pacotes pela ordem é necessário recomeçar por este e reemitir a janela"
 *
 * It provides a good overview of the inner workings of the sender side of a GBN algorithm. As no implementation constraints indicated otherwise, and as
 * the book also refers "é possível introduzir uma optimização aquando da receção de um ACK com número de sequência repetido."
 *
 * We decided on this optimization as to implement the best version of the algorithm.
 */

public class FT21SenderGBN_FR extends FT21AbstractSenderApplication {

    enum State {
        BEGINNING, UPLOADING, FINISHING, FINISHED
    };

    private static final int DEFAULT_TIMEOUT = 1000;
    private final int RECEIVER = 1;

    record Tuple(int seqN, int now){};

    private Queue<Tuple> window;

    private File file;
    private RandomAccessFile rFile;
    private int blocksize;
    private int windowsize;

    private int lastGoBack;

    private int sequenceNumber;
    private int lastSeqNumber;

    private State state;

    public FT21SenderGBN_FR(boolean logOn, String name) {super(logOn, name); }

    public FT21SenderGBN_FR() {
        super(true, "FT21SenderGBN_FR");
    }

    @Override
    public int initialise(int now, int node_id, Node nodeObj, String[] args) {
        try {
            super.initialise(now, node_id, nodeObj, args);

            this.file = new File(args[0]);
            this.rFile = new RandomAccessFile(file, "r");
            this.blocksize = Integer.parseInt(args[1]);
            this.windowsize = Integer.parseInt(args[2]);

            this.window = new LinkedList<>();

            lastSeqNumber = (int) Math.ceil((double) file.length() / (double) blocksize);
            state = State.BEGINNING;
            sequenceNumber = 0;
            lastGoBack = -1;

        } catch(IOException e) {
            throw new Error("File not found");
        }

        return 1;
    }

//=============================================================================
// Event listeners

    @Override
    public void on_clock_tick(int now) {
        if(state != State.FINISHED && window.size() <= windowsize) {
            sendNextPacket(now);
        }
    }

    @Override
    public void on_timeout(int now) {
        //super.on_timeout(now);
        assert window.peek() != null;
        goBackN(window.peek().seqN);

        if(state != State.FINISHED && window.size() <= windowsize)
            sendNextPacket(now);
    }

    @Override
    protected void on_receive_ack(int now, int src, FT21_AckPacket ack) {
        switch(state) {
            case UPLOADING:
                assert window.peek() != null;
                if(window.peek().seqN <= ack.cSeqN) {
                    for(int i = 0; i < (ack.cSeqN - window.peek().seqN + 1); i++) {
                        window.remove();
                    }
                    self.set_timeout(DEFAULT_TIMEOUT);
                } else if (lastGoBack != ack.cSeqN) {
                    goBackN(ack.cSeqN);
                }
                break;

            case FINISHING:
                assert window.peek() != null;
                if(window.peek().seqN <= ack.cSeqN) {
                    state = State.FINISHED;
                } else if (lastGoBack != ack.cSeqN){
                    state = State.UPLOADING;
                    goBackN(ack.cSeqN);
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

    private void goBackN(int newSeqN) {
        System.out.println("==========\nSTARTED AT: " + sequenceNumber);

        sequenceNumber = newSeqN;
        lastGoBack = sequenceNumber;

        System.out.println("JUMPED TO: " + sequenceNumber + "\n==========");

        if(sequenceNumber == 0) state = State.BEGINNING;
        window.clear();
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
               packet = new FT21_UploadPacket(file.getName());

               super.sendPacket(now, RECEIVER, packet);
               self.set_timeout(DEFAULT_TIMEOUT);
               state = State.UPLOADING;
               break;

           case UPLOADING:
               sequenceNumber++;
               packet = readData(sequenceNumber);

               super.sendPacket(now, RECEIVER, packet);

               if (sequenceNumber == lastSeqNumber)
                   state = State.FINISHING;
               break;

           case FINISHING:
               sequenceNumber++;
               packet = new FT21_FinPacket(sequenceNumber);

               super.sendPacket(now, RECEIVER, packet);
               break;

           default:
       }

        window.add(new Tuple(sequenceNumber, now));
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