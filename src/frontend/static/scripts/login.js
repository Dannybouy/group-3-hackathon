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
  // Login client-side validation
  var login = document.querySelector("#login-form");
  login.addEventListener("submit", function(e) {
    if(!login.checkValidity()){
      e.preventDefault();
      e.stopPropagation();
    }
    login.classList.add('was-validated');
  });

  var showAlert = (window.location.search == "?msg=Login+Failed");
  if (showAlert){
      document.querySelector("#alertBanner").classList.remove("hidden");
  }

  if (!localStorage.getItem("mode")) {
    localStorage.setItem("mode", "light_mode");
  }

  //Added the toggle Button Logic
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
});