'use strict';

const ARMENIAN_LETTERS = [
  'Ա', 'Բ', 'Գ', 'Դ', 'Ե', 'Զ', 'Է', 'Ը', 'Թ',
  'Ժ', 'Ի', 'Լ', 'Խ', 'Ծ', 'Կ', 'Հ', 'Ձ', 'Ղ',
  'Ճ', 'Մ', 'Յ', 'Ն', 'Շ', 'Ո', 'Չ', 'Պ', 'Ջ',
  'Ռ', 'Ս', 'Վ', 'Տ', 'Ր', 'Ց', 'Ւ', 'Փ', 'Ք'
];

const WEEK_LETTERS = ['Ա', 'Բ', 'Գ', 'Դ', 'Ե', 'Զ', 'Է'];
const DAY_MS = 24 * 60 * 60 * 1000;
let currentResult = null;
let systemThemeWatcher = null;

function getSystemTheme() {
  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark';
  }
  return 'light';
}

function applyThemePreference(theme) {
  const normalizedTheme = ['system', 'light', 'dark'].includes(theme) ? theme : 'system';
  const effectiveTheme = normalizedTheme === 'system' ? getSystemTheme() : normalizedTheme;

  document.documentElement.setAttribute('data-theme', normalizedTheme);
  document.documentElement.setAttribute('data-effective-theme', effectiveTheme);
  document.documentElement.classList.remove('theme-light', 'theme-dark');
  document.documentElement.classList.add('theme-' + effectiveTheme);

  const themeSelect = document.getElementById('themeSelect');
  if (themeSelect) {
    themeSelect.value = normalizedTheme;
  }
}

function watchSystemThemeChanges() {
  if (!window.matchMedia) return;

  systemThemeWatcher = window.matchMedia('(prefers-color-scheme: dark)');
  const refreshTheme = () => {
    if (loadSavedThemePreference() === 'system') {
      applyThemePreference('system');
    }
  };

  if (typeof systemThemeWatcher.addEventListener === 'function') {
    systemThemeWatcher.addEventListener('change', refreshTheme);
  } else if (typeof systemThemeWatcher.addListener === 'function') {
    systemThemeWatcher.addListener(refreshTheme);
  }
}

function loadSavedThemePreference() {
  try {
    return localStorage.getItem('calendarTheme') || 'system';
  } catch (error) {
    return 'system';
  }
}

function saveThemePreference(theme) {
  try {
    if (theme === 'system') {
      localStorage.removeItem('calendarTheme');
    } else {
      localStorage.setItem('calendarTheme', theme);
    }
  } catch (error) {
    // Ignore storage errors so the calculator still works in private browsing modes.
  }
}


function makeDate(year, month, day) {
  return new Date(Date.UTC(year, month - 1, day));
}

function addDays(date, days) {
  return new Date(date.getTime() + days * DAY_MS);
}

function addWeeks(date, weeks) {
  return addDays(date, weeks * 7);
}

function daysBetween(start, end) {
  return Math.round((end.getTime() - start.getTime()) / DAY_MS);
}

function dayOfYear(date) {
  return daysBetween(makeDate(date.getUTCFullYear(), 1, 1), date) + 1;
}

function sameDate(a, b) {
  return a.getUTCFullYear() === b.getUTCFullYear()
    && a.getUTCMonth() === b.getUTCMonth()
    && a.getUTCDate() === b.getUTCDate();
}

function mod(value, divisor) {
  const m = value % divisor;
  return m < 0 ? m + divisor : m;
}

function formatShortDate(date) {
  return new Intl.DateTimeFormat('en-US', {
    timeZone: 'UTC',
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  }).format(date);
}

function formatLongDate(date) {
  return new Intl.DateTimeFormat('en-US', {
    timeZone: 'UTC',
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric'
  }).format(date);
}

function formatDay(date) {
  return new Intl.DateTimeFormat('en-US', {
    timeZone: 'UTC',
    weekday: 'long'
  }).format(date);
}

function isGregorianLeapYear(year) {
  return (year % 4 === 0) && ((year % 100 !== 0) || (year % 400 === 0));
}

function gregorianEaster(year) {
  const a = year % 19;
  const b = Math.floor(year / 100);
  const c = year % 100;
  const d = Math.floor(b / 4);
  const e = b % 4;
  const f = Math.floor((b + 8) / 25);
  const g = Math.floor((b - f + 1) / 3);
  const h = (19 * a + b - d - g + 15) % 30;
  const i = Math.floor(c / 4);
  const k = c % 4;
  const l = (32 + 2 * e + 2 * i - h - k) % 7;
  const m = Math.floor((a + 11 * h + 22 * l) / 451);
  const month = Math.floor((h + l - 7 * m + 114) / 31);
  const day = ((h + l - 7 * m + 114) % 31) + 1;
  return makeDate(year, month, day);
}

function calculateVosgegir(year) {
  return (year % 19) + 1;
}

function calculateVeradir(year) {
  const a = year % 19;
  const b = Math.floor(year / 100);
  const d = Math.floor(b / 4);
  const f = Math.floor((b + 8) / 25);
  const g = Math.floor((b - f + 1) / 3);
  const value = mod(11 * a + 18 + b - d - g, 30);
  return value === 0 ? 30 : value;
}

function calculateKirakagir(year) {
  const jan1 = makeDate(year, 1, 1);
  const javaDay = jan1.getUTCDay() === 0 ? 7 : jan1.getUTCDay();
  let daysUntilSunday = 7 - javaDay;
  if (daysUntilSunday < 0) daysUntilSunday += 7;
  const firstSunday = addDays(jan1, daysUntilSunday);
  const firstIndex = (dayOfYear(firstSunday) - 1) % 7;
  const first = WEEK_LETTERS[firstIndex];

  if (!isGregorianLeapYear(year)) {
    return first;
  }

  const secondIndex = mod(firstIndex - 1, 7);
  return first + WEEK_LETTERS[secondIndex];
}

function calculateTaregir(easter, leapYear) {
  const march22 = makeDate(easter.getUTCFullYear(), 3, 22);
  const index = daysBetween(march22, easter);
  if (index < 0 || index >= 35) {
    throw new Error('Easter date outside expected Gregorian range.');
  }

  const easterLetter = ARMENIAN_LETTERS[index];
  if (!leapYear) {
    return easterLetter;
  }

  const leapLetter = ARMENIAN_LETTERS[index + 1];
  return leapLetter + easterLetter;
}

function sundayNearest(date) {
  let before = new Date(date.getTime());
  while (before.getUTCDay() !== 0) {
    before = addDays(before, -1);
  }

  let after = new Date(date.getTime());
  while (after.getUTCDay() !== 0) {
    after = addDays(after, 1);
  }

  const daysBefore = daysBetween(before, date);
  const daysAfter = daysBetween(date, after);
  return daysBefore < daysAfter ? before : after;
}

function sundayOnOrAfter(date) {
  let d = new Date(date.getTime());
  while (d.getUTCDay() !== 0) {
    d = addDays(d, 1);
  }
  return d;
}

function saturdayOnOrAfter(date) {
  let d = new Date(date.getTime());
  while (d.getUTCDay() !== 6) {
    d = addDays(d, 1);
  }
  return d;
}

function calculateAllSaintsFromTonatsuytsRule(exaltation, hisnagParegentan) {
  const crossSunday8 = addWeeks(exaltation, 7);
  const crossSunday10 = addWeeks(exaltation, 9);
  const crossSunday11 = addWeeks(exaltation, 10);

  if (sameDate(hisnagParegentan, crossSunday10)) {
    return addDays(crossSunday8, -1);
  }

  if (sameDate(hisnagParegentan, crossSunday11)) {
    return addDays(crossSunday11, -1);
  }

  throw new Error('Hisnag Paregentan did not fall on the 10th or 11th Sunday of the Cross.');
}

function calculateFeasts(year, easter) {
  const poonParegentan = addDays(easter, -49);
  const greatLentBegins = addDays(easter, -48);
  const palmSunday = addDays(easter, -7);
  const pentecost = addDays(easter, 49);
  const holyEtchmiadzin = addDays(pentecost, 14);
  const exaltation = sundayNearest(makeDate(year, 9, 14));
  const hisnagParegentan = sundayNearest(makeDate(year, 11, 18));
  const allSaints = calculateAllSaintsFromTonatsuytsRule(exaltation, hisnagParegentan);
  const crossSunday8 = addWeeks(exaltation, 7);
  const thirdSundayOfHisnag = addWeeks(hisnagParegentan, 3);
  const stJamesOfNisibis = addDays(thirdSundayOfHisnag, 6);

  return [
    { feast: 'Nativity and Theophany of Our Lord †', date: makeDate(year, 1, 6) },
    { feast: 'Presentation of the Lord to the Temple', date: makeDate(year, 2, 14) },
    { feast: 'St. Sarkis the Warrior', date: addDays(poonParegentan, -15) },
    { feast: 'St. Ghevont the Priest and His Companions', date: addDays(poonParegentan, -5) },
    { feast: 'St. Vartan the Warrior and His Companions', date: addDays(poonParegentan, -3) },
    { feast: 'Poon Paregentan', date: poonParegentan },
    { feast: 'Great Lent begins', date: greatLentBegins },
    { feast: 'Meejeenk (Median day of Great Lent)', date: addDays(greatLentBegins, 23) },
    { feast: 'Annunciation', date: makeDate(year, 4, 7) },
    { feast: 'Palm Sunday', date: palmSunday },
    { feast: 'Easter - Resurrection of Our Lord †', date: easter },
    { feast: 'Ascension', date: addDays(easter, 39) },
    { feast: 'Pentecost', date: pentecost },
    { feast: 'Emergence of St. Gregory from the Pit', date: addDays(holyEtchmiadzin, -1) },
    { feast: 'Holy Etchmiadzin', date: holyEtchmiadzin },
    { feast: 'Transfiguration of Our Lord †', date: addDays(easter, 98) },
    { feast: 'Assumption of the Holy Mother-of-God †', date: sundayNearest(makeDate(year, 8, 15)) },
    { feast: 'Nativity of the Holy Mother-of-God', date: makeDate(year, 9, 8) },
    { feast: 'Exaltation of the Holy Cross †', date: exaltation },
    { feast: 'Holy Cross of Varak', date: addDays(exaltation, 14) },
    { feast: 'Holy Translators', date: addDays(exaltation, 27) },
    { feast: 'Discovery of the Holy Cross', date: sundayOnOrAfter(makeDate(year, 10, 23)) },
    { feast: 'All Saints Day', date: allSaints },
    { feast: 'Archangels Gabriel and Michael', date: addDays(crossSunday8, 6) },
    { feast: 'Presentation of the Holy Mother-of-God', date: makeDate(year, 11, 21) },
    { feast: 'Conception of the Holy Mother-of-God', date: makeDate(year, 12, 9) },
    { feast: 'St. James of Nisibis', date: stJamesOfNisibis }
  ];
}

function calculateCalendar(year) {
  const easter = gregorianEaster(year);
  const isLeapYear = isGregorianLeapYear(year);

  return {
    year,
    easter,
    isLeapYear,
    vosgegir: calculateVosgegir(year),
    veradir: calculateVeradir(year),
    kirakagir: calculateKirakagir(year),
    taregir: calculateTaregir(easter, isLeapYear),
    feasts: calculateFeasts(year, easter)
  };
}

function calculateAndDisplay(options = {}) {
  const silentInvalid = options.silentInvalid === true;
  const yearInput = document.getElementById('year');
  const rawYear = yearInput.value.trim();
  const year = Number.parseInt(rawYear, 10);

  if (!Number.isInteger(year) || String(year) !== rawYear || year < 1583 || year > 4099) {
    if (!silentInvalid) {
      alert('Please enter a Gregorian year from 1583 to 4099.');
    }
    return false;
  }

  try {
    currentResult = calculateCalendar(year);
  } catch (error) {
    if (!silentInvalid) {
      alert(error.message);
    }
    return false;
  }

  document.getElementById('subtitle').textContent =
    `New/Gregorian Armenian Church calendar · Year ${year}`;
  document.getElementById('taregirValue').textContent = currentResult.taregir;
  document.getElementById('kirakagirValue').textContent = currentResult.kirakagir;
  document.getElementById('veradirValue').textContent = String(currentResult.veradir);
  document.getElementById('vosgegirValue').textContent = String(currentResult.vosgegir);
  document.getElementById('easterValue').textContent = formatShortDate(currentResult.easter);
  document.getElementById('leapYearValue').textContent = currentResult.isLeapYear ? 'Yes' : 'No';

  renderFeastTable(currentResult);
  return true;
}

function renderFeastTable(result) {
  const tbody = document.getElementById('feastTableBody');
  tbody.innerHTML = '';

  result.feasts.forEach(({ feast, date }) => {
    const tr = document.createElement('tr');
    if (feast.includes('†')) {
      tr.classList.add('dominical');
    }

    const feastTd = document.createElement('td');
    feastTd.textContent = feast;

    const dateTd = document.createElement('td');
    dateTd.textContent = formatShortDate(date);

    const dayTd = document.createElement('td');
    dayTd.textContent = formatDay(date);

    const infoTd = document.createElement('td');
    infoTd.className = 'info-cell';

    const button = document.createElement('button');
    button.className = 'info-button';
    button.type = 'button';
    button.textContent = 'ⓘ';
    button.title = 'Show calculation note';
    button.setAttribute('aria-label', `${feast} calculation note`);
    button.addEventListener('click', () => showInfoDialog(feast, buildFeastNote(feast, currentResult)));

    infoTd.appendChild(button);
    tr.append(feastTd, dateTd, dayTd, infoTd);
    tbody.appendChild(tr);
  });
}

function buildAnnualValueNote(key) {
  if (currentResult == null) {
    return 'Calculate a year first.';
  }
  const year = currentResult.year;
  if (key === 'Տարեգիր / Գիր տարւոյ') {
    const march22 = makeDate(year, 3, 22);
    const number = daysBetween(march22, currentResult.easter) + 1;
    return 'Տարեգիր / Գիր տարւոյ is found from Gregorian Easter. Count inclusively from March 22: March 22 = Ա, March 23 = Բ, and so on.\n\n'
      + `For ${year}, Easter is ${formatLongDate(currentResult.easter)}. That is position ${number} in the March 22-April 25 range.\n\n`
      + `The resulting Easter-letter is ${ARMENIAN_LETTERS[number - 1]}. `
      + (currentResult.isLeapYear
        ? `Because this is a leap year, the printed Տարեգիր has two letters: the next letter first, followed by the Easter-letter. Result: ${currentResult.taregir}.`
        : `Because this is not a leap year, the result is a single letter: ${currentResult.taregir}.`);
  }
  if (key === 'Կիրակագիր / Եօթներեակ') {
    return 'Կիրակագիր / Եօթներեակ is the Sunday-letter. Assign January 1 = Ա, January 2 = Բ, January 3 = Գ, through January 7 = Է, then repeat. The letter that falls on Sunday is the Կիրակագիր.\n\n'
      + 'For leap years there are two letters: one before February 29 and one after February 29, because the leap day shifts the weekly letter alignment.\n\n'
      + `For ${year}, the result is ${currentResult.kirakagir}.`;
  }
  if (key === 'Վերադիր') {
    return 'Վերադիր is a Gregorian epact-type lunar correction used with the 19-year cycle in the Paschal calculation.\n\n'
      + 'Formula used here:\n'
      + 'a = year mod 19\n'
      + 'b = floor(year / 100)\n'
      + 'd = floor(b / 4)\n'
      + 'f = floor((b + 8) / 25)\n'
      + 'g = floor((b - f + 1) / 3)\n'
      + 'Վերադիր = (11a + 18 + b - d - g) mod 30; if the result is 0, write 30.\n\n'
      + `For ${year}, the result is ${currentResult.veradir}.`;
  }
  if (key === 'Ոսկեգիր / Իննեւտասներեակ') {
    return 'Ոսկեգիր / Իննեւտասներեակ is the year\'s place in the 19-year lunar cycle.\n\n'
      + 'Formula: Ոսկեգիր = (Gregorian year mod 19) + 1. If the remainder gives 0 in older notation, the cycle value is 19.\n\n'
      + `For ${year}, the result is ${currentResult.vosgegir}.`;
  }
  if (key === 'Gregorian Easter') {
    return 'Gregorian Easter is calculated by the standard Gregorian computus, here using the Meeus/Jones/Butcher algorithm. This Easter date is then used to construct the Paschal cycle and to find the Տարեգիր.\n\n'
      + `For ${year}, Easter is ${formatLongDate(currentResult.easter)}.`;
  }
  if (key === 'Leap Year') {
    return 'Gregorian leap year rule: a year is a leap year if it is divisible by 4, except century years must also be divisible by 400.\n\n'
      + 'Leap years affect the two-letter forms of Տարեգիր and Կիրակագիր.\n\n'
      + `${year} is ${currentResult.isLeapYear ? 'a leap year.' : 'not a leap year.'}`;
  }
  return 'No note available.';
}

function buildFeastNote(feast, r) {
  const easter = r.easter;
  const poon = addDays(easter, -49);
  const greatLent = addDays(easter, -48);
  const pentecost = addDays(easter, 49);
  const holyEtchmiadzin = addDays(pentecost, 14);
  const exaltation = sundayNearest(makeDate(r.year, 9, 14));
  const hisnagParegentan = sundayNearest(makeDate(r.year, 11, 18));
  const crossSunday8 = addWeeks(exaltation, 7);
  const crossSunday10 = addWeeks(exaltation, 9);
  const crossSunday11 = addWeeks(exaltation, 10);

  if (feast.startsWith('Nativity and Theophany')) {
    return `Nativity and Theophany of Our Lord is fixed on January 6 in the Armenian Church calendar.\n\nFor ${r.year}: January 6 = ${formatLongDate(makeDate(r.year, 1, 6))}.`;
  }
  if (feast.startsWith('Presentation of the Lord')) {
    return `Presentation of the Lord to the Temple is fixed on February 14, forty days after January 6.\n\nFor ${r.year}: February 14 = ${formatLongDate(makeDate(r.year, 2, 14))}.`;
  }
  if (feast.startsWith('St. Sarkis')) {
    return `St. Sarkis is placed on the Saturday before the Fast of the Catechumens, two weeks before Poon Paregentan. In this program it is calculated as Poon Paregentan minus 15 days.\n\nPoon Paregentan: ${formatLongDate(poon)}\nSt. Sarkis: ${formatLongDate(addDays(poon, -15))}.`;
  }
  if (feast.startsWith('St. Ghevont')) {
    return `St. Ghevont the Priest and His Companions is calculated here as the Tuesday before Poon Paregentan: Poon Paregentan minus 5 days.\n\nPoon Paregentan: ${formatLongDate(poon)}\nSt. Ghevont: ${formatLongDate(addDays(poon, -5))}.`;
  }
  if (feast.startsWith('St. Vartan')) {
    return `St. Vartan the Warrior and His Companions is calculated here as the Thursday before Poon Paregentan: Poon Paregentan minus 3 days.\n\nPoon Paregentan: ${formatLongDate(poon)}\nSt. Vartan: ${formatLongDate(addDays(poon, -3))}.`;
  }
  if (feast === 'Poon Paregentan') {
    return `Poon Paregentan is the Sunday immediately before the beginning of Great Lent. It is 7 Sundays before Easter, or Easter minus 49 days.\n\nEaster: ${formatLongDate(easter)}\nPoon Paregentan: ${formatLongDate(poon)}.`;
  }
  if (feast === 'Great Lent begins') {
    return `Great Lent begins on the Monday after Poon Paregentan, calculated as Easter minus 48 days.\n\nEaster: ${formatLongDate(easter)}\nGreat Lent begins: ${formatLongDate(greatLent)}.`;
  }
  if (feast.startsWith('Meejeenk')) {
    return `Meejeenk / Միջինք is the Wednesday of the fourth week of Great Lent. In this program it is calculated as Great Lent begins plus 23 days, which places it on Wednesday.\n\nGreat Lent begins: ${formatLongDate(greatLent)}\nMeejeenk: ${formatLongDate(addDays(greatLent, 23))}.`;
  }
  if (feast === 'Annunciation') {
    return `Annunciation is fixed on April 7.\n\nFor ${r.year}: April 7 = ${formatLongDate(makeDate(r.year, 4, 7))}.`;
  }
  if (feast === 'Palm Sunday') {
    return `Palm Sunday is the Sunday before Easter, calculated as Easter minus 7 days.\n\nEaster: ${formatLongDate(easter)}\nPalm Sunday: ${formatLongDate(addDays(easter, -7))}.`;
  }
  if (feast.startsWith('Easter')) {
    return `Easter / Resurrection of Our Lord is calculated by the Gregorian computus for the New/Gregorian Armenian Church calendar. The program uses the Meeus/Jones/Butcher algorithm.\n\nFor ${r.year}, Easter is ${formatLongDate(easter)}.`;
  }
  if (feast === 'Ascension') {
    return `Ascension is the 40th day of Easter, counting Easter Sunday as day 1. In date arithmetic, it is Easter plus 39 days.\n\nEaster: ${formatLongDate(easter)}\nAscension: ${formatLongDate(addDays(easter, 39))}.`;
  }
  if (feast === 'Pentecost') {
    return `Pentecost is the 50th day of Easter, counting Easter Sunday as day 1. In date arithmetic, it is Easter plus 49 days.\n\nEaster: ${formatLongDate(easter)}\nPentecost: ${formatLongDate(pentecost)}.`;
  }
  if (feast.startsWith('Emergence of St. Gregory')) {
    return `The Emergence of St. Gregory from the Pit is the Saturday immediately before the Feast of Holy Etchmiadzin.\n\nHoly Etchmiadzin: ${formatLongDate(holyEtchmiadzin)}\nEmergence: ${formatLongDate(addDays(holyEtchmiadzin, -1))}.`;
  }
  if (feast === 'Holy Etchmiadzin') {
    return `Holy Etchmiadzin is calculated as the second Sunday after Pentecost, or Pentecost plus 14 days.\n\nPentecost: ${formatLongDate(pentecost)}\nHoly Etchmiadzin: ${formatLongDate(holyEtchmiadzin)}.`;
  }
  if (feast.startsWith('Transfiguration')) {
    return `Transfiguration / Vardavar is calculated here as Easter plus 98 days, the 14th Sunday after Easter.\n\nEaster: ${formatLongDate(easter)}\nTransfiguration: ${formatLongDate(addDays(easter, 98))}.`;
  }
  if (feast.startsWith('Assumption')) {
    return `Assumption of the Holy Mother-of-God is kept on the Sunday nearest August 15.\n\nFor ${r.year}, the Sunday nearest August 15 is ${formatLongDate(sundayNearest(makeDate(r.year, 8, 15)))}.`;
  }
  if (feast.startsWith('Nativity of the Holy Mother')) {
    return `Nativity of the Holy Mother-of-God is fixed on September 8.\n\nFor ${r.year}: September 8 = ${formatLongDate(makeDate(r.year, 9, 8))}.`;
  }
  if (feast.startsWith('Exaltation')) {
    return `Exaltation of the Holy Cross is the Sunday nearest September 14. This Sunday is counted as the 1st Sunday of the Holy Cross season.\n\nFor ${r.year}, the Sunday nearest September 14 is ${formatLongDate(exaltation)}.`;
  }
  if (feast === 'Holy Cross of Varak') {
    return `Holy Cross of Varak is calculated as the second Sunday after Exaltation of the Holy Cross, or Exaltation plus 14 days.\n\nExaltation: ${formatLongDate(exaltation)}\nHoly Cross of Varak: ${formatLongDate(addDays(exaltation, 14))}.`;
  }
  if (feast === 'Holy Translators') {
    return `Holy Translators is calculated here as the Saturday before the 5th Sunday of the Holy Cross season, or Exaltation plus 27 days.\n\nExaltation: ${formatLongDate(exaltation)}\nHoly Translators: ${formatLongDate(addDays(exaltation, 27))}.`;
  }
  if (feast === 'Discovery of the Holy Cross') {
    return `Discovery of the Holy Cross is calculated here as the Sunday on or after October 23.\n\nFor ${r.year}: ${formatLongDate(sundayOnOrAfter(makeDate(r.year, 10, 23)))}.`;
  }
  if (feast === 'All Saints Day') {
    let caseText;
    if (sameDate(hisnagParegentan, crossSunday10)) {
      caseText = 'Hisnag Paregentan falls on the 10th Sunday of the Holy Cross, so this is the shorter Cross season. All Saints is kept on the Saturday before the 8th Sunday of the Holy Cross.';
    } else if (sameDate(hisnagParegentan, crossSunday11)) {
      caseText = 'Hisnag Paregentan falls on the 11th Sunday of the Holy Cross, so this is the longer Cross season with an added week. All Saints is moved to the Saturday before the 11th Sunday / Hisnag Paregentan.';
    } else {
      caseText = 'Hisnag Paregentan did not match the expected 10th or 11th Sunday of the Holy Cross; check the year calculation.';
    }
    return 'All Saints follows the two-case rule given in the Tonatsuyts notes, not a single fixed distance from Hisnag Paregentan.\n\n'
      + `Exaltation / 1st Sunday of the Cross: ${formatLongDate(exaltation)}\n`
      + `8th Sunday of the Cross: ${formatLongDate(crossSunday8)}\n`
      + `10th Sunday of the Cross: ${formatLongDate(crossSunday10)}\n`
      + `11th Sunday of the Cross: ${formatLongDate(crossSunday11)}\n`
      + `Hisnag Paregentan: ${formatLongDate(hisnagParegentan)}\n\n`
      + `${caseText}\n\n`
      + `All Saints: ${formatLongDate(calculateAllSaintsFromTonatsuytsRule(exaltation, hisnagParegentan))}.`;
  }
  if (feast === 'Archangels Gabriel and Michael') {
    return 'Archangels Gabriel and Michael is always the Saturday after the 8th Sunday of the Holy Cross.\n\n'
      + `Exaltation / 1st Sunday of the Cross: ${formatLongDate(exaltation)}\n`
      + `8th Sunday of the Cross: ${formatLongDate(crossSunday8)}\n`
      + `Saturday after the 8th Sunday: ${formatLongDate(addDays(crossSunday8, 6))}.`;
  }
  if (feast === 'Presentation of the Holy Mother-of-God') {
    return `Presentation of the Holy Mother-of-God is fixed on November 21.\n\nFor ${r.year}: November 21 = ${formatLongDate(makeDate(r.year, 11, 21))}.`;
  }
  if (feast === 'Conception of the Holy Mother-of-God') {
    return `Conception of the Holy Mother-of-God is fixed on December 9.\n\nFor ${r.year}: December 9 = ${formatLongDate(makeDate(r.year, 12, 9))}.`;
  }
  if (feast === 'St. James of Nisibis') {
    const hisnagParegentan = sundayNearest(makeDate(r.year, 11, 18));
    const thirdSundayOfHisnag = addWeeks(hisnagParegentan, 3);
    const stJames = addDays(thirdSundayOfHisnag, 6);
    return `St. James of Nisibis is commemorated on the Saturday following the Third Sunday of Hisnag.\n\nHisnag Paregentan: ${formatLongDate(hisnagParegentan)}\nThird Sunday of Hisnag: ${formatLongDate(thirdSundayOfHisnag)}\nSt. James of Nisibis: ${formatLongDate(stJames)}.`;
  }
  return 'No calculation note is available for this item.';
}

function showInfoDialog(title, message) {
  const dialog = document.getElementById('infoDialog');
  document.getElementById('dialogTitle').textContent = title;
  document.getElementById('dialogMessage').textContent = message;

  if (typeof dialog.showModal === 'function') {
    dialog.showModal();
  } else {
    alert(`${title}\n\n${message}`);
  }
}

function setDefaultYearToCurrentYear() {
  const yearInput = document.getElementById('year');
  if (!yearInput) {
    return;
  }

  const currentYear = new Date().getFullYear();
  const minYear = Number.parseInt(yearInput.dataset.min || yearInput.min || '1583', 10);
  const maxYear = Number.parseInt(yearInput.dataset.max || yearInput.max || '4099', 10);

  if (currentYear >= minYear && currentYear <= maxYear) {
    yearInput.value = String(currentYear);
  }
}


function getYearBounds(yearInput) {
  return {
    min: Number.parseInt(yearInput.dataset.min || yearInput.min || '1583', 10),
    max: Number.parseInt(yearInput.dataset.max || yearInput.max || '4099', 10)
  };
}

function stepYear(yearInput, amount) {
  const bounds = getYearBounds(yearInput);
  const current = Number.parseInt(yearInput.value, 10);
  const fallback = new Date().getFullYear();
  const base = Number.isInteger(current) ? current : fallback;
  const next = Math.min(bounds.max, Math.max(bounds.min, base + amount));
  yearInput.value = String(next);
  calculateAndDisplay({ silentInvalid: true });
}

function wireYearStepper(yearInput) {
  document.querySelectorAll('[data-year-step]').forEach(button => {
    const amount = Number.parseInt(button.dataset.yearStep, 10) || 0;
    let repeatDelay = null;
    let repeatInterval = null;

    const stopRepeating = () => {
      window.clearTimeout(repeatDelay);
      window.clearInterval(repeatInterval);
      repeatDelay = null;
      repeatInterval = null;
    };

    const startRepeating = event => {
      event.preventDefault();
      yearInput.focus();
      stepYear(yearInput, amount);
      stopRepeating();
      repeatDelay = window.setTimeout(() => {
        repeatInterval = window.setInterval(() => stepYear(yearInput, amount), 120);
      }, 450);
    };

    button.addEventListener('pointerdown', startRepeating);
    button.addEventListener('pointerup', stopRepeating);
    button.addEventListener('pointercancel', stopRepeating);
    button.addEventListener('pointerleave', stopRepeating);
    button.addEventListener('click', event => event.preventDefault());
  });
}

function wireUpUi() {
  applyThemePreference(loadSavedThemePreference());
  watchSystemThemeChanges();
  setDefaultYearToCurrentYear();

  document.getElementById('themeSelect').addEventListener('change', event => {
    const selectedTheme = event.target.value;
    saveThemePreference(selectedTheme);
    applyThemePreference(selectedTheme);
  });

  const yearInput = document.getElementById('year');
  let autoUpdateTimer = null;
  const autoUpdateCalendar = () => {
    window.clearTimeout(autoUpdateTimer);
    autoUpdateTimer = window.setTimeout(() => {
      calculateAndDisplay({ silentInvalid: true });
    }, 150);
  };

  yearInput.addEventListener('input', () => {
    yearInput.value = yearInput.value.replace(/[^0-9]/g, '');
    autoUpdateCalendar();
  });
  yearInput.addEventListener('change', () => calculateAndDisplay({ silentInvalid: true }));
  yearInput.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
      calculateAndDisplay();
    }
  });

  wireYearStepper(yearInput);
  yearInput.addEventListener('wheel', event => {
    if (document.activeElement !== yearInput) {
      return;
    }
    event.preventDefault();
    stepYear(yearInput, event.deltaY < 0 ? 1 : -1);
  }, { passive: false });
  document.getElementById('dialogClose').addEventListener('click', () => {
    document.getElementById('infoDialog').close();
  });
  document.getElementById('infoDialog').addEventListener('click', event => {
    if (event.target.id === 'infoDialog') {
      event.target.close();
    }
  });

  document.querySelectorAll('[data-info]').forEach(button => {
    button.addEventListener('click', () => {
      const key = button.dataset.info;
      showInfoDialog(key, buildAnnualValueNote(key));
    });
  });

  calculateAndDisplay();
}

document.addEventListener('DOMContentLoaded', wireUpUi);
