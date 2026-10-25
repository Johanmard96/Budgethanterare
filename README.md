Budgethanteraren

Interaktiv konsolapplikation i Java där man registrerar inkomster och utgifter, kategoriserar dem,
sparar/läser dem från en CSV-fil och får en sammanställning av sin ekonomi.
Fokus i uppgiften är testning och felsökning (läranderesultat 7).

Bygga och köra

Kräver JDK 25 eller senare (inställt via maven.compiler.release i pom.xml) och Maven.


mvn test kör alla enhetstester
mvn package bygger projektet


Kör programmet genom att starta Main i IntelliJ (valfritt programargument: sökväg till datafilen,
standard är transaktioner.csv i arbetskatalogen). Loggar skrivs till budgethanteraren.log.

Struktur (paket com.example.java26.budget)

 Klass  Ansvar 

 TransactionType  enum INKOMST / UTGIFT 
 Transaction  record (datum, kategori, belopp, typ). Validerar i konstruktorn, så en ogiltig transaktion kan inte skapas 
 Repository<T>  egen generisk klass: add, addAll, findAll, findWhere(Predicate), remove, clear 
 TransactionPredicates  färdiga filter (typ, kategori, datumintervall) som kan kombineras med and() 
 BudgetCalculator  saldo, summor och summa per kategori (Stream API: filter, groupingBy, summingDouble) 
 TransactionCsvStorage  läser/skriver CSV med Files.newBufferedReader/Writer i try-with-resources 
 InvalidTransactionException unchecked – ogiltigt belopp/kategori/datum 
 FileFormatException  checked – trasig rad i filen (med radnummer) 
 ConsoleUi, Main  menyn respektive start/inläsning 
 LoggingConfig konfigurerar loggningen 

 Designbeslut

Belopp lagras alltid positivt, typen avgör tecknet (signedAmount()). Negativa belopp och noll nekas
  med ett meddelande som förklarar hur utgifter anges. Belopp avrundas till två decimaler och är högst
  1 000 000 000.
Flyttal: summor avrundas till två decimaler så att t.ex. 10 × 0.10 blir exakt 1.00.
Filformat: datum;kategori;belopp;typ (UTF-8, punkt som decimaltecken). Vid inmatning accepteras även decimalkomma.
**Felhantering vid filläsning: saknas filen skapas en ny. Trasiga rader hoppas över och loggas, och
  originalfilen kopieras till <fil>.bak så att inget data försvinner när programmet sparar. Riktiga I/O-fel
  (t.ex. ogiltig teckenkodning) fångas i Main, loggas som SEVERE och programmet startar med en tom lista.
Säker sparning: skriver först till <fil>.tmp och byter sedan ut originalet.

Loggning

java.util.logging med fyra nivåer, skrivna till budgethanteraren.log (konsolen lämnas ren för menyn):

 Nivå | Används för 

 FINE (debug) | menyval, varje tillägg/borttagning i repositoryt, beräkningar 
INFO programstart/avslut, tillagda transaktioner, inlästa/sparade filer 
WARNING ogiltig inmatning, ogiltigt menyval, trasiga rader i filen 
SEVERE (error)  I/O-fel vid läsning/sparning, oväntade fel 

 Tester

JUnit 5, en testklass per komponent, alla med Arrange-Act-Assert:

- RepositoryTest – normalfall, tomt repository, null, defensiv kopia, findWhere, samma klass med Repository<String> och Repository<Transaction>
- BudgetCalculatorTest – tom lista, negativt saldo, summa per kategori (skiftlägesokänslig), flyttal, minsta/största belopp, null
- TransactionTest – validering, gränsvärden (0.01, MAX_AMOUNT, strax över), parametriserade ogiltiga belopp/kategorier, sortering
- TransactionPredicatesTest – inklusive datumgränser, kombinerade filter, assertThrows för felaktigt intervall
- TransactionCsvStorageTest – round-trip, saknad/tom fil, trasiga rader + .bak, BOM, ogiltig teckenkodning (@TempDir)

 Dokumenterad felsökning




Generics. Repository<T> vet inget om transaktioner, så samma klass kunde testas med Repository<String>
och användas med Repository<Transaction>. Utan generics hade jag antingen skrivit en klass per typ eller
använt Object och typkonverteringar, med risk för ClassCastException först vid körning. Nu fångar
kompilatorn felet direkt. Metoden findWhere(Predicate<? super T>) ersätter en hel rad specialmetoder
(findByType, findByCategory, …) – filtret skickas in som en lambda.

Stream API. Summa per kategori blev ett enda uttryck med groupingBy(..., summingDouble(...)). Utan
Stream hade det krävts en loop, en Map, en containsKey/put-kontroll per rad och en separat sortering.
Strömmen beskriver vad som ska beräknas i stället för hur, vilket gjorde koden kortare och lättare att
testa. Filter från TransactionPredicates kan kombineras med and() utan ny kod.

Nackdelar/lärdomar. långa stream-kedjor kan vara svåra att felsöka med debuggern jämfört med en vanlig loop).