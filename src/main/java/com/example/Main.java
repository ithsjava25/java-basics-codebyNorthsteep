package com.example;

import com.example.api.ElpriserAPI;

import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


//Att göra idag
//todo: Skapa en metod för att snygga till min switch
//todo: skapa en metod som beräknar min,max & medelpris
//todo: hämta morgondagens priser och skriv ut dem
//todo: skapa en metod för sliding window till -charging 2h,4h,8h


public class Main {
    public static void main(String[] args) {

        ElpriserAPI elpriserApi = new ElpriserAPI();
        System.out.println("Välkommen till Elpris-kollen!");
        String zoneOf = null;
        String dateOf = null;
        String chargeOf = null;
        boolean isHelped = false;
        boolean isSorted = false;

        List<String> validZones = new ArrayList<>();
        validZones.add("SE1");
        validZones.add("SE2");
        validZones.add("SE3");
        validZones.add("SE4");


//Loopar igenom args och letar efter input från terminalen, case stryr vad som händer om ex --zone skrivs in
        //if ger nya värden till zoneOf, dateOf, chargeOf och isHelped och skickar tillbaka till main-metoden
        if (args.length == 0) {ifInvalidChoice();
        }

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone" -> { if (i + 1 < args.length) {
                        zoneOf = args[++i];
                    } else { ifInvalidChoice();
                        return;
                    }
                }
                case "--date" -> { if (i + 1 < args.length) {
                        dateOf = args[++i];

                    } else { ifInvalidChoice();
                        return;
                    }
                }
                case "--charge" -> { if (i + 1 < args.length) {
                        chargeOf = args[++i];
                        if (chargeOf != null) {
                            //if charge anropas visa laddningsfönster
                        } else { ifInvalidChoice();
                            return;
                        }
                    }
                }
                case "--sorted" -> isSorted = true;

                case "--help" -> { helpMe(); isHelped = true;
                }
            }
            if (isHelped) return;

        }
        LocalDate dagensDatum;


        if (dateOf != null)
            try {
                dagensDatum = LocalDate.parse(dateOf);
            } catch (DateTimeException e) {
                System.out.println("Ogiltigt datum: " + dateOf);
                ifInvalidChoice();
                return;
            }
        else {
            dagensDatum = LocalDate.now();
        }
        LocalDate tomorrow = dagensDatum.plusDays(1);
        ElpriserAPI.Prisklass zon;

        if (zoneOf == null || !validZones.contains(zoneOf.toUpperCase())) {
            System.out.println("Ogiltig zon: " + zoneOf);
            ifInvalidChoice();
            return;
        }
        zon = ElpriserAPI.Prisklass.valueOf(zoneOf.toUpperCase());

        List<ElpriserAPI.Elpris> priserIdag = elpriserApi.getPriser(dagensDatum, zon);
        List<ElpriserAPI.Elpris> priserImorgon = elpriserApi.getPriser(tomorrow, zon);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH");
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("sv", "SE"));
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);


        skrivUtPriser(priserIdag,timeFormatter, numberFormat, "Dagenspriser: ", isSorted);
        skrivUtPriser(priserImorgon,timeFormatter, numberFormat, "Morgondagens priser: ", isSorted);
        medelPris(priserIdag, numberFormat);
        priceMinMax(priserIdag, numberFormat, timeFormatter);


//SLiding window int min =  int index =  double sum =
    }

    public static void chargingWindow (List<ElpriserAPI.Elpris> elpriserLadda, int timmar, DateTimeFormatter timeFormatter, NumberFormat numberFormat) {
        //todo: 2,4,8 h, testa i sub-arrays(Sliding windows)
        int min;
        int max;
        int index;
    }

    public static void skrivUtPriser (List<ElpriserAPI.Elpris> elprisList, DateTimeFormatter timeFormatter, NumberFormat numberFormat,String dag, boolean isSorted) {


        if (elprisList == null || elprisList.isEmpty()) {
            System.out.println("Ingen data för " + dag + " är tillgänglig");
            return;
        }
        if (isSorted) {
            elprisList.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh).reversed());

        }
        System.out.println(dag);
        for (ElpriserAPI.Elpris elpriser : elprisList) {
            String formateratPris = numberFormat.format(elpriser.sekPerKWh() * 100);
            System.out.println("Tid: " + elpriser.timeStart().format(timeFormatter) +"-"+ elpriser.timeEnd().format(timeFormatter) + " Pris: " + formateratPris + " öre/KWh");
        }

    }

    private static void medelPris(List<ElpriserAPI.Elpris> prisLista, NumberFormat numberFormat) {
        double summa = 0.0;
        for (ElpriserAPI.Elpris elpriser : prisLista) {
            summa  += elpriser.sekPerKWh();
        }
        double medelPrisOfDay = summa/ prisLista.size();
        System.out.println("Medelpriser för dagen är: " +  numberFormat.format(medelPrisOfDay*100) + " öre/KWh");
    }

    public static void priceMinMax (List<ElpriserAPI.Elpris> priser, NumberFormat numberFormat, DateTimeFormatter timeFormatter) {
        double minPris = Double.MAX_VALUE;
        double maxPris = Double.MIN_VALUE;
        String minTid = null;
        String minTidSlut = null;
        String maxTid = null;
        String maxTidSlut = null;
        if (priser.isEmpty()) {
            System.out.println("Ingen prisdata finns att visa");
            return;
        }

        for (ElpriserAPI.Elpris elpriser : priser) {
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


    //Metod som anropas om det inmatats ett felaktigt argument
    private static void ifInvalidChoice() {
        System.out.println("Ogiltigt val, du skickas nu till hjälpmeny");
        helpMe();

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
      java Main --zone SE3 --charging 4h
    """);
    }
    //plusDays(1) - metod för att visa för nästa dag

}






