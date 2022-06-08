package com.greenlearner.ticketbookingservice.recording;

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
public class TicketBookingServiceRequestStubbingTest {

    private TicketBookingService tbs;

    private WireMockServer wireMockServer; //use WiremockServer for JUNIT 5 and WireMockRule for JUNIT 4

    @BeforeEach
    public void setup(){
        wireMockServer = new WireMockServer ();
        configureFor ("localhost",8080);
        wireMockServer.start ();

        //wireMockServer.startRecording ("http://localhost:8082"); //here we have to give the base url and hit it only once to capture ther equest and response stubbings

        PaymentProcessorGateway paymentProcessorGateway = new PaymentProcessorGateway("localhost",wireMockServer.port ());
        tbs = new TicketBookingService(paymentProcessorGateway);

    }


    @Test
    public void testCase1 () {

        TicketBookingPaymentRequest ticketBookingPaymentRequest =
                new TicketBookingPaymentRequest ("1111", 100D,
                                                 new CardDetails ("1111-1111-1111-1111", LocalDate.now()));
        TicketBookingResponse response = tbs.payForBooking (ticketBookingPaymentRequest);

        assertThat (response).isEqualToComparingFieldByField (new TicketBookingResponse ("1111", "41ec6a8f-949a-4965-b96e-7f9d42267459", SUCCESS));

        verify (postRequestedFor (urlPathEqualTo ("/payments"))
                        .withRequestBody (equalToJson ("{\n" +
                                                               "\"cardNumber\" : \"1111-1111-1111-1111\",\n" +
                                                               "\"cardExpiryDate\" : \"2022-06-07\",\n" +
                                                               "\"amount\" : 100.00\n" +
                                                               "} ")));
    }

    @AfterEach
    public void teardown () {
        //wireMockServer.stopRecording ();
        wireMockServer.stop ();
    }

}
