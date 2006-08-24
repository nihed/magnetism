package com.dumbhippo.aim;

public enum TocError {
	ERROR_NOT_SEEN_BEFORE(-1, "AIM library code doesn't know about this one %s"),
	SIGNON_ERROR(-2, "Signon error: %s"),
	// * General Errors *
	UNAVAILABLE(901, "%s not currently available"),
	WARNING_UNAVAILABLE(902, "Warning of %s not currently available"),
	MESSAGE_DROPPED(903, "A message has been dropped, you are exceeding the server speed limit"),
	// * Admin Errors  *
	INVALID_INPUT(911, "Error validating input"),
	INVALID_ACCOUNT(912, "Invalid account"),
	ERROR_PROCESSING_REQUEST(913, "Error encountered while processing request"),
	SERVICE_UNAVAILABLE(914, "Service unavailable"),
	// * Chat Errors  *
	CHAT_UNAVAILABLE(950, "Chat in %s is unavailable"),
	// * IM & Info Errors *
	SENDING_TOO_FAST(960, "You are sending message too fast to %s"),
	MISSED_TOO_BIG_IM(961, "You missed an im from %s because it was too big"),
	MISSED_TOO_FAST_IM(962, "You missed an im from %s because it was sent too fast"),
	BOT_CANNOT_INITIATE(964, "This screen name is registered as an open bot and cannot start an IM conversation"),
	// * Dir Errors *
	DIR_FAILURE(970, "Directory failure"),
	DIR_TOO_MANY_MATCHES(971, "Too many matches in directory"),
	DIR_NEED_MORE_QUALIFIERS(972, "Need more qualifiers"),
	DIR_UNAVAILABLE(973, "Dir service temporarily unavailable"),
	DIR_RESTRICTED(974, "Email lookup restricted"),
	DIR_KEYWORD_IGNORED(975, "Keyword Ignored"),
	DIR_NO_KEYWORDS(976, "No Keywords"),
	DIR_LANGUAGE_UNSUPPORTED(977, "Language not supported"),
	DIR_COUNTRY_UNSUPPORTED(978, "Country not supported"),
	DIR_UNKNOWN_FAILURE(979, "Failure unknown %s"),
	// * Auth errors *
	INCORRECT_CREDENTIALS(980, "Incorrect nickname or password"),
	AUTH_UNAVAILABLE(981, "The service is temporarily unavailable"),
	TOO_HATED(982, "Your warning level is currently too high to sign on"),
	TOO_MANY_CONNECTIONS(983, "You have been connecting and "
		   + "disconnecting too frequently.  Wait 10 minutes and try again. "
		   + "If you continue to try, you will need to wait even longer."),
	AUTH_UNKNOWN(989, "An unknown signon error has occurred %s");

	private int code;
	private String format;
	
	private TocError(int code, String format) {
		this.code = code;
		this.format = format;
	}
	
	public String format(String args) {
		
		// if there's no %s then "args" is ignored
		
		if (this.code < 0)
			return String.format("unknown code: " + format, args);
		else
			return String.format(code + ": " + format, args);
	}
	
	public static TocError parse(String codeString) {
		int code;
		try {
			code = Integer.parseInt(codeString);
		} catch (NumberFormatException e) {
			code = -500;
		}
		if (code >= 0) {
			for (TocError te : values()) {
				if (te.code == code)
					return te;
			}
		} else if (codeString.equals("Signon err")) {
			return SIGNON_ERROR;
		}
		
		return ERROR_NOT_SEEN_BEFORE;
	}
}
