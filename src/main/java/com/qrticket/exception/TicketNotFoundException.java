package com.qrticket.exception;

import org.springframework.http.HttpStatus;

/**
 * 票券不存在異常
 *
 * @author QR Ticket System Team
 */
public class TicketNotFoundException extends TicketException {

    public TicketNotFoundException(String ticketCode) {
        super("TICKET_NOT_FOUND",
                String.format("票券不存在：%s", ticketCode),
                HttpStatus.NOT_FOUND);
    }
}
