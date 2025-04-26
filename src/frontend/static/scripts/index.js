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
        mode.innerText = "light_mode"
        localStorage.setItem('mode', "dark_mode")
        console.log("Switching to dark");
    }
  }
  mode.innerText = localStorage.getItem('mode');
  modeChange();
  document.getElementById("mode").addEventListener("click", modeChange)


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
  const directPdfLink = document.getElementById('directPdfLink');
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
    // Update direct link if start date is also set
    updateDirectLink();
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
  
  // Function to update direct link
  function updateDirectLink() {
    const startDate = startDateInput.value;
    const endDate = endDateInput.value;
    if (startDate && endDate) {
      const pdfUrl = `/statement/${accountId}/pdf?startDate=${startDate}&endDate=${endDate}`;
      directPdfLink.href = pdfUrl;
      directPdfLink.classList.remove('disabled');
    } else {
      directPdfLink.href = '#';
      directPdfLink.classList.add('disabled');
    }
  }
  
  // Update link when date inputs change
  if (startDateInput) startDateInput.addEventListener('change', updateDirectLink);
  if (endDateInput) endDateInput.addEventListener('change', updateDirectLink);
  
  // Initialize direct link state
  if (directPdfLink) {
    directPdfLink.classList.add('disabled');
    directPdfLink.addEventListener('click', function(e) {
      const startDate = startDateInput.value;
      const endDate = endDateInput.value;
      if (!startDate || !endDate) {
        e.preventDefault();
        alert('Please select both start and end dates');
      }
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

});
