/*
 * Copyright 2023 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
document.addEventListener("DOMContentLoaded", function(event) {
  // Deposit modal client-side validation
  var depositForm = document.querySelector("#deposit-form");
  depositForm.addEventListener("submit", function(e) {
    var isNewAcct = (document.querySelector("#accounts").value == "add");
    document.querySelector("#external_account_num").required = isNewAcct;
    document.querySelector("#external_routing_num").required = isNewAcct;

    if(!depositForm.checkValidity() || document.querySelector("#deposit-amount").value <= 0.00){
      e.preventDefault();
      e.stopPropagation();
    }
    depositForm.classList.add("was-validated");
  });

  // Reset form on cancel event
  document.querySelectorAll(".deposit-cancel").forEach((depositCancel) => {
    depositCancel.addEventListener("click", function () {
      depositForm.reset();
      depositForm.classList.remove("was-validated");
      RefreshModals();
    });
  });

  // Send payment modal client-side validation
  var paymentForm = document.querySelector("#payment-form");
  paymentForm.addEventListener("submit", function(e) {
    // Check if account number is required
    document.querySelector("#contact_account_num").required = (document.querySelector("#payment-accounts").value == "add");

    if(!paymentForm.checkValidity() || document.querySelector("#payment-amount").value <= 0.00){
      e.preventDefault();
      e.stopPropagation();
    }
    paymentForm.classList.add("was-validated");
  });

  // Reset form on cancel event
  document.querySelectorAll(".payment-cancel").forEach((paymentCancel) => {
    paymentCancel.addEventListener("click", function () {
      paymentForm.reset();
      paymentForm.classList.remove("was-validated");
      RefreshModals();
    });
  });

  // Handle new account option in Send Payment modal
  document.querySelector("#payment-accounts").addEventListener("change", function(e) {
    RefreshModals();
  });

  // Handle new account option in Deposit modal
  document.querySelector("#accounts").addEventListener("change", function(e) {
    RefreshModals();
  });


  //Added the toggle Button Logic
  if (!localStorage.getItem("mode")) {
    localStorage.setItem("mode", "light_mode");
  }
  let body = document.querySelector('body');
  let card = document.querySelectorAll('.cardc, .in');
  let cardheading = document.querySelectorAll('.text-transaction-header')
  let nav = document.querySelector('.navbar-top');
  let foot = document.querySelector('.footer');
  let headings = document.querySelectorAll('.text-muted, .header-title, .account-number, #account-user-name, .card-header-title, .sign-in input')
  let td = document.querySelectorAll('td');
  let debit = document.querySelectorAll(".transaction-amount-debit")
  let credit = document.querySelectorAll(".transaction-amount-credit")
  let mode = document.getElementById('mode');
  const modeChange = function(){
    if(mode.innerText === "light_mode"){
        body.style.setProperty("background-color", "#fff", "important");
        body.style.setProperty("color", "#333", "important");
        nav?.style.setProperty("background-color", "#fff", "important");
        foot?.style.setProperty("background-color", "#fff", "important");
        foot?.style.setProperty("color", "#333", "important");
        card[0]?.style.setProperty("background-color", "#F8F8F8", "important");
        card[0]?.style.setProperty("color", "#333", "important");
        card[1]?.style.setProperty("background-color", "#F8F8F8", "important");
        card[1]?.style.setProperty("color", "#333", "important");
        td?.forEach(e => e.style.setProperty("color", "#333"));
        cardheading?.forEach(e => e.style.setProperty("color", "#333", "important"))
        headings?.forEach(e => e.style.setProperty("color", "#333", "important"))
        credit?.forEach(e => e.style.setProperty("color", "#008A20", "important")) 
        debit?.forEach(e => e.style.setProperty("color", "#FF0000", "important")) 
        
        // Reset modal headings and content to light mode
        document.querySelectorAll('.modal-title').forEach(el => el.style.setProperty("color", "#343434", "important"));
        document.querySelectorAll('.modal-content').forEach(el => el.style.setProperty("background-color", "#fff", "important"));
        document.querySelectorAll('.modal label').forEach(el => el.style.setProperty("color", "#444", "important"));
        document.querySelectorAll('.modal-content .text-muted').forEach(el => el.style.setProperty("color", "#444", "important"));
        document.querySelectorAll('.modal-content select, .modal-content input').forEach(el => el.style.setProperty("color", "#333", "important"));
        
        // Transaction table text in light mode
        document.querySelectorAll('.transaction-account, .transaction-label').forEach(el => el.style.setProperty("color", "#333", "important"));
        document.querySelectorAll('.transaction-date p').forEach(el => el.style.setProperty("color", "#333", "important"));
        document.querySelectorAll('.transaction-label-none').forEach(el => el.style.setProperty("color", "#333", "important"));
        document.querySelectorAll('.table-responsive').forEach(el => el.style.setProperty("background-color", "#fff", "important"));
        document.querySelectorAll('.card-table').forEach(el => el.style.setProperty("background-color", "#fff", "important"));
        document.querySelectorAll('.table-sm tbody tr').forEach(el => el.style.setProperty("background-color", "#fff", "important"));
        
        mode.innerText = "dark_mode"
        localStorage.setItem('mode', "light_mode")
        console.log("Switching to light");
    }
    else if(mode.innerText === "dark_mode"){
        body.style.setProperty("background-color", "#111", "important");
        body.style.setProperty("color", "#fff", "important");
        nav?.style.setProperty("background-color", "#111", "important");
        foot?.style.setProperty("background-color", "#111", "important");
        foot?.style.setProperty("color", "#fff", "important");
        card[0]?.style.setProperty("background-color", "#333", "important");
        card[0]?.style.setProperty("color", "#fff", "important");
        card[1]?.style.setProperty("background-color", "#333", "important");
        card[1]?.style.setProperty("color", "#fff", "important");
        td.forEach(e => e.style.setProperty("color", "#fff"));
        cardheading?.forEach(e=>e.style.setProperty("color", "#fff", "important"))
        headings?.forEach(e => e.style.setProperty("color", "#fff", "important"))
        credit?.forEach(e => e.style.setProperty("color", "#008A20", "important"))
        debit?.forEach(e => e.style.setProperty("color", "#FF0000", "important"))
        
        // Set modal headings and content to dark mode
        document.querySelectorAll('.modal-title').forEach(el => el.style.setProperty("color", "#fff", "important"));
        document.querySelectorAll('.modal-content').forEach(el => el.style.setProperty("background-color", "#333", "important")); 
        document.querySelectorAll('.modal label').forEach(el => el.style.setProperty("color", "#eee", "important"));
        document.querySelectorAll('.modal-content .text-muted').forEach(el => el.style.setProperty("color", "#eee", "important"));
        document.querySelectorAll('.modal-content select, .modal-content input').forEach(el => el.style.setProperty("color", "#fff", "important"));
        
        // Transaction table text in dark mode
        document.querySelectorAll('.transaction-account, .transaction-label').forEach(el => el.style.setProperty("color", "#fff", "important"));
        document.querySelectorAll('.transaction-date p').forEach(el => el.style.setProperty("color", "#fff", "important"));
        document.querySelectorAll('.transaction-label-none').forEach(el => el.style.setProperty("color", "#fff", "important"));
        document.querySelectorAll('.card-table-header').forEach(el => el.style.setProperty("background-color", "#222", "important"));
        document.querySelectorAll('.table-responsive').forEach(el => el.style.setProperty("background-color", "#222", "important"));
        document.querySelectorAll('.card-table').forEach(el => el.style.setProperty("background-color", "#222", "important"));
        
        // Apply alternating row colors
        applyTransactionTableDarkMode();
        
        mode.innerText = "light_mode"
        localStorage.setItem('mode', "dark_mode")
        console.log("Switching to dark");
    }
  }
  mode.innerText = localStorage.getItem('mode');
  modeChange();
  document.getElementById("mode").addEventListener("click", modeChange)

  // Function to apply alternating row colors to transaction table in dark mode
  function applyTransactionTableDarkMode() {
    if (localStorage.getItem('mode') === 'dark_mode') {
      // Get all transaction rows
      const transactionRows = document.querySelectorAll('.table-sm tbody tr');
      // Apply alternating colors
      transactionRows.forEach((row, index) => {
        if (index % 2 === 0) {
          row.style.setProperty("background-color", "#2a2a2a", "important");
        } else {
          row.style.setProperty("background-color", "#333", "important");
        }
        // Ensure text is white in all cells
        row.querySelectorAll('td').forEach(cell => {
          cell.style.setProperty("color", "#fff", "important");
        });
      });
      
      // Ensure transaction type icons maintain visibility
      document.querySelectorAll('.transaction-type').forEach(el => {
        el.style.setProperty("color", "#fff", "important");
      });
      
      // Ensure transaction labels are visible
      document.querySelectorAll('.transaction-label, .transaction-account').forEach(el => {
        el.style.setProperty("color", "#fff", "important");
      });
    }
  }
  
  // Apply dark mode styling to transaction table after page load
  setTimeout(applyTransactionTableDarkMode, 500);
  
  // Also apply after any mode change
  document.getElementById("mode").addEventListener("click", function() {
    setTimeout(applyTransactionTableDarkMode, 100);
  });

  function uuidv4() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  };


  // Reset Modals to proper state
  function RefreshModals(){
      paymentSelection = document.querySelector("#payment-accounts").value;
      if (paymentSelection == "add") {
        document.querySelector("#otherAccountInputs").classList.remove("hidden");
      } else {
        document.querySelector("#otherAccountInputs").classList.add("hidden");
      }
      depositSelection = document.querySelector("#accounts").value;
      if (depositSelection == "add") {
        document.querySelector("#otherDepositInputs").classList.remove("hidden");
      } else {
        document.querySelector("#otherDepositInputs").classList.add("hidden");
      }
      // generate new uuids
      document.querySelector("#payment-uuid").value = uuidv4();
      document.querySelector("#deposit-uuid").value = uuidv4();
  }
  RefreshModals();

  // --- Generate PDF Statement Logic ---
  const statementForm = document.getElementById('statement-form');
  const startDateInput = document.getElementById('startDate');
  const endDateInput = document.getElementById('endDate');
  const dateError = document.getElementById('dateError');
  const accountId = document.querySelector('.account-number').textContent.trim(); // Get account ID from page
  console.log('Account ID:', accountId);
  
  // Format today's date as YYYY-MM-DD for the date input
  function getTodayFormatted() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0'); // Add leading zero if needed
    const day = String(today.getDate()).padStart(2, '0'); // Add leading zero if needed
    return `${year}-${month}-${day}`;
  }
  
  // Set max attribute for date inputs to prevent future dates
  const today = getTodayFormatted();
  if (startDateInput) startDateInput.setAttribute('max', today);
  if (endDateInput) endDateInput.setAttribute('max', today);
  
  // Set end date to today when modal opens
  $('#statementModal').on('shown.bs.modal', function() {
    endDateInput.value = today;
  });
  
  // Set account ID in hidden field
  if (document.getElementById('statementAccountId')) {
    document.getElementById('statementAccountId').value = accountId;
  }
  
  // Set up the form action
  if (statementForm) {
    statementForm.action = `/statement/${accountId}/pdf`;
    
    // Add form validation
    statementForm.addEventListener('submit', function(e) {
      console.log('Statement form submitted');
      const startDate = startDateInput.value;
      const endDate = endDateInput.value;
      let isValid = true;

      // Basic validation
      startDateInput.classList.remove('is-invalid');
      endDateInput.classList.remove('is-invalid');
      dateError.classList.add('hidden');

      if (!startDate) {
        startDateInput.classList.add('is-invalid');
        isValid = false;
      }
      if (!endDate) {
        endDateInput.classList.add('is-invalid');
        isValid = false;
      }

      if (startDate && endDate && startDate > endDate) {
        endDateInput.classList.add('is-invalid');
        dateError.classList.remove('hidden');
        isValid = false;
      }
      
      if (!isValid) {
        e.preventDefault();
        return false;
      }
      
      // Close the modal after a short delay to allow the form to submit
      setTimeout(function() {
        $('#statementModal').modal('hide');
        
        // Reset form
        startDateInput.value = '';
        endDateInput.value = '';
      }, 500);
    });
  }

  // Reset statement modal validation on close
  $('#statementModal').on('hidden.bs.modal', function (e) {
    startDateInput.value = '';
    endDateInput.value = '';
    startDateInput.classList.remove('is-invalid');
    endDateInput.classList.remove('is-invalid');
    dateError.classList.add('hidden');
  });
  // --- End Generate PDF Statement Logic ---

  // --- Dark Mode Modal Fix ---
  // Apply dark mode styling when modals are opened
  $('#depositFunds, #sendPayment, #statementModal').on('show.bs.modal', function() {
    // Check if dark mode is active
    if (localStorage.getItem('mode') === 'dark_mode') {
      const modal = $(this);
      modal.find('.modal-content').css('background-color', '#333');
      modal.find('.modal-title').css('color', '#fff');
      modal.find('label').css('color', '#eee');
      modal.find('.text-muted, .text-uppercase').css('color', '#eee');
      modal.find('select, input').css({
        'background-color': '#444',
        'color': '#fff',
        'border-color': '#666'
      });
      modal.find('.close span').css('color', '#fff');
      modal.find('.input-group-text').css({
        'background-color': '#444',
        'color': '#fff',
        'border-color': '#666'
      });
    }
  });
  // --- End Dark Mode Modal Fix ---

  // --- Credit Score Prototype Logic ---
  // Get transaction history from the table
  function calculateCreditScore() {
    console.log('Calculating credit score...');
    
    // Get transaction data from the transaction table
    const transactionRows = document.querySelectorAll('#transaction-list tr');
    const currentBalance = parseFloat(document.getElementById('current-balance').textContent.replace(/[$,]/g, ''));
    
    // If we don't have transactions or balance data yet, retry after a delay
    if (transactionRows.length === 0 || isNaN(currentBalance)) {
      console.log('Transaction data not loaded yet, retrying...');
      setTimeout(calculateCreditScore, 500);
      return;
    }

    // Get the last 3 months worth of transactions
    const transactions = [];
    const transactionTypes = { CREDIT: [], DEBIT: [] };
    
    transactionRows.forEach(row => {
      const dateElement = row.querySelector('.transaction-date p');
      const typeElement = row.querySelector('.transaction-type');
      const amountElement = row.querySelector('.transaction-amount');
      
      if (dateElement && typeElement && amountElement) {
        const type = typeElement.textContent.trim().includes('Credit') ? 'CREDIT' : 'DEBIT';
        const amount = parseFloat(amountElement.textContent.replace(/[+$,]/g, '').replace(/[-]/g, ''));
        
        transactions.push({ type, amount });
        transactionTypes[type].push(amount);
      }
    });

    console.log(`Analyzed ${transactions.length} transactions`);
    
    // Calculate credit factors
    const creditFactors = {
      // Balance factor: Score higher for higher balances, max score at $10,000+
      balance: Math.min(Math.floor(currentBalance / 100), 100),
      
      // Transaction frequency: Score higher for more transactions
      frequency: Math.min(Math.floor(transactions.length * 5), 100),
      
      // Deposit consistency: Score based on regular deposits 
      depositConsistency: calculateDepositConsistencyScore(transactionTypes.CREDIT)
    };
    
    // Update the UI with factor scores
    document.getElementById('balanceFactor').textContent = creditFactors.balance + '/100';
    document.getElementById('frequencyFactor').textContent = creditFactors.frequency + '/100';
    document.getElementById('depositFactor').textContent = creditFactors.depositConsistency + '/100';
    
    // Calculate the final score (300-850 range, like FICO)
    const weightedScore = (
      (creditFactors.balance * 0.4) + 
      (creditFactors.frequency * 0.3) + 
      (creditFactors.depositConsistency * 0.3)
    );
    
    // Map the 0-100 weighted score to 300-850 range
    const finalScore = Math.floor(300 + (weightedScore * 5.5));
    
    // Update the credit score display
    const creditScoreElement = document.getElementById('creditScoreValue');
    // Remove any existing spinner
    const existingSpinner = creditScoreElement.querySelector('.spinner-border');
    if (existingSpinner) {
      existingSpinner.remove();
    }
    creditScoreElement.textContent = finalScore;
    
    // Update the progress bar
    const progressBar = document.getElementById('creditScoreProgress');
    const percentage = ((finalScore - 300) / (850 - 300)) * 100;
    progressBar.style.width = `${percentage}%`;
    progressBar.setAttribute('aria-valuenow', finalScore);
    
    // Set color based on score range for both progress bar and score display
    let colorClass, colorHex;
    if (finalScore < 580) {
      colorClass = 'bg-danger';
      colorHex = '#dc3545'; // Red
    } else if (finalScore < 670) {
      colorClass = 'bg-warning';
      colorHex = '#ffc107'; // Yellow
    } else if (finalScore < 740) {
      colorClass = 'bg-info';
      colorHex = '#17a2b8'; // Blue
    } else {
      colorClass = 'bg-success';
      colorHex = '#28a745'; // Green
    }
    
    // Apply colors
    progressBar.className = 'progress-bar ' + colorClass;
    creditScoreElement.style.color = colorHex;
    creditScoreElement.style.borderBottom = `4px solid ${colorHex}`;
    
    // Update Quick Cash eligibility
    updateQuickCashEligibility(finalScore, currentBalance);
  }
  
  // Helper function to calculate deposit consistency score
  function calculateDepositConsistencyScore(creditTransactions) {
    if (creditTransactions.length < 2) {
      return 30; // Base score for few transactions
    }
    
    // Sort credit transactions by amount (descending)
    const sortedDeposits = [...creditTransactions].sort((a, b) => b - a);
    
    // Look for recurring deposits of similar amounts
    let regularDeposits = 0;
    
    for (let i = 0; i < sortedDeposits.length - 1; i++) {
      const current = sortedDeposits[i];
      const next = sortedDeposits[i + 1];
      
      // If amounts are within 10% of each other, consider it a regular deposit
      if (Math.abs(current - next) / current < 0.1) {
        regularDeposits++;
      }
    }
    
    // Score based on percentage of regular deposits
    const regularityScore = Math.min(
      Math.floor((regularDeposits / (sortedDeposits.length - 1)) * 100), 
      100
    );
    
    return Math.max(30, regularityScore); // Minimum of 30 points
  }
  
  // Update Quick Cash eligibility UI
  function updateQuickCashEligibility(score, balance) {
    const eligibilityStatus = document.getElementById('eligibilityStatus');
    const quickCashOptions = document.getElementById('quickCashOptions');
    const quickCashIneligible = document.getElementById('quickCashIneligible');
    const quickCashAmount = document.getElementById('quickCashAmount');
    
    // Clear loading status
    eligibilityStatus.textContent = '';
    
    // Determine eligibility (score >= 630 and balance >= $200)
    const isEligible = score >= 630 && balance >= 200;
    
    if (isEligible) {
      quickCashOptions.style.display = 'block';
      quickCashIneligible.style.display = 'none';
      
      // Calculate Quick Cash amount based on score and balance
      let amount = 500; // Default
      
      if (score >= 750) {
        amount = Math.min(Math.floor(balance * 0.5), 5000);
      } else if (score >= 700) {
        amount = Math.min(Math.floor(balance * 0.3), 2500);
      } else if (score >= 650) {
        amount = Math.min(Math.floor(balance * 0.2), 1000);
      }
      
      // Round to nearest 100
      amount = Math.floor(amount / 100) * 100;
      quickCashAmount.textContent = amount;
    } else {
      quickCashOptions.style.display = 'none';
      quickCashIneligible.style.display = 'block';
    }
  }
  
  // Mock function for Quick Cash request
  window.requestQuickCash = function() {
    const amount = document.getElementById('quickCashAmount').textContent;
    
    // Show a small loading state
    const button = document.getElementById('requestQuickCash');
    const originalText = button.innerHTML;
    button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Processing...';
    button.disabled = true;
    
    // Simulate API request delay
    setTimeout(() => {
      // Create success message alert
      const alertContainer = document.createElement('div');
      alertContainer.classList.add('row', 'col-lg-12', 'align-items-start');
      alertContainer.id = 'quick-cash-alert';
      
      const alertHTML = `
        <div class="col-lg">
          <div class="card snackbar-card">
            <div class="card-body snackbar-body">
              <div class="row align-items-center">
                <div class="col">
                  <h5 class="alert-message-container">
                    <div class="check-mark-container">
                      <span class="snackbar-close material-icons">check_circle</span>
                    </div>
                    Your Quick Cash request for $${amount} has been approved and deposited to your account!
                  </h5>
                </div>
                <div class="button-icon col-auto">
                  <span class="snackbar-close material-icons" onclick="document.getElementById('quick-cash-alert').remove();">close</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      `;
      
      alertContainer.innerHTML = alertHTML;
      
      // Add to top of page
      const mainContent = document.querySelector('main.container');
      mainContent.insertBefore(alertContainer, mainContent.firstChild);

      // Auto-dismiss alert after 5 seconds
      setTimeout(function() {
        $('#quick-cash-alert').fadeOut('slow', function() {
          $(this).remove();
        });
      }, 5000);
      
      // Change button to "Approved" state and keep it disabled
      button.innerHTML = '<span class="material-icons">check_circle</span> Approved';
      button.classList.remove('btn-success');
      button.classList.add('btn-secondary');
      button.disabled = true;
      
      // Optionally, update the balance to simulate receiving funds
      const currentBalanceEl = document.getElementById('current-balance');
      const currentBalance = parseFloat(currentBalanceEl.textContent.replace(/[$,]/g, ''));
      const newBalance = currentBalance + parseInt(amount);
      
      // Format with dollar sign and commas
      currentBalanceEl.textContent = '$' + newBalance.toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      });
      
    }, 2000);
  };
  
  // Call the function to calculate credit score
  setTimeout(calculateCreditScore, 1000);
  // --- End Credit Score Prototype Logic ---

});

// --- Send Statement by Email Logic ---
// Moving this function outside the DOMContentLoaded event listener
// to make it globally accessible
function sendStatementByEmail() {
  const startDate = document.getElementById('startDate').value;
  const endDate = document.getElementById('endDate').value;
  const accountId = document.querySelector('.account-number').textContent.trim();

  if (!startDate || !endDate) {
      alert('Please select start and end dates');
      return;
  }

  // Show loading indicator
  const button = document.querySelector('#sendStatementEmail');
  const originalText = button.textContent;
  button.textContent = 'Sending...';
  button.disabled = true;

  fetch(`/send_statement_email/${accountId}`, {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json',
      },
      body: JSON.stringify({
          startDate: startDate,
          endDate: endDate
      })
  })
  .then(response => response.json())
  .then(data => {
      if (data.error) {
          throw new Error(data.error);
      }
      alert('Statement sent to your registered email!');
  })
  .catch(error => {
      console.error('Error:', error);
      alert('Failed to send statement: ' + error.message);
  })
  .finally(() => {
      // Reset button
      button.textContent = originalText;
      button.disabled = false;
  });
}
// --- End Send Statement by Email Logic ---
