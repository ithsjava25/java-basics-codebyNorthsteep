package com.example;

import com.example.api.ElpriserAPI;

import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;

public class Main {
    public static void main(String[] args) {

        System.out.println("Välkommen till Elpris-kollen!");
        ElpriserAPI elpriserApi = new ElpriserAPI();

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
        if (args.length == 0) {
            ifInvalidChoice();
            return;
        }

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone" -> { if (i + 1 < args.length) {
                        zoneOf = args[++i];
                    } else {
                        ifInvalidChoice();
                        return;
                    }
                }
                case "--date" -> { if (i + 1 < args.length) {
                        dateOf = args[++i];

                    } else {
                        ifInvalidChoice();
                        return;
                    }
                }
                case "--charge" -> { if (i + 1 < args.length) {
                        chargeOf = args[++i];
                        if (chargeOf != null) {
                            //if charge anropas visa laddningsfönster
                        } else {
                            ifInvalidChoice();
                            return;
                        }
                    }
                }
                case "--sorted" -> isSorted = true;

                case "--help" -> {
                    //om args inehåller --help anropas metoden sendHelp(); och sedan sätts isHelped till true.
                    helpMe();
                    isHelped = true;
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

        ElpriserAPI.Prisklass zon;

        if (zoneOf == null || !validZones.contains(zoneOf.toUpperCase())) {
            System.out.println("Ogiltig zon: " + zoneOf);
            ifInvalidChoice();
            return;
        }
        zon = ElpriserAPI.Prisklass.valueOf(zoneOf.toUpperCase());

        List<ElpriserAPI.Elpris> prisLista = elpriserApi.getPriser(dagensDatum, zon);
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("sv", "SE"));
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);

        if (prisLista == null || prisLista.isEmpty()) {
            System.out.println("Inga priser hittades för zon: " + zon + "den " + dagensDatum);
            return;
        }
        if (isSorted) {
            prisLista.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh).reversed()); //Ta varje Elpris-objekt och anropa sekPerKwh | reversed blir fallande
        }

        //LocalDate tomorrow = dagensDatum.plusDays(1);
        for (ElpriserAPI.Elpris elpriser : prisLista) {

            String formateratPris = numberFormat.format(elpriser.sekPerKWh() * 100);
            System.out.println("Tid: " + elpriser.timeStart().toLocalTime() +"-"+ elpriser.timeEnd().toLocalTime() + " Pris: " + formateratPris + " öre/KWh");

        }
        /*if (tomorrow != null) {
            System.out.println("Morgon dagens elpriser: " + tomorrow);
        }*/


        double summa = 0.0;
        for (ElpriserAPI.Elpris elpriser : prisLista) {
            summa  += elpriser.sekPerKWh();
        }
        double medelPrisOfDay = summa/ prisLista.size();
        System.out.println("Medelpriset för dagens elpriser är: " +  numberFormat.format(medelPrisOfDay*100) + " öre/KWh");

//SLiding window int min =  int index =  double sum =
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




 //Metod för att göra att hämta elpriser?




}






