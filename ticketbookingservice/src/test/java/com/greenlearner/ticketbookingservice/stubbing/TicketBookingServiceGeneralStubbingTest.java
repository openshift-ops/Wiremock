package com.greenlearner.ticketbookingservice.stubbing;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.greenlearner.ticketbookingservice.dto.CardDetails;
import com.greenlearner.ticketbookingservice.dto.PaymentUpdateResponse;
import com.greenlearner.ticketbookingservice.dto.TicketBookingPaymentRequest;
import com.greenlearner.ticketbookingservice.gateway.PaymentProcessorGateway;
import com.greenlearner.ticketbookingservice.service.TicketBookingService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Bishesh
 */
public class TicketBookingServiceGeneralStubbingTest {

    private TicketBookingService tbs;

    private WireMockServer wireMockServer; //use WiremockServer for JUNIT 5 and WireMockRule for JUNIT 4

    @BeforeEach
    public void setup(){
        wireMockServer = new WireMockServer ();
        configureFor ("localhost",8080);
        wireMockServer.start ();

        PaymentProcessorGateway paymentProcessorGateway = new PaymentProcessorGateway("localhost",wireMockServer.port ());
        tbs = new TicketBookingService(paymentProcessorGateway);

    }


    @Test
    public void testCase1 () {

        stubFor(any (anyUrl ()).willReturn (ok ()));
        TicketBookingPaymentRequest ticketBookingPaymentRequest =
                new TicketBookingPaymentRequest ("1111", 100D,
                                                 new CardDetails ("1234567890", LocalDate.of (2023, 4, 4)));
        PaymentUpdateResponse response = tbs.updatePaymentDetails (ticketBookingPaymentRequest);

        assertThat (response.getStatus ()).isEqualTo ("SUCCESS");

        //verify
        verify (postRequestedFor (urlEqualTo ("/update")));
    }

    @AfterEach
    public void teardown () {
        wireMockServer.stop ();
    }

}
