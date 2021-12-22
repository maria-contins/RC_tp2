package ft21;

public class FT21_UploadPacket extends FT21Packet {
	public final String filename;
	public boolean opDataAcked;
	public int opDataTimeStarted;
	
	public FT21_UploadPacket(String filename) {
		super(PacketType.UPLOAD);
		super.putByte(NO_OPTIONAL_DATA_LEN);
		this.filename = filename;
		super.putString(filename);
		this.opDataAcked = false;
		this.opDataTimeStarted = -1;
	}
	
	public String toString() {
		return String.format("UPLOAD<%s>", filename);
	}

	public void setAcked() {
		opDataAcked = !opDataAcked;
	}

	public boolean getAckedVal() {
		return opDataAcked;
	}

	// is -1 if off
	public void setTimeStarted(int time) {
		opDataTimeStarted = time;
	}

	public int getTimeStarted() {
		return opDataTimeStarted;
	}

}
