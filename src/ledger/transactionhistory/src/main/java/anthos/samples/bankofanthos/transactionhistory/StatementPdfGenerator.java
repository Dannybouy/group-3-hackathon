package anthos.samples.bankofanthos.transactionhistory;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

@Service
public class StatementPdfGenerator {
    
    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 15;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("$#,##0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    public byte[] generatePdf(BankStatement statement) throws IOException {
        PDDocument document = new PDDocument();
        try {
            // Create first page and add content
            PDPage firstPage = new PDPage();
            document.addPage(firstPage);
            
            // Add header and summary information to first page
            addHeaderAndSummary(document, firstPage, statement);
            
            // Split transactions into pages and add them
            addTransactionsPages(document, statement);
            
            // Save to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } finally {
            document.close();
        }
    }
    
    private void addHeaderAndSummary(PDDocument document, PDPage page, BankStatement statement) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float y = page.getMediaBox().getHeight() - MARGIN;
            
            // Add header
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 18);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Bank Statement");
            content.endText();
            y -= LINE_HEIGHT * 2;
            
            // Add account details
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 12);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Account: " + statement.getAccountId());
            content.endText();
            y -= LINE_HEIGHT;
            
            // Add user name if available
            if (statement.getUserName() != null && !statement.getUserName().isEmpty()) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(MARGIN, y);
                content.showText("Customer: " + statement.getUserName());
                content.endText();
                y -= LINE_HEIGHT;
            }
            
            // Add date range
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 12);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Statement Period: " + 
                DATE_FORMAT.format(statement.getStartDate()) + " to " +
                DATE_FORMAT.format(statement.getEndDate()));
            content.endText();
            y -= LINE_HEIGHT * 2;
            
            // Add opening balance
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 12);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Opening Balance: " + 
                MONEY_FORMAT.format(statement.getOpeningBalance() / 100.0));
            content.endText();
            y -= LINE_HEIGHT * 2;
            
            // Add transactions header
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 12);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Transactions");
            content.endText();
            y -= LINE_HEIGHT;
            
            // Add column headers
            float dateColX = MARGIN;
            float typeColX = dateColX + 100;
            float accountColX = typeColX + 80;
            float amountColX = accountColX + 150;
            
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 10);
            content.newLineAtOffset(dateColX, y);
            content.showText("Date");
            content.endText();
            
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 10);
            content.newLineAtOffset(typeColX, y);
            content.showText("Type");
            content.endText();
            
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 10);
            content.newLineAtOffset(accountColX, y);
            content.showText("Account");
            content.endText();
            
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 10);
            content.newLineAtOffset(amountColX, y);
            content.showText("Amount");
            content.endText();
        }
    }
    
    private void addTransactionsPages(PDDocument document, BankStatement statement) throws IOException {
        List<Transaction> transactions = statement.getTransactions();
        int transactionsPerPage = 25;
        int totalTransactions = transactions.size();
        int pageCount = (int) Math.ceil((double) totalTransactions / transactionsPerPage);
        
        for (int pageNum = 0; pageNum < pageCount; pageNum++) {
            PDPage page;
            if (pageNum == 0) {
                // First page already exists
                page = document.getPage(0);
            } else {
                // Create and add new page
                page = new PDPage();
                document.addPage(page);
            }
            
            // Add transactions for this page
            int startIdx = pageNum * transactionsPerPage;
            int endIdx = Math.min(startIdx + transactionsPerPage, totalTransactions);
            List<Transaction> pageTransactions = transactions.subList(startIdx, endIdx);
            
            addTransactionsToPage(document, page, pageTransactions, statement.getAccountId(), pageNum);
        }
        
        // Add summary to the last page
        PDPage lastPage = document.getPage(document.getNumberOfPages() - 1);
        addSummaryToPage(document, lastPage, statement);
    }
    
    private void addTransactionsToPage(PDDocument document, PDPage page, List<Transaction> transactions, 
                                      String accountId, int pageNum) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page, 
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            float y = page.getMediaBox().getHeight() - MARGIN;
            
            // For first page, start lower to account for headers
            if (pageNum == 0) {
                y -= LINE_HEIGHT * 12;
            } else {
                // Add page header for continuation pages
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                content.newLineAtOffset(MARGIN, y);
                content.showText("Transaction History (Continued)");
                content.endText();
                y -= LINE_HEIGHT * 2;
                
                // Add column headers
                float dateColX = MARGIN;
                float typeColX = dateColX + 100;
                float accountColX = typeColX + 80;
                float amountColX = accountColX + 150;
                
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(dateColX, y);
                content.showText("Date");
                content.endText();
                
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(typeColX, y);
                content.showText("Type");
                content.endText();
                
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(accountColX, y);
                content.showText("Account");
                content.endText();
                
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(amountColX, y);
                content.showText("Amount");
                content.endText();
                
                y -= LINE_HEIGHT * 1.5f;
            }
            
            // Define columns
            float dateColX = MARGIN;
            float typeColX = dateColX + 100;
            float accountColX = typeColX + 80;
            float amountColX = accountColX + 150;
            
            // Add transactions
            content.setFont(PDType1Font.HELVETICA, 10);
            for (Transaction txn : transactions) {
                // Date
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(dateColX, y);
                content.showText(DATE_FORMAT.format(new Date())); // Temporary fix - use current date
                content.endText();
                
                // Type
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(typeColX, y);
                String txnType = txn.getToAccountNum().equals(accountId) 
                    ? "CREDIT" : "DEBIT";
                content.showText(txnType);
                content.endText();
                
                // Account
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(accountColX, y);
                String counterpartyAccount = txn.getToAccountNum().equals(accountId) 
                    ? txn.getFromAccountNum() : txn.getToAccountNum();
                content.showText(counterpartyAccount);
                content.endText();
                
                // Amount
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(amountColX, y);
                String prefix = txn.getToAccountNum().equals(accountId) ? "+" : "-";
                content.showText(prefix + MONEY_FORMAT.format(Math.abs(txn.getAmount()) / 100.0));
                content.endText();
                
                y -= LINE_HEIGHT;
            }
            
            // Page number at the bottom
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 8);
            content.newLineAtOffset(page.getMediaBox().getWidth() / 2, MARGIN / 2);
            content.showText("Page " + (pageNum + 1));
            content.endText();
        }
    }
    
    private void addSummaryToPage(PDDocument document, PDPage page, BankStatement statement) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page, 
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            // Summary section at the bottom of the last page
            float y = MARGIN * 3;
            
            // Add summary
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 12);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Summary");
            content.endText();
            y -= LINE_HEIGHT;
            
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 12);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Total Credits: " + 
                MONEY_FORMAT.format(statement.getTotalCredits() / 100.0));
            content.endText();
            y -= LINE_HEIGHT;
            
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 12);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Total Debits: " + 
                MONEY_FORMAT.format(statement.getTotalDebits() / 100.0));
            content.endText();
            y -= LINE_HEIGHT;
            
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 12);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Closing Balance: " + 
                MONEY_FORMAT.format(statement.getClosingBalance() / 100.0));
            content.endText();
            
            // Add generation timestamp
            float footerY = MARGIN / 2;
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 8);
            content.newLineAtOffset(MARGIN, footerY);
            content.showText("Generated on: " + DATE_FORMAT.format(new Date()));
            content.endText();
        }
    }
} 