package ft21;

public class FT21_Packet extends FT21Packet {
	public final String filename;
	
	public FT21_Packet(String filename) {
		super(PacketType.UPLOAD);
		super.putByte(NO_OPTIONAL_DATA_LEN);
		this.filename = filename;
		super.putString(filename);
	}
	
	public String toString() {
		return String.format("UPLOAD<%s>", filename);
	}

}
