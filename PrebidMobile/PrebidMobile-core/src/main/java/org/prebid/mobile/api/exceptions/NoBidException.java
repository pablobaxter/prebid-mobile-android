package org.prebid.mobile.api.exceptions;

public class NoBidException extends AdException {
    public NoBidException() {
        super("No bids", "Response code 204.");
    }
}
