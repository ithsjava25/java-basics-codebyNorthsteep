package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.Map;

public class Main {
    public static void main(String[] args) {

        System.out.println("Välkommen till Elpris-kollen!");
        ElpriserAPI elpriserApi = new ElpriserAPI();


        //en if - sats för att tolka olika argument, key = variabeln för argument som samlats i args[]
        if(containsArg(args, "--help")) {
            helpMe();
            //programmet avslutas efter visad text i output
        }
        if(containsArg(args, "--date")) {
            String date = getArgValue(args, "--date");
            LocalDate idag = LocalDate.now();
            System.out.println("Datum: " + idag);
        }


        }




 // metod för att gå igenom och letar efter valt argument i innehållet i args[]
    //key är en variabel för argumentet i kommandotolken
    //behövs för om man har arument utan värde, Kollar om ett argument finns

    /*args[]: alla argument från kommandoraden
- key: det argument du letar efter, t.ex. "--zone"*/


    public static boolean containsArg(String[] args, String key) {
    for(String arg : args) {
        if (arg.equalsIgnoreCase(key)) {
            return true;
        }
    }
     return false;
 }

 public static String getArgValue(String[] args, String key) {
        for (int i = 0; i < args.length -1 ; i++) {
            if (args[i].equalsIgnoreCase(key)) {
                return args[i+1];
            }
        }
     return null;
 }

// metod för --help att skickas till if-sats
public static void helpMe() {
    System.out.println("""
    Hjälpcenter
    ______________________

    Användning:
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

}






