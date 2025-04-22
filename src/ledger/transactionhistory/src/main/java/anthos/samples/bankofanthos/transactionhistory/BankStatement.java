package anthos.samples.bankofanthos.transactionhistory;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BankStatement {

    /* We are using JsonProperty to help convert the Java object to JSON and vice   versa so that the JSON is more readable to the frontend user.
        We are creating different fields that will be in the bank statement.
    */
    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("startDate")
    private Date startDate;

    @JsonProperty("endDate")
    private Date endDate;

    @JsonProperty("openingBalance")
    private Long openingBalance;

    @JsonProperty("closingBalance")
    private Long closingBalance;

    @JsonProperty("transactions")
    private List<Transaction> transactions;

    @JsonProperty("totalCredits")
    private Long totalCredits;

    @JsonProperty("totalDebits")
    private Long totalDebits;

   // We are creating a constructor for the BankStatement class.
    public BankStatement(String accountId, Date startDate, Date endDate, Long openingBalance, Long closingBalance, List<Transaction> transactions, Long totalCredits, Long totalDebits) {
        this.accountId = accountId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.openingBalance = openingBalance;
        this.closingBalance = closingBalance;
        this.transactions = transactions;
        this.totalCredits = totalCredits;
        this.totalDebits = totalDebits;

    }

    // We are creating a getter for the BankStatement class.
    public String getAccountId() {
        return accountId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Long getOpeningBalance() { return openingBalance; }

    public Long getClosingBalance() { return closingBalance; }

    public List<Transaction> getTransactions() { return transactions; }

    public Long getTotalCredits() { return totalCredits; }

    public Long getTotalDebits() { return totalDebits; }


}
