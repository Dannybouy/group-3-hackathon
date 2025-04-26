/*
 * Copyright 2020, Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package anthos.samples.bankofanthos.transactionhistory;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ContentDisposition;

/**
 * Controller for the TransactionHistory service.
 *
 * Functions to show the transaction history for each user account.
 */
@RestController
public final class TransactionHistoryController {

    private static final Logger LOGGER =
        LogManager.getLogger(TransactionHistoryController.class);

    @Autowired
    private TransactionRepository dbRepo;

    @Value("${EXTRA_LATENCY_MILLIS:#{null}}")
    private Integer extraLatencyMillis;
    @Value("${HISTORY_LIMIT:100}")
    private Integer historyLimit;
    private String version;
    private String localRoutingNum;

    private JWTVerifier verifier;
    private LedgerReader ledgerReader;
    private LoadingCache<String, Deque<Transaction>> cache;

    /**
     * Constructor.
     *
     * Initializes JWT verifier and a connection to the bank ledger.
     */
    @Autowired
    public TransactionHistoryController(LedgerReader reader,
            StackdriverMeterRegistry meterRegistry,
            JWTVerifier verifier,
            @Value("${PUB_KEY_PATH}") final String publicKeyPath,
            LoadingCache<String, Deque<Transaction>> cache,
            @Value("${LOCAL_ROUTING_NUM}") final String localRoutingNum,
            @Value("${VERSION}") final String version) {
        this.version = version;
        this.localRoutingNum = localRoutingNum;
        // Initialize JWT verifier.
        this.verifier = verifier;
        // Initialize cache
        this.cache = cache;
        GuavaCacheMetrics.monitor(meterRegistry, this.cache, "Guava");
        // Initialize transaction processor.
        this.ledgerReader = reader;
        LOGGER.debug("Initialized transaction processor");
        this.ledgerReader.startWithCallback(transaction -> {
            final String fromId = transaction.getFromAccountNum();
            final String fromRouting = transaction.getFromRoutingNum();
            final String toId = transaction.getToAccountNum();
            final String toRouting = transaction.getToRoutingNum();

            if (fromRouting.equals(localRoutingNum)
                    && this.cache.asMap().containsKey(fromId)) {
                processTransaction(fromId, transaction);
            }
            if (toRouting.equals(localRoutingNum)
                    && this.cache.asMap().containsKey(toId)) {
                processTransaction(toId, transaction);
            }
        });
    }

    /**
     * Helper function to add a single transaction to the internal cache
     *
     * @param accountId   the accountId associated with the transaction
     * @param transaction the full transaction object
     */
    private void processTransaction(String accountId, Transaction transaction) {
        LOGGER.debug("Modifying transaction cache: " + accountId);
        Deque<Transaction> tList = this.cache.asMap()
                                             .get(accountId);
        tList.addFirst(transaction);
        // Drop old transactions
        if (tList.size() > historyLimit) {
            tList.removeLast();
        }
    }

   /**
     * Version endpoint.
     *
     * @return  service version string
     */
    @GetMapping("/version")
    public ResponseEntity version() {
        return new ResponseEntity<>(version, HttpStatus.OK);
    }

    /**
     * Readiness probe endpoint.
     *
     * @return HTTP Status 200 if server is ready to receive requests.
     */
    @GetMapping("/ready")
    @ResponseStatus(HttpStatus.OK)
    public String readiness() {
        return "ok";
    }

    /**
     * Liveness probe endpoint.
     *
     * @return HTTP Status 200 if server is healthy and serving requests.
     */
    @GetMapping("/healthy")
    public ResponseEntity liveness() {
        if (!ledgerReader.isAlive()) {
            // background thread died.
            LOGGER.error("Ledger reader not healthy");
            return new ResponseEntity<>("Ledger reader not healthy",
                                              HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>("ok", HttpStatus.OK);
    }

    /**
     * Return a list of transactions for the specified account.
     *
     * The currently authenticated user must be allowed to access the account.
     * @param bearerToken  HTTP request 'Authorization' header
     * @param accountId    the account to get transactions for.
     * @return             a list of transactions for this account.
     */
    @GetMapping("/transactions/{accountId}")
    public ResponseEntity<?> getTransactions(
            @RequestHeader("Authorization") String bearerToken,
            @PathVariable String accountId) {

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken = bearerToken.split("Bearer ")[1];
        }
        try {
            DecodedJWT jwt = verifier.verify(bearerToken);
            // Check that the authenticated user can access this account.
            if (!accountId.equals(jwt.getClaim("acct").asString())) {
                LOGGER.error("Failed to retrieve account transactions: "
                    + "not authorized");
                return new ResponseEntity<>("not authorized",
                                                  HttpStatus.UNAUTHORIZED);
            }

            // Load from cache
            Deque<Transaction> historyList = cache.get(accountId);

            // Set artificial extra latency.
            LOGGER.debug("Setting artificial latency");
            if (extraLatencyMillis != null) {
                try {
                    Thread.sleep(extraLatencyMillis);
                } catch (InterruptedException e) {
                    // Fake latency interrupted. Continue.
                }
            }

            return new ResponseEntity<Collection<Transaction>>(
                    historyList, HttpStatus.OK);
        } catch (JWTVerificationException e) {
            LOGGER.error("Failed to retrieve account transactions: "
                + "not authorized");
            return new ResponseEntity<>("not authorized",
                                              HttpStatus.UNAUTHORIZED);
        } catch (ExecutionException | UncheckedExecutionException e) {
            LOGGER.error("Cache error");
            return new ResponseEntity<>("cache error",
                                              HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Autowired
    private StatementService statementService;
    
    @Autowired
    private StatementPdfGenerator statementPdfGenerator;

    @GetMapping("/statement/{accountId}")
    public ResponseEntity<?> generateStatement(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable String accountId,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate
        ) 
    {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken = bearerToken.split("Bearer ")[1];
        }

        try {
            // Check that the bearer token is valid
            DecodedJWT jwt = verifier.verify(bearerToken);

            // Extract user name from JWT
            String userName = jwt.getClaim("name").asString();

            // Check that the authenticated user can access this account
            if (!accountId.equals(jwt.getClaim("acct").asString())) {
                LOGGER.error("Failed to generate statement: "
                    + "not authorized");
                return new ResponseEntity<>("not authorized", HttpStatus.UNAUTHORIZED);
            }

            // Validate the dates
            if(startDate.after(endDate)) {
                LOGGER.error("Start date cannot be after end date");
                return new ResponseEntity<>("Start date must be before end date", HttpStatus.BAD_REQUEST);
            }

            // Generate the statement
            BankStatement statement = statementService.generateStatement(accountId, userName, localRoutingNum, startDate, endDate);

            return new ResponseEntity<>(statement, HttpStatus.OK);

        } catch (JWTVerificationException e) {
            LOGGER.error("Failed to generate statement: not authorized");
            return new ResponseEntity<>("not authorized", HttpStatus.UNAUTHORIZED);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to generate statement: invalid date range");
            return new ResponseEntity<>("Invalid date range", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            LOGGER.error("Failed to generate statement: " + e.getMessage());
            return new ResponseEntity<>("Failed to generate statement", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Generate a PDF bank statement for the specified account and date range.
     *
     * @param bearerToken  HTTP request 'Authorization' header
     * @param accountId    the account to generate statement for
     * @param startDate    start date for the statement period
     * @param endDate      end date for the statement period
     * @return            PDF bank statement data or error response
     */
    @GetMapping(value = "/statement/{accountId}/pdf", 
               produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> generateStatementPdf(
            @RequestHeader("Authorization") String bearerToken,
            @PathVariable String accountId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken = bearerToken.split("Bearer ")[1];
        }
        
        try {
            // Verify JWT token
            DecodedJWT jwt = verifier.verify(bearerToken);
            
            // Check authorization
            if (!accountId.equals(jwt.getClaim("acct").asString())) {
                LOGGER.error("Failed to generate statement PDF: not authorized");
                return new ResponseEntity<>("not authorized",
                    HttpStatus.UNAUTHORIZED);
            }
            
            // Validate dates
            if(startDate.after(endDate)) {
                LOGGER.error("Start date cannot be after end date");
                return new ResponseEntity<>("Start date must be before end date", 
                    HttpStatus.BAD_REQUEST);
            }
            
            // Generate the statement
            String userName = jwt.getClaim("name").asString();
            BankStatement statement = statementService.generateStatement(
                accountId, 
                userName,
                localRoutingNum, 
                startDate, 
                endDate
            );
            
            // Generate PDF
            byte[] pdfBytes = statementPdfGenerator.generatePdf(statement);
            
            // Return PDF with enhanced headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition
                .attachment()
                .filename("bank_statement.pdf")
                .build());
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
            headers.add("X-Content-Type-Options", "nosniff");
            
            return ResponseEntity
                .ok()
                .headers(headers)
                .body(pdfBytes);
                
        } catch (JWTVerificationException e) {
            LOGGER.error("Failed to generate statement PDF: not authorized");
            return new ResponseEntity<>("not authorized", HttpStatus.UNAUTHORIZED);
        } catch (IOException e) {
            LOGGER.error("Failed to generate statement PDF: " + e.getMessage());
            return new ResponseEntity<>("Error generating PDF", 
                HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            LOGGER.error("Failed to generate statement PDF: " + e.getMessage());
            return new ResponseEntity<>("Failed to generate statement PDF", 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

