package anthos.samples.bankofanthos.transactionhistory;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.awt.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
public class StatementPdfGenerator {
    
    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 15;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("$#,##0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    // Add color constants
    private static final PDColor GREEN_COLOR = new PDColor(new float[]{0.0f, 0.5f, 0.0f}, PDDeviceRGB.INSTANCE);
    private static final PDColor RED_COLOR = new PDColor(new float[]{0.8f, 0.0f, 0.0f}, PDDeviceRGB.INSTANCE);
    private static final PDColor GRAY_COLOR = new PDColor(new float[]{0.5f, 0.5f, 0.5f}, PDDeviceRGB.INSTANCE);
    
    // Update the TABLE_WIDTHS to better accommodate the balance column
    private static final float[] TABLE_WIDTHS = {90f, 70f, 130f, 100f, 100f};  // Adjusted widths
    private static final float TABLE_ROW_HEIGHT = 20f;
    private static final float TABLE_CELL_PADDING = 5f;

    private static final Logger LOGGER = LogManager.getLogger(StatementPdfGenerator.class);

    private final String localRoutingNum;
    
    @Autowired
    public StatementPdfGenerator(@Value("${LOCAL_ROUTING_NUM}") String localRoutingNum) {
        this.localRoutingNum = localRoutingNum;
    }
    
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
            
            addTransactionsToPage(document, page, pageTransactions, statement.getAccountId(), pageNum, statement);
        }
        
        // Add summary to the last page
        PDPage lastPage = document.getPage(document.getNumberOfPages() - 1);
        addSummaryToPage(document, lastPage, statement);
    }
    
    private void addTransactionsToPage(PDDocument document, PDPage page, List<Transaction> transactions, 
                                      String accountId, int pageNum, BankStatement statement) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page, 
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            float y = page.getMediaBox().getHeight() - MARGIN;
            float tableStartX = MARGIN;
            
            // Adjust starting Y position for first page
            if (pageNum == 0) {
                y -= LINE_HEIGHT * 12;
            } else {
                // Add continuation header
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                content.newLineAtOffset(MARGIN, y);
                content.showText("Transaction History (Continued)");
                content.endText();
                y -= LINE_HEIGHT * 2;
            }

            // Draw table headers
            String[] headers = {"Date", "Type", "Account", "Amount", "Balance"};
            float currentX = tableStartX;
            for (int i = 0; i < headers.length; i++) {
                drawTableCell(content, currentX, y, TABLE_WIDTHS[i], TABLE_ROW_HEIGHT, 
                             headers[i], true, null);
                currentX += TABLE_WIDTHS[i];
            }
            y -= TABLE_ROW_HEIGHT;

            // Add this near the top of addTransactionsToPage method:
            double runningBalance = statement.getOpeningBalance() / 100.0;

            // Draw transaction rows
            for (Transaction txn : transactions) {
                currentX = tableStartX;
                // Update the isCredit check to also verify routing numbers:
                boolean isCredit = txn.getToAccountNum().equals(accountId) && 
                                  txn.getToRoutingNum().equals(localRoutingNum);
                PDColor amountColor = isCredit ? GREEN_COLOR : RED_COLOR;
                
                // Date
                drawTableCell(content, currentX, y, TABLE_WIDTHS[0], TABLE_ROW_HEIGHT, 
                             DATE_FORMAT.format(txn.getTimestamp()), false, null);
                currentX += TABLE_WIDTHS[0];
                
                // Type
                drawTableCell(content, currentX, y, TABLE_WIDTHS[1], TABLE_ROW_HEIGHT, 
                             isCredit ? "CREDIT" : "DEBIT", false, amountColor);
                currentX += TABLE_WIDTHS[1];
                
                // Account
                String counterpartyAccount = isCredit ? txn.getFromAccountNum() : txn.getToAccountNum();
                String counterpartyRouting = isCredit ? txn.getFromRoutingNum() : txn.getToRoutingNum();
                if (!counterpartyRouting.equals(localRoutingNum)) {
                    counterpartyAccount += " (External)";
                }
                drawTableCell(content, currentX, y, TABLE_WIDTHS[2], TABLE_ROW_HEIGHT, 
                             counterpartyAccount, false, null);
                currentX += TABLE_WIDTHS[2];
                
                // Amount
                String formattedAmount;
                if (isCredit) {
                    formattedAmount = "+" + MONEY_FORMAT.format(txn.getAmount() / 100.0);
                } else {
                    formattedAmount = "-" + MONEY_FORMAT.format(Math.abs(txn.getAmount()) / 100.0);
                }
                drawTableCell(content, currentX, y, TABLE_WIDTHS[3], TABLE_ROW_HEIGHT, 
                             formattedAmount, false, amountColor);
                
                // Then inside the transaction loop, add a balance column:
                if (isCredit) {
                    runningBalance += txn.getAmount() / 100.0;
                } else {
                    runningBalance -= txn.getAmount() / 100.0;
                }

                // Add balance column to show running total
                float balanceX = currentX + TABLE_WIDTHS[3];
                drawTableCell(content, balanceX, y, TABLE_WIDTHS[3], TABLE_ROW_HEIGHT,
                             MONEY_FORMAT.format(runningBalance), false, null);

                // Add inside the transaction loop:
                LOGGER.debug("Processing transaction: ID={}, Amount={}, isCredit={}, Balance={}",
                    txn.getTransactionId(),
                    txn.getAmount(),
                    isCredit,
                    runningBalance);

                y -= TABLE_ROW_HEIGHT;
            }

            // Page number
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

    private void drawTableCell(PDPageContentStream content, float x, float y, float width, float height, 
                              String text, boolean isHeader, PDColor textColor) throws IOException {
        // Draw cell border
        content.setStrokingColor(GRAY_COLOR);
        content.addRect(x, y, width, height);
        content.stroke();

        // Add text
        content.beginText();
        content.setFont(isHeader ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, 10);
        if (textColor != null) {
            content.setNonStrokingColor(textColor);
        }
        content.newLineAtOffset(x + TABLE_CELL_PADDING, y + TABLE_CELL_PADDING);
        content.showText(text != null ? text : "");
        content.endText();
        
        // Reset color
        content.setNonStrokingColor(Color.BLACK);
    }
}