package com.example;

import com.example.api.ElpriserAPI;

import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


//Att göra idag
//todo: Skapa en metod för att snygga till min switch
//todo: skapa en metod för sliding window till -charging 2h,4h,8h

//todo: datumformatering


public class Main {
    static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH");
    static NumberFormat numberFormat = NumberFormat.getNumberInstance();
    public static void main(String[] args) {

        ElpriserAPI elpriserApi = new ElpriserAPI();
        Locale.setDefault(Locale.of("sv", "SE"));
        System.out.println("Välkommen till Elpris-kollen!");
        String zoneOf = null;
        String dateOf = null;
        String chargingInput = null;
        boolean isHelped = false;
        boolean isSorted = false;

        List<String> validZones = new ArrayList<>();
        validZones.add("SE1");
        validZones.add("SE2");
        validZones.add("SE3");
        validZones.add("SE4");


        //Loopar igenom args och letar efter input från terminalen, case styr vad som händer om ex --zone skrivs in
        //if ger nya värden till zoneOf, dateOf, chargeOf och isHelped och skickar tillbaka till main-metoden
        if (args.length == 0) { helpMe();
        }

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone" -> { if (i + 1 < args.length) {
                        zoneOf = args[++i];
                    } else { helpMe(); return;

                    }
                }
                case "--date" -> { if (i + 1 < args.length) {
                        dateOf = args[++i];

                    } else { helpMe(); return;
                    }
                }
                case "--charging" -> { if (i + 1 < args.length) {
                        chargingInput = args[++i];
                        } else { helpMe(); return;
                        }
                    }

                case "--sorted" -> {
                    isSorted = true;
                }

                case "--help" -> {
                    isHelped = true;
                    helpMe(); return;
                }
            }

        }
        LocalDate dagensDatum;


        if (dateOf != null)
            try {
                dagensDatum = LocalDate.parse(dateOf);
            } catch (DateTimeException e) {
                System.out.println("Ogiltigt datum: " + dateOf);
                helpMe();
                return;
            }
        else {
            dagensDatum = LocalDate.now();
        }
        LocalDate tomorrow = dagensDatum.plusDays(1);
        ElpriserAPI.Prisklass zon;


        if (zoneOf == null || !validZones.contains(zoneOf.toUpperCase())) {
            System.out.println("Ogiltig zon: " + zoneOf);
            helpMe();
            return;
        }
        zon = ElpriserAPI.Prisklass.valueOf(zoneOf.toUpperCase());
        List<ElpriserAPI.Elpris> priserIdag = elpriserApi.getPriser(dagensDatum, zon);
        if(priserIdag == null) {
            System.out.println("Kunde inte hitta några priser");
            return;
        }
        List<ElpriserAPI.Elpris> priserImorgon = elpriserApi.getPriser(tomorrow, zon);
        if(priserImorgon == null){
            System.out.println("Kunde inte hitta några priser för imorgon");
            return;
        }

        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);

        List<ElpriserAPI.Elpris> kombineradeListor = combinedLists(priserIdag,priserImorgon);


            int timmar =0;
        if (chargingInput != null) {
            try {
                //när användaren skriver 4h h ersätts med ""
                 timmar = Integer.parseInt(chargingInput.replace("h", ""));

            } catch (NumberFormatException e) {
                helpMe(); return;
            }
        }

        if (timmar > 0) {
            chargingWindow(kombineradeListor, timmar);
            return;
        }
        if (isSorted) {
            List<ElpriserAPI.Elpris> sorteradePriser = isSortedCombined(priserIdag, priserImorgon);
            printPriser(sorteradePriser);
            return;
        } //else printhelp
        skrivUtPriser(priserIdag, priserImorgon);
        priceMinMax(priserIdag);
        medelPris(priserIdag);
        listWith96Prices(priserIdag);


    }

    public static List<ElpriserAPI.Elpris> combinedLists(List<ElpriserAPI.Elpris> priserIdag, List<ElpriserAPI.Elpris> priserImorgon) {
        List<ElpriserAPI.Elpris> kombineradeLaddpriser = new ArrayList<>();
        if (priserIdag != null) kombineradeLaddpriser.addAll(priserIdag);
        if (priserImorgon != null) kombineradeLaddpriser.addAll(priserImorgon);


        return kombineradeLaddpriser;
    }

    public static void chargingWindow (List<ElpriserAPI.Elpris> elpriserLadda, int timmar) {
        //todo: 2,4,8 h, testa i sub-arrays(Sliding windows)
        DateTimeFormatter minuteFormatter = DateTimeFormatter.ofPattern("HH:mm");
        NumberFormat newFormat = NumberFormat.getNumberInstance(Locale.of("sv", "SE"));
        newFormat.setMaximumFractionDigits(2);
        newFormat.setMinimumFractionDigits(1);

        if (elpriserLadda == null ||timmar<= 0|| elpriserLadda.isEmpty()|| timmar > elpriserLadda.size()) {
            System.out.println("Inga eller för få timmar för att beräkna laddningsfönster.");
            return;
        }

        double minSumma = Double.MAX_VALUE;
        int startIndex = -1;

        for (int i = 0; i <= elpriserLadda.size() - timmar; i++) {
            double summa = 0.0;
            for (int j = 0; j < timmar; j++) {
                summa += elpriserLadda.get(i+j).sekPerKWh();
            }
            if (summa< minSumma) {
                minSumma = summa;
                startIndex = i;
            }

        }
        if (startIndex != -1) {
            ElpriserAPI.Elpris start = elpriserLadda.get(startIndex);
            ElpriserAPI.Elpris slut = elpriserLadda.get(startIndex + timmar -1);

            String startTid = start.timeStart().format(minuteFormatter);
            String slutTid = slut.timeEnd().format(minuteFormatter);
            double snittPris = (minSumma/timmar) * 100;
            String formateratPris = newFormat.format(snittPris);

            System.out.printf("Billigaste laddningsfönster för %dh är kl %s-%s\nMedelpris för fönster: %s öre\n Påbörja laddning %s", timmar, startTid, slutTid, formateratPris, startTid);
        }
    }
    // Expected Min: Hour 0 -> avg(0.10, 0.11, 0.12, 0.13) = 0.115 SEK/kWh = 11,50 öre
    // Expected Max: Hour 23 -> avg(2.40, 2.41, 2.42, 2.43) = 2.415 SEK/kWh = 241,50 öre
    public static void listWith96Prices (List<ElpriserAPI.Elpris> elpriser96) {
         //behöver 24 grupper där varje grupp visar 4 priser som medelpris
        //timme = 0, prisindex 1-4, (P1, P2, P3, P4)

        //i är 0; sålänge i är mindre än storleken på listan; öka i med 4
            for (int i = 0; i < elpriser96.size(); i += 4) {
                //I varje loop, stoppa de fyra värderna i en sublist
                List<ElpriserAPI.Elpris> listaPerTimme = elpriser96.subList(i, i + 4);

                double sum = listaPerTimme.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).sum();
                double medelPris = sum/4.0;

                int timme = i/4;
                int timme2 = timme +1;

                String timDel = String.format("%02d-%02d", timme, timme2);
                //String priserIparantes = listaPerTimme.stream().map(elpris -> numberFormat.format(elpris.sekPerKWh()*100)).collect(Collectors.joining(","));
                double medelprisToOre = medelPris * 100;
                String formateratMedelPris = numberFormat.format(medelprisToOre);

                System.out.printf("%s Medelpris: %s öre\n", timDel, formateratMedelPris);


            }
        }



    static void printPriser (List<ElpriserAPI.Elpris> priser) {
        priser.stream().forEach(elpriser -> System.out.printf("""
                %s-%s %.2f öre\n""",elpriser.timeStart().format(timeFormatter), elpriser.timeEnd().format(timeFormatter), elpriser.sekPerKWh()*100));
    }

    public static List<ElpriserAPI.Elpris> isSortedCombined (List<ElpriserAPI.Elpris> priserIdag, List<ElpriserAPI.Elpris> priserImorgon) {
            List<ElpriserAPI.Elpris> sammansattaPriser = new ArrayList<>();
            sammansattaPriser.addAll(priserIdag);
            sammansattaPriser.addAll(priserImorgon);
            sammansattaPriser.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh).reversed().thenComparing(ElpriserAPI.Elpris::timeStart));


        return sammansattaPriser;
    }

    public static void skrivUtPriser(List<ElpriserAPI.Elpris> elprisIdag,List<ElpriserAPI.Elpris> elprisImorgon) {

//todo: Hämta elpriserna i en ArrayList för att skriva ut två dagars priser om morgondagens priser finns //List av listElpriserAPI

        List<ElpriserAPI.Elpris> sammansattaPriser = new ArrayList<>();
        if (elprisIdag != null) {
            sammansattaPriser.addAll(elprisIdag);
            System.out.println("Dagens Priser:");
            for (ElpriserAPI.Elpris elpriser : elprisIdag) {

                String pris = numberFormat.format(elpriser.sekPerKWh()*100);
                System.out.printf("%s-%s %s öre\n", elpriser.timeStart().format(timeFormatter), elpriser.timeEnd().format(timeFormatter), pris);
            }
        }
        if (elprisImorgon != null) {
            sammansattaPriser.addAll(elprisImorgon);
            System.out.println("Morgondagens priser: ");
            for (ElpriserAPI.Elpris elpriser : elprisImorgon) {

                String pris = numberFormat.format(elpriser.sekPerKWh() * 100);
                System.out.printf("%s-%s %s öre\n", elpriser.timeStart().format(timeFormatter), elpriser.timeEnd().format(timeFormatter), pris);
            }
        }
    }

    private static void medelPris(List<ElpriserAPI.Elpris> prisLista) {
        double summa = 0.0;
        if (prisLista == null) {
            System.out.println("Ingen data tillgänglig för medelpris");
        }
        for (ElpriserAPI.Elpris elpriser : prisLista) {
            summa  += elpriser.sekPerKWh();
        }
        double medelPrisOfDay = summa/ prisLista.size();
        System.out.printf("Medelpris: %s öre \n",  numberFormat.format(medelPrisOfDay*100));
    }

    public static void priceMinMax (List<ElpriserAPI.Elpris> prisLista) {
        //todo: bryt ut max till en egen metod
        double minPris = Double.MAX_VALUE;
        double maxPris = Double.MIN_VALUE;
        String minTid = null;
        String minTidSlut = null;
        String maxTid = null;
        String maxTidSlut = null;
        if (prisLista.isEmpty()) {
            System.out.println("Ingen data finns att visa");
            return;
        }

        for (ElpriserAPI.Elpris elpriser : prisLista) {
            double worthOf = elpriser.sekPerKWh();

            if (minPris > worthOf ) {
                minPris = worthOf;
                minTid = elpriser.timeStart().format(timeFormatter);
                minTidSlut = elpriser.timeEnd().format(timeFormatter);
            }
            if (maxPris < worthOf) {
                maxPris = worthOf;
                maxTid = elpriser.timeStart().format(timeFormatter);
                maxTidSlut = elpriser.timeEnd().format(timeFormatter);
            }
        }
        System.out.printf("Lägsta pris för dagen: %s öre/KWh Kl: %s - %s \n", numberFormat.format(minPris*100), minTid, minTidSlut);
        System.out.printf("Högsta pris för dagen: %s öre/KWh Kl: %s - %s \n", numberFormat.format(maxPris*100), maxTid, maxTidSlut);

    }

    // metod för --help att skickas till if-sats
    public static void helpMe() {
        System.out.println("""
    Hjälpcenter
    ______________________

    Usage / Användning:
      java -cp target/classes com.example.Main [alternativ]

    Alternativ:
      --zone SE1|SE2|SE3|SE4      (obligatoriskt) Välj elpris-zon
      --date YYYY-MM-DD           (valfritt)      Välj datum, t.ex. 2025-09-21
      --charging 2h|4h|8h         (valfritt)      Hitta billigaste laddningsfönster
      --sorted                    (valfritt)      Sortera priser fallande
      --help                      (valfritt)      Visa denna hjälptext

    Exempel:
      java Main --zone SE3 -- date 2025-09-24 --charging 4h
    """);
    }

}

// java -cp target/classes com.example.Main --zone SE2 --date 2025-09-04 --sorted



