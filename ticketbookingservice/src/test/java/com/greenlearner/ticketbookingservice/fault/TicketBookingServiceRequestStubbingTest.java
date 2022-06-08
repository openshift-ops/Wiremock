package com.greenlearner.ticketbookingservice.fault;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.greenlearner.ticketbookingservice.dto.CardDetails;
import com.greenlearner.ticketbookingservice.dto.TicketBookingPaymentRequest;
import com.greenlearner.ticketbookingservice.dto.TicketBookingResponse;
import com.greenlearner.ticketbookingservice.gateway.PaymentProcessorGateway;
import com.greenlearner.ticketbookingservice.service.TicketBookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.greenlearner.ticketbookingservice.dto.TicketBookingResponse.BookingResponseStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                        .willReturn (serverError ()));
        TicketBookingPaymentRequest ticketBookingPaymentRequest =
                new TicketBookingPaymentRequest ("1111", 100D,
                                                 new CardDetails ("1111-1111-1111-1111", LocalDate.now()));
        //TicketBookingResponse response = tbs.payForBooking (ticketBookingPaymentRequest);

      assertThrows(HttpServerErrorException.InternalServerError.class,() -> tbs.payForBooking (ticketBookingPaymentRequest));
    }

    @Test
    public void testCase2 () {

        stubFor(post ("/payments").withRequestBody (equalToJson ("{\n" +
                                                                         "\"cardNumber\" : \"1111-1111-1111-1111\",\n" +
                                                                         "\"cardExpiryDate\" : \"2022-06-07\",\n" +
                                                                         "\"amount\" : 100.00\n" +
                                                                         "} "))
                        .willReturn (aResponse ().withFault (Fault.MALFORMED_RESPONSE_CHUNK)));
        TicketBookingPaymentRequest ticketBookingPaymentRequest =
                new TicketBookingPaymentRequest ("1111", 100D,
                                                 new CardDetails ("1111-1111-1111-1111", LocalDate.now()));
        //TicketBookingResponse response = tbs.payForBooking (ticketBookingPaymentRequest);

        assertThrows(ResourceAccessException.class, () -> tbs.payForBooking (ticketBookingPaymentRequest));
    }

    @Test
    public void testCase3 () {

        stubFor(post ("/payments").withRequestBody (equalToJson ("{\n" +
                                                                         "\"cardNumber\" : \"1111-1111-1111-1111\",\n" +
                                                                         "\"cardExpiryDate\" : \"2022-06-07\",\n" +
                                                                         "\"amount\" : 100.00\n" +
                                                                         "} "))
                        .willReturn (serverError ().withStatusMessage ("This is an exception")));
        TicketBookingPaymentRequest ticketBookingPaymentRequest =
                new TicketBookingPaymentRequest ("1111", 100D,
                                                 new CardDetails ("1111-1111-1111-1111", LocalDate.now()));
        //TicketBookingResponse response = tbs.payForBooking (ticketBookingPaymentRequest);

        HttpServerErrorException.InternalServerError result = assertThrows (HttpServerErrorException.InternalServerError.class, ( ) -> tbs.payForBooking (ticketBookingPaymentRequest));
        assertThat (result.getStatusText ()).isEqualTo ("This is an exception");
    }

    @AfterEach
    public void teardown () {
        wireMockServer.stop ();
    }

}
