package depchain.common.messaging;

public class ErrorMessage extends Message {
	private String errorMessage;

	public ErrorMessage(String errorMessage) {
		super(MessageType.ERROR);
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public String toString() {
		return "ErrorMessage{" +
				"errorMessage='" + errorMessage + '\'' +
				'}';
	}
}
