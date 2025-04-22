// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package anthos.samples.bankofanthos.transactionhistory;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository class for performing queries on the Transaction database
 */
@Repository
public interface TransactionRepository
        extends CrudRepository<Transaction, Long> {

    /**
     * Returns the id of the latest transaction, or NULL if none exist.
     */
    @Query("SELECT MAX(transactionId) FROM Transaction")
    Long latestTransactionId();

    @Query("SELECT t FROM Transaction t "
        + " WHERE (t.fromAccountNum=?1 AND t.fromRoutingNum=?2) "
        + "   OR (t.toAccountNum=?1 AND t.toRoutingNum=?2) "
        + " ORDER BY t.timestamp DESC")
    LinkedList<Transaction> findForAccount(String accountNum,
                                           String routingNum,
                                           Pageable pager);

    @Query("SELECT t FROM Transaction t "
        + " WHERE t.transactionId > ?1 ORDER BY t.transactionId ASC")
    List<Transaction> findLatest(long latestTransaction);

    /*We need to add new methods to fetch transactions for a specific date range.
    We will use the @Query annotation to define the SQL query.
    We will use the ?1, ?2, ?3, etc. to bind the parameters to the query.

    findTransactionsForDateRange: used to fetch all transactions for a specific account number and routing number that occurred between a given start date and end date.
    */
    @Query("SELECT t FROM Transaction t "
        + " WHERE (t.fromAccountNum = ?1 AND t.fromRoutingNum = ?2) "
        + "   OR (t.toAccountNum = ?1 AND t.toRoutingNum = ?2) "
        + "   AND t.timestamp BETWEEN ?3 AND ?4 "
        + " ORDER BY t.timestamp ASC")
    List<Transaction> findTransactionsForDateRange(String accountNum, String routingNum, Date startDate, Date endDate);

    /*
    getBalanceAsOf: used to fetch the balance of an account as of a specific date.
    */
    @Query(value = "SELECT " +
           "(SELECT COALESCE(SUM(AMOUNT), 0) FROM TRANSACTIONS " +
           "WHERE TO_ACCT = ?1 AND TO_ROUTE = ?2 " +
           "AND TIMESTAMP < ?3) - " +
           "(SELECT COALESCE(SUM(AMOUNT), 0) FROM TRANSACTIONS " +
           "WHERE FROM_ACCT = ?1 AND FROM_ROUTE = ?2 " +
           "AND TIMESTAMP < ?3)",
           nativeQuery = true)
    Long getBalanceAsOf(String accountNum, String routingNum, Date date);
}