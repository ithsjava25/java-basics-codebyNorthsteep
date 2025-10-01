package com.example;

import com.example.api.ElpriserAPI;

import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


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
        boolean isSorted = false;

        List<String> validZones = new ArrayList<>();
        validZones.add("SE1");
        validZones.add("SE2");
        validZones.add("SE3");
        validZones.add("SE4");

        List<String> validHours = new ArrayList<>();
        validHours.add("2h");
        validHours.add("4h");
        validHours.add("8h");


        if (args.length == 0) { helpMe();
        }

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone" -> { if (i + 1 < args.length) {
                        zoneOf = args[++i];
                    }
                }
                case "--date" -> { if (i + 1 < args.length) {
                        dateOf = args[++i];
                    }
                }
                case "--charging" -> { if (i + 1 < args.length) {
                        chargingInput = args[++i];
                        }
                    }
                case "--sorted" ->
                    isSorted = true;

                case "--help" -> { helpMe(); return;}

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
        if(priserIdag.isEmpty()) {
            System.out.println("Inga priser hittade för idag");
            return;
        } else {
            System.out.println("Dagens priser:");
        }
        List<ElpriserAPI.Elpris> priserImorgon = elpriserApi.getPriser(tomorrow, zon);
        if(priserImorgon.isEmpty()){
            System.out.println("Inga priser för imorgon hittades");
        } else {
            System.out.println("Morgondagens priser:");
        }

        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);

        List<ElpriserAPI.Elpris> kombineradeListor = combinedLists(priserIdag,priserImorgon);



            int timmar =0;
        if (chargingInput != null) {
           if (!validHours.contains(chargingInput.toLowerCase())) {
               System.out.println("Ogiltig charging: " + chargingInput);
               helpMe();
               return;
           }
           try {
               timmar = Integer.parseInt(chargingInput.toLowerCase().replace("h",""));
           } catch (NumberFormatException e) {
               helpMe();
               return;
           }
        }

        if (timmar > 0) {
            chargingWindow(kombineradeListor, timmar);
            return;
        }
        if (isSorted) {
            List<ElpriserAPI.Elpris> sorteradePriser = isSortedCombined(priserIdag, priserImorgon);
            printPriser(sorteradePriser);
        }


        printPriser(kombineradeListor);
        priceMinMax(kombineradeListor);
        medelPris(kombineradeListor);
        calculateHourlyAverages(priserIdag);

    }

    public static List<ElpriserAPI.Elpris> combinedLists(List<ElpriserAPI.Elpris> priserIdag, List<ElpriserAPI.Elpris> priserImorgon) {
        List<ElpriserAPI.Elpris> kombineradeLaddpriser = new ArrayList<>();
        if (priserIdag != null) kombineradeLaddpriser.addAll(priserIdag);
        if (priserImorgon != null) kombineradeLaddpriser.addAll(priserImorgon);

        return kombineradeLaddpriser;
    }

    public static void chargingWindow (List<ElpriserAPI.Elpris> elpriserLadda, int timmar) {

        DateTimeFormatter minuteFormatter = DateTimeFormatter.ofPattern("HH:mm");


        if (elpriserLadda == null) {
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


            System.out.printf("Billigaste laddningsfönster för %dh är kl %s-%s\nMedelpris för fönster: %.2f öre\n Påbörja laddning %s", timmar, startTid, slutTid, snittPris, startTid);
        }
    }
    //Processar prislistan som får 96 priser
    public static void calculateHourlyAverages (List<ElpriserAPI.Elpris> elpriser96) {
            if (elpriser96.size() > 48) {
        //i är 0; så länge i är mindre än storleken på listan; öka i med 4
            for (int i = 0; i < elpriser96.size(); i += 4) {
                //I varje loop, stoppa de fyra värdena i en sublist
                List<ElpriserAPI.Elpris> listaPerTimme = elpriser96.subList(i, i + 4);

                double sum = listaPerTimme.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).sum();
                double medelPris = sum / 4.0;

                int timme = i / 4;
                int timme2 = timme + 1;

                String timDel = String.format("%02d-%02d", timme, timme2);
                double medelprisToOre = medelPris * 100;


                System.out.printf("%s Medelpris: %.2f öre\n", timDel, medelprisToOre);
            }
            }
        }

    static void printPriser (List<ElpriserAPI.Elpris> priser) {
        priser.forEach(elpriser -> System.out.printf("""
                %s-%s %.2f öre
                """,elpriser.timeStart().format(timeFormatter), elpriser.timeEnd().format(timeFormatter), elpriser.sekPerKWh()*100));
    }

    public static List<ElpriserAPI.Elpris> isSortedCombined (List<ElpriserAPI.Elpris> priserIdag, List<ElpriserAPI.Elpris> priserImorgon) {
            List<ElpriserAPI.Elpris> sammansattaPriser = new ArrayList<>();
            sammansattaPriser.addAll(priserIdag);
            sammansattaPriser.addAll(priserImorgon);
            sammansattaPriser.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh).reversed().thenComparing(ElpriserAPI.Elpris::timeStart));

        return sammansattaPriser;
    }

    private static void medelPris(List<ElpriserAPI.Elpris> prisLista) {
        double summa = 0.0;
        if (prisLista == null) {
            System.out.println("Ingen data tillgänglig för medelpris");
            return;
        }
        for (ElpriserAPI.Elpris elpriser : prisLista) {
            summa  += elpriser.sekPerKWh();
        }
        double medelPrisOfDay = summa/ prisLista.size() *100;
        System.out.printf("Medelpris: %.2f öre \n",  medelPrisOfDay);
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
            double worthOf = elpriser.sekPerKWh() *100;

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
        System.out.printf("Lägsta pris för dagen: %.2f öre/KWh Kl: %s - %s \n", minPris, minTid, minTidSlut);
        System.out.printf("Högsta pris för dagen: %.2f öre/KWh Kl: %s - %s \n", maxPris, maxTid, maxTidSlut);

    }

    // metod för --help att skickas till if-sats
    public static void helpMe() {
        System.out.println("""
    Hjälpcenter
    ______________________

    Usage / Användning:
      java -cp target/classes com.example.Main [alternativ]

    Alternativ:
      --zone SE1|SE2|SE3|SE4      (obligatoriskt) Välj giltig elpris-zon
      --date YYYY-MM-DD           (valfritt)      Välj datum, t.ex. 2025-09-21
      --charging 2h|4h|8h         (valfritt)      Hitta billigaste laddningsfönster
      --sorted                    (valfritt)      Sortera priser fallande
      --help                      (valfritt)      Visa denna hjälptext

    Exempel:
      java Main --zone SE3 -- date 2025-09-21 --charging 4h
    """);
    }
}

// java -cp target/classes com.example.Main --zone SE2 --date 2025-09-04 --sorted



