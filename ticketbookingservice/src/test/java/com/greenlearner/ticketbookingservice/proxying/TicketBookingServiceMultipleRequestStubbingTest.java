package com.greenlearner.ticketbookingservice.proxying;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.greenlearner.ticketbookingservice.dto.CardDetails;
import com.greenlearner.ticketbookingservice.dto.TicketBookingPaymentRequest;
import com.greenlearner.ticketbookingservice.dto.TicketBookingResponse;
import com.greenlearner.ticketbookingservice.gateway.PaymentProcessorGateway;
import com.greenlearner.ticketbookingservice.service.TicketBookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.greenlearner.ticketbookingservice.dto.TicketBookingResponse.BookingResponseStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Bishesh
 */
public class TicketBookingServiceMultipleRequestStubbingTest {

    private TicketBookingService tbs;

    private WireMockServer wireMockServer; //use WiremockServer for JUNIT 5 and WireMockRule for JUNIT 4

    @BeforeEach
    public void setup(){
        wireMockServer = new WireMockServer ();
        configureFor ("localhost",8080);
        wireMockServer.start ();

        PaymentProcessorGateway paymentProcessorGateway = new PaymentProcessorGateway("localhost",wireMockServer.port ());
        tbs = new TicketBookingService(paymentProcessorGateway);
        //apply the proxy here
        //basically proxy helps in the scenarios where some apis are developed and other have not we can mix the stubiing and the actual calls together

        stubFor (any (anyUrl ()).willReturn (aResponse ().proxiedFrom ("http://localhost:8082")));
    }


    @Test
    public void testCase1 () {

        stubFor (get (urlPathEqualTo ("/fraudCheck/1111-1111-1111-1111" )).willReturn (okJson ("{\n" +
                                                                                   "\"blacklisted\" : false\n" +
                                                                                   "}")));
        TicketBookingPaymentRequest ticketBookingPaymentRequest =
                new TicketBookingPaymentRequest ("1111", 100D,
                                                 new CardDetails ("1111-1111-1111-1111", LocalDate.now()));
        ticketBookingPaymentRequest.setFraudAlert (true);
        TicketBookingResponse response = tbs.payForBooking (ticketBookingPaymentRequest);

        assertThat (response.getBookingResponseStatus ()).isEqualTo (SUCCESS);

        verify (postRequestedFor (urlPathEqualTo ("/payments"))
                        .withRequestBody (equalToJson ("{\n" +
                                                               "\"cardNumber\" : \"1111-1111-1111-1111\",\n" +
                                                               "\"cardExpiryDate\" : \"2022-06-07\",\n" +
                                                               "\"amount\" : 100.00\n" +
                                                               "} ")));


        verify (getRequestedFor (urlPathEqualTo ("/fraudCheck/1111-1111-1111-1111")));

    }

    @AfterEach
    public void teardown () {
        wireMockServer.stop ();
    }

}
