package com.greenlearner.ticketbookingservice.timeout;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockApp;
import com.greenlearner.ticketbookingservice.dto.CardDetails;
import com.greenlearner.ticketbookingservice.dto.TicketBookingPaymentRequest;
import com.greenlearner.ticketbookingservice.dto.TicketBookingResponse;
import com.greenlearner.ticketbookingservice.gateway.PaymentProcessorGateway;
import com.greenlearner.ticketbookingservice.service.TicketBookingService;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.greenlearner.ticketbookingservice.dto.TicketBookingResponse.BookingResponseStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Bishesh
 */
public class TicketBookingServiceRequestStubbingTest {

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

        stubFor(post ("/payments").withRequestBody (equalToJson ("{\n" +
                                                                         "\"cardNumber\" : \"1111-1111-1111-1111\",\n" +
                                                                         "\"cardExpiryDate\" : \"2022-06-07\",\n" +
                                                                         "\"amount\" : 100.00\n" +
                                                                         "} "))
                        .willReturn (okJson ("{\n" +
                                                     "\"paymentId\" : \"3333\",\n" +
                                                     "\"paymentResponseStatus\" : \"SUCCESS\"\n" +
                                                     "}").withFixedDelay (1200)));
        TicketBookingPaymentRequest ticketBookingPaymentRequest =
                new TicketBookingPaymentRequest ("1111", 100D,
                                                 new CardDetails ("1111-1111-1111-1111", LocalDate.now()));
        //TicketBookingResponse response = tbs.payForBooking (ticketBookingPaymentRequest);

        assertThrows(ResourceAccessException.class,() -> tbs.payForBooking (ticketBookingPaymentRequest));

        verify (postRequestedFor (urlPathEqualTo ("/payments"))
                        .withRequestBody (equalToJson ("{\n" +
                                                               "\"cardNumber\" : \"1111-1111-1111-1111\",\n" +
                                                               "\"cardExpiryDate\" : \"2022-06-07\",\n" +
                                                               "\"amount\" : 100.00\n" +
                                                               "} ")));
    }

    @Test
    public void testCase2 () {

        stubFor(post ("/payments").withRequestBody (equalToJson ("{\n" +
                                                                         "\"cardNumber\" : \"1111-1111-1111-1111\",\n" +
                                                                         "\"cardExpiryDate\" : \"2022-06-07\",\n" +
                                                                         "\"amount\" : 100.00\n" +
                                                                         "} "))
                        .willReturn (okJson ("{\n" +
                                                     "\"paymentId\" : \"3333\",\n" +
                                                     "\"paymentResponseStatus\" : \"SUCCESS\"\n" +
                                                     "}").withLogNormalRandomDelay (1000000,0.4)));
        TicketBookingPaymentRequest ticketBookingPaymentRequest =
                new TicketBookingPaymentRequest ("1111", 100D,
                                                 new CardDetails ("1111-1111-1111-1111", LocalDate.now()));
        //TicketBookingResponse response = tbs.payForBooking (ticketBookingPaymentRequest);

        assertThrows(ResourceAccessException.class,() -> tbs.payForBooking (ticketBookingPaymentRequest));

        verify (postRequestedFor (urlPathEqualTo ("/payments"))
                        .withRequestBody (equalToJson ("{\n" +
                                                               "\"cardNumber\" : \"1111-1111-1111-1111\",\n" +
                                                               "\"cardExpiryDate\" : \"2022-06-07\",\n" +
                                                               "\"amount\" : 100.00\n" +
                                                               "} ")));
    }

    @AfterEach
    public void teardown () {
        wireMockServer.stop ();
    }

}
