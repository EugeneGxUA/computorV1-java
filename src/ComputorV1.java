import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComputorV1 {

    private static String equation;
    private static String[] parts;

    private static int someNumeric = 0;

    private static final Pattern PATTERN_STARS = Pattern.compile("[ * ]+");
    private static final DecimalFormat dF = new DecimalFormat("#.##"); //"1.2"

    final static ScriptEngineManager manager = new ScriptEngineManager();
    final static ScriptEngine engine = manager.getEngineByName("js");

    private static void validateSymbols() {

        int wrongOrder = 0;
        int sign = 0;
        int notSign = 0;

        boolean haveX = false;
        boolean lastSign = false;

        Matcher matcherSymbols;

        final String noSpaceEquation = equation.replace(" ", ""); {
            matcherSymbols = PATTERN_STARS.matcher(noSpaceEquation);
            while (matcherSymbols.find()) {
                if (
                        noSpaceEquation.toCharArray()[matcherSymbols.start() - 1] == '*'
                                ||
                        noSpaceEquation.toCharArray()[matcherSymbols.start() + 1] == '*'
                ) {
                    System.out.println("Invalid string to many stars =)");
                    System.exit(0);
                }
            }
        }
        equation = equation.toUpperCase().replace(" * ", "");

        parts = equation.split(" ");

        for (String part : parts) {

            if (wrongOrder < 0 || wrongOrder > 1) {
                System.out.println("Bad syntax. You have wrong number of signs in your equation =)");
                System.exit(0);
            }
            Matcher matcherSymbolsBig = Pattern.compile("[-+]?[0-9]*\\.?[0-9]*[X]?(\\^\\d+)?").matcher(part);
            Matcher matcherSymbolsSmall = Pattern.compile("[+|\\-|=]").matcher(part);
            Matcher matcherX = Pattern.compile("[X]").matcher(part);

            if (matcherSymbolsSmall.matches()) {
                wrongOrder--;
                sign++;

                if (part.equals("=")) {
                    lastSign = true;
                }

            } else if (matcherSymbolsBig.matches()) {
                wrongOrder++;
                notSign++;

                if (matcherX.matches()) {
                    haveX = true;
                }

                if (lastSign) lastSign = false;

            } else {
                System.out.println("Invalid syntax =)");
                System.exit(0);
            }

        }

        if (sign != (notSign - 1) && !haveX) {
            System.out.println("There is no symbol X in the equation =)");
            System.exit(0);
        }
        if (lastSign) {
            System.out.println("There is no symbol '=' in the equation  =)");
            System.exit(0);
        }
    }

    private static String toZeroEquation;
    private static String allLeft;


    // replace all to left side
    private static void rightToZero() {
        final String[] someParts = equation.split("=");
        String leftPart = someParts[0].trim();

        String minusStr = someParts[1].replace(" - ", " m ").trim();
        String plusStr = minusStr.replace(" + ", " p ");

        String minusStringNew = plusStr.replace(" p ", " - ");
        String plusStringNew = minusStringNew.replace(" m ", " + ");

        toZeroEquation = leftPart + " - " + plusStringNew + " = 0";
        allLeft = leftPart + " - " + plusStringNew;

        System.out.println("TO ZERO EQ = " + toZeroEquation);
        System.out.println("All LEFT = " + allLeft);

    }

    private static List<Part> listOfParts = new ArrayList<>();

    private static void splitParts() {

        List<String> signs = new ArrayList<>();

        String[] tmpParts = allLeft.split(" ");
        for (String part : tmpParts) {
            if (part.contains("X")) {
                listOfParts.add(new Part(part, true, false));
            } else if (part.matches("[+|\\-|=]")) {
                signs.add(part);
            } else {
                listOfParts.add(new Part(part, false, false));
            }
        }

        //Add sign
        for (int i = 0; i < listOfParts.size(); i++) {
            listOfParts.get(i).sign = (i == 0) ? "+" : signs.get(i - 1);
        }
    }

    private static List<Part> newParts = new ArrayList<>();
    private static String finalEquation;

    private static void uniteSameParts() {
        List<Part> xList = new ArrayList<>();

        newParts = new ArrayList<>();
        String numeric = "0";

        for (Part part : listOfParts) {
            if (part.isX) {
                xList.add(part);
            } else {

            	numeric = numeric + " " + part.sign + " " + dF.format(part.index);

            }
        }


        try {
            Object result = engine.eval(numeric);
            Integer tmp = (Integer) result;
            someNumeric = tmp;
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }


        Map<Integer, List<Part>> xArrayDegreeMap = new HashMap<>();
        for (int i = 0; i < xList.size(); i++) {
            if (xArrayDegreeMap.containsKey(xList.get(i).power)) {
                List<Part> partsList = xArrayDegreeMap.get(xList.get(i).power);
                partsList.add(xList.get(i));
                xArrayDegreeMap.put(xList.get(i).power, partsList);
            } else {
                List<Part> parts = new ArrayList<>();
                parts.add(xList.get(i));
                xArrayDegreeMap.put(xList.get(i).power, parts);
            }
        }

        for (Map.Entry<Integer, List<Part>> entry : xArrayDegreeMap.entrySet()) {
                Part newPart = createNewPart(entry.getValue());
                if (newPart != null) {
                    newParts.add(newPart);
                }

        }

        for (Part part: newParts) {
            System.out.println(part); //todo del
        }


        String lastOne = String.valueOf(someNumeric);

        for (Part part : newParts) {

        	lastOne = lastOne + " " + part.sign + " " + part.index + "X^" + part.power;

            finalEquation = lastOne + " = 0";
        }

        System.out.println("Last one = " + lastOne);

        for (Part part : newParts) {
            if (part.power > 2) {
                System.out.println("Basic form: " + equation);
                System.out.println("Reduced form: " + toZeroEquation);
                System.out.println("Final form: " + finalEquation);
                System.out.println("Polynomial degree is " + part.power + " and it greater than 2. Sorry I can\'t solve this polynomial equation");
                System.exit(0);
            }
        }
        System.out.println("Basic form: " + equation);
        System.out.println("Reduced form: " + toZeroEquation);

        if (newParts.size() != 0) {
			System.out.println("Final form: " + finalEquation);
		} else {
			System.out.println("Final form: 0");
		}
    }

    private static boolean isQuadratic;

    private static Part createNewPart(List<Part> partList) {
        String sign;
        String index = "0";
        Double indexValue = 0d;

        int power = partList.get(0).power;

        for (Part part : partList) {

        	index = index + " " + part.sign + " " + part.index;

        }

        try {
            Object result = engine.eval(index);
			indexValue = (Double) result;

			sign = (indexValue >= 0) ? "+" : "-";

			if (indexValue < 0) indexValue *= -1;

			if (indexValue == 0) {
				return null;
			}

			if (power == 2) {
				isQuadratic = true;
			}

			Part part = new Part(dF.format(indexValue) + "X^" + power, true, false);
			part.sign = sign;
			return part;

        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }

    }

    private static double sqrt(double value) {
    	if (value == 0 || value == 1) {
    		return value;
		}

		double res = value;
		double diff;
		do {
			diff = res;
			res = 0.5 * (res + value / res);
		} while (res != diff);
		return res;
	}

    private static void solveQuadric() {
    	double a = 0;
    	double b = 0;
    	double c = someNumeric;


    	double d;
		for (Part part : newParts) {
			if (part.power == 2) {

				a = part.index;

			} else {

				b = part.index;

			}
		}

		d = b * b - (4 * a * c);

		if (d > 0) {
			System.out.println("Discriminant is strictly positive, the two solutions are:");

			double x1 = ((b * -1) + sqrt(d)) / (2 * a);
			double x2 = ((b * -1) - sqrt(d)) / (2 * a);

			System.out.println("X1 = " + dF.format(x1));
			System.out.println("X2 = " +dF.format(x2));
		} else if (d == 0) {

			System.out.println("Discriminant is equals zero :");

			double x = (b / (2 * a)) * -1;
			System.out.println("X = " + dF.format(x));
		} else {
			System.out.println("Discriminant is strictly negative.There are no real roots for this equation");
		}
	}

	private static void solveLinear() {
    	float newNumeric = someNumeric * -1;

    	if (newNumeric == 0 || newParts.size() == 0) {
//			System.out.println("Solve: " + dF.format(newNumeric));
			System.out.println("Any natural number is a solution of this equation");
			System.exit(1);
		}

		System.out.println("Solve: ");

		System.out.println("\t" + dF.format(newParts.get(0).index) + "X = " + newNumeric);
		double x = newNumeric / newParts.get(0).index;
		System.out.println("\tX = " + x);

	}



    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Invalid arguments = " + args.length + ", please enter 1 arg.");
            System.exit(0);
        }

        equation = args[0];

        String[] equationParts = equation.split("=");
        if (equationParts.length != 2) {
            System.out.println("Invalid string: too many or NO equation marks");
            System.exit(0);
        }

        validateSymbols();
        rightToZero();
        splitParts();
        uniteSameParts();
        if (isQuadratic) solveQuadric();
        else solveLinear();


        System.exit(1);

    }


    static class Part {

        private boolean isSign;
        private boolean isX;
        private boolean isDegree;

        private String sign;

        private double index;

        private int power;

        private Part(String part, boolean isX, boolean isSign) {
            this.isSign = isX;
            this.isX = isSign;

            if (isX) {
                splitX(part);
            } else {
                splitNumber(part);
            }
        }

        private void splitX(String part) {
            String[] splittedPart = part.trim().split("X");
            for (int i = 0; i < splittedPart.length; i++) {
                splittedPart[i] = splittedPart[i].trim();
            }

            if (!splittedPart[0].isEmpty()) {
                getIndex(splittedPart);
            } else {
                index = 1;
            }


            if (index == 0) {
                this.isX = false;
                this.isDegree = false;
            } else {
                power = splittedPart[1].contains("^") ?
                        Integer.parseInt(splittedPart[1].substring(splittedPart[1].indexOf("^") + 1)) :
                        1;
                if (power == 0) {
                    this.isDegree = false;
                    this.isX = false;
                } else {
                    this.isX = true;
                }
            }
        }


        private void splitNumber(String part) {
            if (part.contains("^")) {

                String[] splittedPart = part.split("^");
                if (splittedPart[0].isEmpty()) {
                    System.out.println("Bad syntax. You have more than one space one after another , non-numeric parts or some another mistake in your equation near ");
					System.exit(0);
                } else {
                    getIndex(splittedPart);

                    try {
						power = Integer.parseInt(splittedPart[1]);
					} catch (ClassCastException e) {
						System.out.println("Bad syntax. You have more than one space one after another , non-numeric parts or some another mistake in your equation near ");
						System.exit(0);
					}
                    toDegree();

                }

            } else {
                isDegree = false;

                index = Double.parseDouble(part);

            }
        }

        private void getIndex(String[] splittedPart) {

                index = Double.parseDouble(splittedPart[0]);

        }

        private void toDegree() {
            double tmpIndex = index;
            for (int i = power; i > 1; i--) {
                tmpIndex = tmpIndex * this.index;
            }

            this.index = tmpIndex;
        }

        @Override
        public String toString() {
            return "index = " + index + "\npower = " + power + "\nsign = " + sign + "\nisX = "+ isX +"\n\n";
        }
    }
}
