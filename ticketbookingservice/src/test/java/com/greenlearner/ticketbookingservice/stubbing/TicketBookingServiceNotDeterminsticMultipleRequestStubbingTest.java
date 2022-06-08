package com.greenlearner.ticketbookingservice.stubbing;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.greenlearner.ticketbookingservice.dto.CardDetails;
import com.greenlearner.ticketbookingservice.dto.TicketBookingPaymentRequest;
import com.greenlearner.ticketbookingservice.dto.TicketBookingResponse;
import com.greenlearner.ticketbookingservice.gateway.PaymentProcessorGateway;
import com.greenlearner.ticketbookingservice.service.TicketBookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.RandomAccess;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.greenlearner.ticketbookingservice.dto.TicketBookingResponse.BookingResponseStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Bishesh
 */
public class TicketBookingServiceNotDeterminsticMultipleRequestStubbingTest {

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

       stubFor (post (urlPathEqualTo ("/payments"))
                        .withRequestBody (matchingJsonPath ("cardNumber"))
                        .withRequestBody (matchingJsonPath ("cardExpiryDate"))
                        .withRequestBody (matchingJsonPath ("amount"))
                        .willReturn (okJson ("{\n" +
                                                     "\"paymentResponseStatus\" : \"SUCCESS\"\n" +
                                                     "}")));

        stubFor (get (urlPathMatching ("/fraudCheck/.*")).willReturn (okJson ("{\n" +
                                                                                      "\"blacklisted\" : false\n" +
                                                                                      "}")));
        List<TicketBookingPaymentRequest> reuqests = IntStream.range (0, 10).mapToObj (this::generateTicketBooking).collect (Collectors.toList ( ));
        List<TicketBookingResponse> response = tbs.batchPayment (reuqests);

        assertThat (response).hasSize (reuqests.size ());
        response.forEach (r -> assertThat (r.getBookingResponseStatus ()).isEqualTo (SUCCESS));
        verify (5, getRequestedFor (urlPathMatching ("/fraudCheck/.*")));
        verify (10, postRequestedFor (urlPathEqualTo ("/payments")));
        //assertThat (response).isEqualToComparingFieldByField (new TicketBookingResponse ("1111", "3333", SUCCESS));
    }

    public TicketBookingPaymentRequest generateTicketBooking(int i)
    {

        CardDetails cardDetails = new CardDetails (String.valueOf (new Random().nextLong ()), LocalDate.of (2022, i+1, i+1));
        TicketBookingPaymentRequest ticketBookingPaymentRequest = new TicketBookingPaymentRequest
                (String.valueOf (new Random ().nextInt ()),new Random ().nextDouble (),cardDetails);
        if(i % 2 == 0)
        {
            ticketBookingPaymentRequest.setFraudAlert (true);
        }
        return ticketBookingPaymentRequest;
    }

    @AfterEach
    public void teardown () {
        wireMockServer.stop ();
    }

}
