package montreal;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class PersonDescriptionBuilder {
    public static String buildPersonDescription(Person person) {
        // Extracting person attributes
        Integer ageDummy = (Integer) person.getAttributes().getAttribute("age");
        String bikeAvailability = (String) person.getAttributes().getAttribute("bikeAvailability");
        String carAvail = (String) person.getAttributes().getAttribute("carAvail");
        Integer economicSector = (Integer) person.getAttributes().getAttribute("economic_sector");
        String hasLicense = (String) person.getAttributes().getAttribute("hasLicense");
        Long householdId = (Long) person.getAttributes().getAttribute("household_id");
        Integer householdIncome = (Integer) person.getAttributes().getAttribute("household_income");
        Boolean isCarPassenger = (Boolean) person.getAttributes().getAttribute("isCarPassenger");
        Id<Person> personId = person.getId();
        Integer sex = (Integer) person.getAttributes().getAttribute("sex");

        // Converting dummy values to actual descriptions
        String ageDescription = getAgeDescription(ageDummy);
        String sexDescription = (sex == 1) ? "male" : "female";
        String incomeDescription = getIncomeDescription(householdIncome);
        String carPassengerDescription = isCarPassenger ? "She is a car passenger." : "She is not a car passenger.";

        // Building the description string
        String description = String.format(
            "Person ID %s is a %s aged between %s years old. She belongs to household ID %d and has a household income of %s per year. " +
            "She holds a driver's license (%s) and her car availability is '%s'. " +
            "She works in the economic sector %d. %s Additionally, she has bike availability for '%s'.",
            personId.toString(), sexDescription, ageDescription, householdId, incomeDescription, hasLicense, carAvail, economicSector, carPassengerDescription, bikeAvailability
        );

        return description;
    }

    private static String getAgeDescription(int ageDummy) {
        switch (ageDummy) {
            case 1:
                return "0-15";
            case 2:
                return "16-20";
            case 3:
                return "21-25";
            case 4:
                return "26-30";
            case 5:
                return "31-35";
            case 6:
                return "36-40";
            case 7:
                return "41-45";
            case 8:
                return "46-50";
            case 9:
                return "51-55";
            case 10:
                return "56-60";
            case 11:
                return "61-65";
            case 12:
                return "66-70";
            case 13:
                return "71-75";
            case 14:
                return "75 and above";
            default:
                return "unknown";
        }
    }

    private static String getIncomeDescription(int incomeDummy) {
        switch (incomeDummy) {
            case 1:
                return "less than $50,000";
            case 2:
                return "$50,000-$90,000";
            case 3:
                return "$90,000-$120,000";
            case 4:
                return "$120,000-$150,000";
            case 5:
                return "$150,000 and above";
            default:
                return "unknown";
        }
    }
}
