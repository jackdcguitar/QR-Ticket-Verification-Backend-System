package com.qrticket.exception;

import org.springframework.http.HttpStatus;

/**
 * 票券已使用異常
 *
 * @author QR Ticket System Team
 */
public class TicketAlreadyUsedException extends TicketException {

    public TicketAlreadyUsedException(String ticketCode) {
        super("TICKET_ALREADY_USED",
                String.format("票券已使用：%s", ticketCode),
                HttpStatus.CONFLICT);
    }
}
