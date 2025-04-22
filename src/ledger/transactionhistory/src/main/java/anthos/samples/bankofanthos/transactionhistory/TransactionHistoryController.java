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
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/statement/{accountId}")
    public ResponseEntity<?> generateStatement(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable String accountId,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate
        //@RequestParam String localRoutingNum //local routing number
        ) 
                {
                    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                        bearerToken = bearerToken.split("Bearer ")[1];
                    }
        
                    try {
                        // Check that the bearer token is valid
                        DecodedJWT jwt = verifier.verify(bearerToken);
        
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
                        BankStatement statement = statementService.generateStatement(accountId, localRoutingNum, startDate, endDate);

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
}

//eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoidGVzdHVzZXIiLCJhY2N0IjoiMTAxMTIyNjExMSIsIm5hbWUiOiJUZXN0IFVzZXIiLCJpYXQiOjE3NDUwNjU4NjgsImV4cCI6MTc0NTA2OTQ2OH0.ou7i3FAiH3w2OGUeUF3YJdu5x6BUrfziDQJo9nxqntJU_wEM3Res6uS4A7V6x5jQTRuYOd__Ma4NH_ho1kefPCxWdVI_XKB7mfWvcVWa6sdBOEzfd8mHTkvmMEehuCFAOPOtwU6MoLSWjghUzuNjCcP2_EoK8xUhsgp_SRQhjMpt8Y-8UxBvdfRz9nLByuPcOKhHfMDnPoodw-iTibYwmSZlMMSXO6pnsEtr3XplRLYMBK_r9KilZ181fqpHWOrM9wWXAwVaCye4s4jb8tDQqrZzyS8UdLRXNCnyqf285mwy34D5sHeX36u51Yl-OtZL9pWQz-3gfprOkflhwwi_tNbp_fNEzOjVVkG4voLG8cWRiE-_g8d8dTmsC_5-1E_zCX7XuZ5bYLEsI80Sqr7Q-SK1tRpZDkagc1z8Q4FSjoeWi6yLDC-Gdk1U8VFFj9inzmfLa8qEcqEvqzwJgvzeg1EJ0CYFQMR0ZK9Z8CCwOzFMvsBPzEIKiK1AiDxDbulQezlO9kVFIk7JCbwI4wd7tJW_qY3uCkdUCd52rtIktYZjsJTc7L2bv34jng_2vPiVwJVFk61yTKiueiNQXKYokco2OCLdykChO7W4i5Vy9b7Pft-M7Sn1kZoYWZWSuHNFurChZ0s8daw5L1cre0d4o6HIKETKzwrUweh7SV0ZXag
